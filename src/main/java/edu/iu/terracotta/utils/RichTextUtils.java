package edu.iu.terracotta.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import lombok.experimental.UtilityClass;

/**
 * Text from what the rich text editor saves (essay responses), for places that show plain text,
 * such as the data export.
 */
@UtilityClass
public class RichTextUtils {

    // what the editor saves always starts with one of its block elements (a YouTube video is a div);
    // anything else was typed before essays took rich text, as plain text
    private static final Pattern RICH_TEXT = Pattern.compile("^<(p|ul|ol|blockquote|h[1-6]|pre|hr|div)[\\s/>]", Pattern.CASE_INSENSITIVE);

    public boolean isRichText(String value) {
        return value != null && RICH_TEXT.matcher(value.strip()).find();
    }

    /**
     * The text of rich text: a blank line between paragraphs, a line per list item ("- " for a
     * bulleted one, "1. " for a numbered one, indented when nested). Plain text is returned as it is.
     */
    public String toPlainText(String value) {
        if (StringUtils.isBlank(value) || !isRichText(value)) {
            return value;
        }

        StringBuilder text = new StringBuilder();
        appendBlocks(Jsoup.parseBodyFragment(value).body(), text, "");

        return text.toString().strip();
    }

    private void appendBlocks(Element container, StringBuilder text, String indent) {
        for (Node node : container.childNodes()) {
            if (node instanceof TextNode textNode) {
                if (!textNode.isBlank()) {
                    text.append(textNode.getWholeText());
                }

                continue;
            }

            if (!(node instanceof Element element)) {
                continue;
            }

            switch (element.normalName()) {
                case "ul", "ol" -> {
                    appendList(element, text, indent);

                    if (indent.isEmpty()) {
                        text.append("\n");
                    }
                }
                case "blockquote", "div" -> appendBlocks(element, text, indent);
                case "hr" -> text.append("\n");
                // a video has no text of its own, so the export gives its address
                case "iframe" -> text.append(indent).append(element.attr("src")).append("\n\n");
                default -> {
                    text.append(indent).append(inlineText(element).strip()).append("\n");

                    if (indent.isEmpty()) {
                        text.append("\n");
                    }
                }
            }
        }
    }

    private void appendList(Element list, StringBuilder text, String indent) {
        boolean numbered = "ol".equals(list.normalName());
        int number = 1;

        for (Element item : list.children()) {
            if (!"li".equals(item.normalName())) {
                continue;
            }

            String marker = numbered ? (number++) + ". " : "- ";
            boolean first = true;

            for (Element child : item.children()) {
                if ("ul".equals(child.normalName()) || "ol".equals(child.normalName())) {
                    appendList(child, text, indent + "  ");
                    continue;
                }

                text.append(indent).append(first ? marker : " ".repeat(marker.length())).append(inlineText(child).strip()).append("\n");
                first = false;
            }

            if (first) {
                // an item whose text isn't in a paragraph
                text.append(indent).append(marker).append(inlineText(item).strip()).append("\n");
            }
        }
    }

    private String inlineText(Element element) {
        StringBuilder text = new StringBuilder();

        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                text.append(textNode.getWholeText());
            } else if (node instanceof Element child) {
                text.append("br".equals(child.normalName()) ? "\n" : inlineText(child));
            }
        }

        return text.toString();
    }

}
