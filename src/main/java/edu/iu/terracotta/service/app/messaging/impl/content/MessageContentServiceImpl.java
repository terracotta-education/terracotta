package edu.iu.terracotta.service.app.messaging.impl.content;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageContentBodyHtmlElement;
import edu.iu.terracotta.dao.repository.messaging.content.MessageContentRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextService;
import edu.iu.terracotta.service.app.messaging.MessageContentAttachmentService;
import edu.iu.terracotta.service.app.messaging.MessageContentService;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class MessageContentServiceImpl implements MessageContentService {

    @Autowired private MessageContentRepository contentRepository;
    @Autowired private MessageConditionalTextService conditionalTextService;
    @Autowired private MessageContentAttachmentService contentAttachmentService;
    @Autowired private MessagePipedTextService pipedTextService;

    @Override
    public void create(Message message) {
        message.setContent(
            MessageContent.builder()
                .message(message)
                .build()
        );
    }

    @Override
    public void update(MessageContentDto contentDto, Message message) {
        message.setContent(
            fromDto(
                contentDto,
                message.getContent()
            )
        );
    }

    @Override
    public void duplicate(MessageContent content, Message message) throws MessageBodyParseException {
        MessageContent newMessageContent = MessageContent.builder()
            .html(content.getHtml())
            .message(message)
            .build();

        message.setContent(newMessageContent);
        contentAttachmentService.duplicate(content.getAttachments(), newMessageContent);
        conditionalTextService.duplicate(content.getConditionalTexts(), newMessageContent);
        pipedTextService.duplicate(content.getPipedText(), newMessageContent);
        updatePlaceholders(newMessageContent, false);
    }

    @Override
    public MessageContentDto toDto(MessageContent content) {
        if (content == null) {
            return null;
        }

        return MessageContentDto.builder()
            .id(content.getUuid())
            .attachments(
                contentAttachmentService.toDto(content.getAttachments())
            )
            .conditionalTexts(
                conditionalTextService.toDto(content.getConditionalTexts())
            )
            .html(content.getHtml())
            .messageId(content.getMessage().getUuid())
            .pipedText(
                pipedTextService.toDto(content.getPipedText())
            )
            .build();
    }

    @Override
    public MessageContent fromDto(MessageContentDto contentDto, MessageContent content) {
        content.setHtml(contentDto.getHtml());
        contentAttachmentService.update(contentDto.getAttachments(), content);
        conditionalTextService.upsert(contentDto.getConditionalTexts(), content);
        pipedTextService.upsert(contentDto.getPipedText(), content);

        return content;
    }

    @Override
    public void updatePlaceholders(MessageContent content, boolean save) throws MessageBodyParseException {
        if (content == null || StringUtils.isBlank(content.getHtml())) {
            return;
        }

        Document.OutputSettings outputSettings = new Document.OutputSettings()
            .prettyPrint(false);
        Document document = Jsoup.parse(content.getHtml());
        document.outputSettings(outputSettings);

        /*
         * Handle conditional text elements first
         */

        updateConditionalText(document, content);

        /*
         * Handle piped text elements after conditional text, as conditional text replacement may have added additional piped text elements
         */

        updatePipedText(document, content);

        /*
         * Handle piped text elements in conditional texts
         */

        for (MessageConditionalText conditionalText : content.getConditionalTexts()) {
            if (StringUtils.isBlank(conditionalText.getResult().getHtml())) {
                continue;
            }

            Document conditionalTextDocument = Jsoup.parse(conditionalText.getResult().getHtml());
            updatePipedText(conditionalTextDocument, content);
            conditionalText.getResult().setHtml(conditionalTextDocument.body().selectFirst("body").html());
        }

        content.setHtml(document.body().selectFirst("body").html());

        if (save) {
            content = contentRepository.save(content);
        }
    }

    @Override
    public MessageContentDto updatePlaceholders(MessageContent content, MessageContentDto contentDto) throws MessageBodyParseException {
        // update content with DTO html and pipedText fields only
        content.setHtml(contentDto.getHtml());
        content.setPipedText(
            pipedTextService.fromDto(
                contentDto.getPipedText(),
                MessagePipedText.builder()
                    .content(content)
                    .build(),
                true
            )
        );
        content.setConditionalTexts(
            conditionalTextService.fromDto(
                contentDto.getConditionalTexts(),
                content,
                true,
                true
            )
        );

        updatePlaceholders(content, false);

        // update the DTO's html field with the processed content html
        contentDto.setHtml(content.getHtml());
        contentDto.setConditionalTexts(
            conditionalTextService.toDto(content.getConditionalTexts())
        );

        return contentDto;
    }

    private void updateConditionalText(Document document, MessageContent content) {
        List<Element> conditionalTextElements = document.getElementsByTag(MessageContentBodyHtmlElement.TAG_CONDITIONAL_TEXT.getValue());

        for (Element element : conditionalTextElements) {
            String dataId = element.attr(MessageContentBodyHtmlElement.ATTR_DATA_ID.getValue());
            String dataLabel = element.attr(MessageContentBodyHtmlElement.ATTR_DATA_LABEL.getValue());
            String onClick = element.attr(MessageContentBodyHtmlElement.ONCLICK.getValue());
            MessageConditionalText conditionalText = content.getConditionalTexts().stream()
                .filter(
                    ct ->
                        Strings.CS.equals(
                            ct.getLabel(),
                            Strings.CS.removeStart(
                                dataLabel,
                                MessageContentBodyHtmlElement.LABEL_PREPEND_CONDITIONAL_TEXT.getValue()
                            )
                        )
                )
                .findFirst()
                .orElse(null);

            if (conditionalText == null) {
                log.error("Conditional text not found for data-id: [{}] in message content ID: [{}]", dataId, content.getId());
                List<String>  attributeToRemove = new ArrayList<>();
                Attributes attributes = element.attributes();
                for (Attribute attribute : attributes) {
                    attributeToRemove.add(attribute.getKey());
                }

                for(String attribute : attributeToRemove) {
                    element.removeAttr(attribute);
                }

                element.text(String.format("{{ INVALID %s }}", dataLabel));

                continue;
            }

            element.attr(MessageContentBodyHtmlElement.ATTR_DATA_ID.getValue(), conditionalText.getUuid().toString());
            element.attr(MessageContentBodyHtmlElement.ONCLICK.getValue(), Strings.CS.replace(onClick, dataId, conditionalText.getUuid().toString()));
        }
    }

    private void updatePipedText(Document document, MessageContent content) {
        List<Element> pipedTextElements = document.getElementsByTag(MessageContentBodyHtmlElement.TAG_PIPED_TEXT.getValue());

        for (Element element : pipedTextElements) {
            String dataId = element.attr(MessageContentBodyHtmlElement.ATTR_DATA_ID.getValue());
            String dataLabel = element.attr(MessageContentBodyHtmlElement.ATTR_DATA_LABEL.getValue());
            String prepend = StringUtils.isNotBlank(dataId) ? MessageContentBodyHtmlElement.LABEL_PREPEND_PIPED_TEXT.getValue() : String.format("%s %s", MessageContentBodyHtmlElement.INVALID.getValue(), MessageContentBodyHtmlElement.LABEL_PREPEND_PIPED_TEXT.getValue());
            MessagePipedTextItem pipedTextItem = content.getPipedText().getItems().stream()
                .filter(
                    pti ->
                        Strings.CS.equals(
                            pti.getKey(),
                            Strings.CS.removeStart(
                                dataLabel,
                                prepend
                            )
                        )
                )
                .findFirst()
                .orElse(null);

            if (pipedTextItem == null) {
                log.error("Piped text item not found for data-id: [{}] in message content ID: [{}]", dataId, content.getId());
                List<String>  attributeToRemove = new ArrayList<>();
                Attributes attributes = element.attributes();

                for (Attribute attribute : attributes) {
                    if (attribute.getKey().equals(MessageContentBodyHtmlElement.ATTR_DATA_LABEL.getValue())) {
                        continue; // keep the data-label attribute
                    }
                    attributeToRemove.add(attribute.getKey());
                }

                for (String attribute : attributeToRemove) {
                    element.removeAttr(attribute);
                }

                // mark the placeholder as invalid
                if (!Strings.CI.startsWith(dataLabel, MessageContentBodyHtmlElement.INVALID.getValue())) {
                    element.attr(MessageContentBodyHtmlElement.ATTR_DATA_LABEL.getValue(), String.format("%s %s", MessageContentBodyHtmlElement.INVALID.getValue(), dataLabel));
                    element.text(String.format("{{ INVALID %s }}", dataLabel));
                }

                // add the invalid css class if not already present
                if (!element.hasClass(MessageContentBodyHtmlElement.INVALID_CSS_CLASS.getValue())) {
                    element.addClass(MessageContentBodyHtmlElement.INVALID_CSS_CLASS.getValue());
                }

                continue;
            }

            // update the data-label and text to remove the INVALID prefix if it exists
            if (Strings.CI.startsWith(dataLabel, MessageContentBodyHtmlElement.INVALID.getValue())) {
                String updatedDataLabel = StringUtils.trimToEmpty(Strings.CS.removeStart(dataLabel, MessageContentBodyHtmlElement.INVALID.getValue()));
                element.attr(MessageContentBodyHtmlElement.ATTR_DATA_LABEL.getValue(), updatedDataLabel);
                element.text(String.format("{{ %s }}", updatedDataLabel));
            }

            // remove the invalid css class if present
            if (element.hasClass(MessageContentBodyHtmlElement.INVALID_CSS_CLASS.getValue())) {
                element.removeClass(MessageContentBodyHtmlElement.INVALID_CSS_CLASS.getValue());
            }

            element.attr(MessageContentBodyHtmlElement.ATTR_DATA_ID.getValue(), pipedTextItem.getUuid().toString());
        }
    }

    @Override
    public String prepareBodyHtmlForExport(String body) {
        if (StringUtils.isBlank(body)) {
            return body;
        }

        Document.OutputSettings outputSettings = new Document.OutputSettings()
            .prettyPrint(false);
        Document document = Jsoup.parse(body);
        document.outputSettings(outputSettings);
        List<Element> conditionalTextElements = document.getElementsByTag(MessageContentBodyHtmlElement.TAG_CONDITIONAL_TEXT.getValue());

        for (Element element : conditionalTextElements) {
            element.replaceWith(new TextNode(element.text()));
        }

        List<Element> pipedTextElements = document.getElementsByTag(MessageContentBodyHtmlElement.TAG_PIPED_TEXT.getValue());

        for (Element element : pipedTextElements) {
            element.replaceWith(new TextNode(element.text()));
        }

        return document.body().html();
    }

}
