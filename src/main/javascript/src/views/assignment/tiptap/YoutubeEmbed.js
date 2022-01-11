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
        }
      },
      group: "block",
      selectable: false,
      parseDOM: [
        {
          tag: "iframe",
          getAttrs: dom => ({
            youtubeID: dom.getAttribute('data-youtube-id'),
          })
        }
      ],
      toDOM: node => [
        "iframe",
        {
          src: `https://www.youtube.com/embed/${node.attrs.youtubeID}`,
          "data-youtube-id": node.attrs.youtubeID,
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
