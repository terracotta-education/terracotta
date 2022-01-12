import { Node } from "tiptap";

// Tiptap Node extension
export default class Iframe extends Node {
  get name() {
      // TODO: rename to "youtube"
    return "iframe";
  }

  get schema() {
    return {
      attrs: {
        youtubeID: {
            default: null
        },
        height: {
            default: 315
        },
        width: {
            default: 560
        }
      },
      group: "block",
      selectable: false,
      parseDOM: [
        {
          tag: "iframe",
          getAttrs: dom => ({
            youtubeID: dom.getAttribute('data-youtube-id'),
            height: dom.getAttribute('height'),
            width: dom.getAttribute('width'),
          })
        }
      ],
      toDOM: node => [
        "iframe",
        {
          src: `https://www.youtube.com/embed/${node.attrs.youtubeID}`,
          "data-youtube-id": node.attrs.youtubeID,
          height: node.attrs.height,
          width: node.attrs.width,
          frameborder: 0,
          allowfullscreen: "true",
          allow:
            "accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
          // You can set the width and height here also
        }
      ]
    };
  }

  commands({ type }) {
    return attrs => (state, dispatch) => {
      const { selection } = state;
      const position = selection.$cursor
        ? selection.$cursor.pos
        : selection.$to.pos;
      const node = type.create(attrs);
      const transaction = state.tr.insert(position, node);
      dispatch(transaction);
    };
  }
}
