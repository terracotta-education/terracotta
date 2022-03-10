import { Node } from "tiptap";
import { Plugin } from "prosemirror-state";
import { Slice, Fragment } from "prosemirror-model";
import { parseIframeEmbed, youtubeParser } from "./YoutubeEmbedUtil";

// Source: https://github.com/ueberdosis/tiptap/issues/689#issuecomment-624076217
function nodePasteRule(regexp, type, getAttrs) {
  const handler = (fragment) => {
    const nodes = [];

    fragment.forEach((child) => {
      if (child.isText) {
        const { text } = child;
        let pos = 0;
        let match;

        // eslint-disable-next-line
        while ((match = regexp.exec(text)) !== null) {
          if (match[0]) {
            const start = match.index;
            const end = start + match[0].length;
            const attrs =
              getAttrs instanceof Function ? getAttrs(match) : getAttrs;

            // adding text before markdown to nodes
            if (start > 0) {
              nodes.push(child.cut(pos, start));
            }

            // create the node
            nodes.push(type.create(attrs));

            pos = end;
          }
        }

        // adding rest of text to nodes
        if (pos < text.length) {
          nodes.push(child.cut(pos));
        }
      } else {
        nodes.push(child.copy(handler(child.content)));
      }
    });

    return Fragment.fromArray(nodes);
  };

  return new Plugin({
    props: {
      transformPasted: (slice) =>
        new Slice(handler(slice.content), slice.openStart, slice.openEnd),
    },
  });
}

// Tiptap Node extension
export default class YoutubeEmbed extends Node {
  get name() {
    return "youtube-embed";
  }

  get schema() {
    return {
      attrs: {
        youtubeID: {
          default: null,
        },
        height: {
          default: 315,
        },
        width: {
          default: 560,
        },
      },
      group: "block",
      selectable: false,
      parseDOM: [
        {
          tag: "iframe[data-youtube-id]",
          getAttrs: (dom) => ({
            youtubeID: dom.getAttribute("data-youtube-id"),
            height: dom.getAttribute("height"),
            width: dom.getAttribute("width"),
          }),
        },
      ],
      toDOM: (node) => [
        "iframe",
        {
          src: `https://www.youtube.com/embed/${node.attrs.youtubeID}?enablejsapi=1`,
          "data-youtube-id": node.attrs.youtubeID,
          height: node.attrs.height,
          width: node.attrs.width,
          frameborder: 0,
          allowfullscreen: "true",
          allow:
            "accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture",
          // This doesn't work. Must add enablejsapi as a query parameter of the
          // src attribute, see above.
          // enablejsapi: "true",
        },
      ],
    };
  }

  commands({ type }) {
    return (attrs) => (state, dispatch) => {
      const { selection } = state;
      const position = selection.$cursor
        ? selection.$cursor.pos
        : selection.$to.pos;
      const node = type.create(attrs);
      const transaction = state.tr.insert(position, node);
      dispatch(transaction);
    };
  }

  pasteRules({ type }) {
    return [
      nodePasteRule(
        new RegExp(`<iframe .*src="https://www.youtube.com.*></iframe>`, "g"),
        type,
        (match) => {
          const text = match[0];
          const iframe = parseIframeEmbed(text);
          const youtubeID = youtubeParser(iframe.src);
          const height = parseInt(iframe.height) || undefined;
          const width = parseInt(iframe.width) || undefined;
          return {
            youtubeID,
            height,
            width,
          };
        }
      ),
    ];
  }
}
