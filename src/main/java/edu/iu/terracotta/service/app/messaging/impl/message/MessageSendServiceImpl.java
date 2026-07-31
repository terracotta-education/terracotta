package edu.iu.terracotta.service.app.messaging.impl.message;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchEmailProjection;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentState;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentType;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItemValue;
import edu.iu.terracotta.dao.model.enums.messaging.MessageContentBodyHtmlElement;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.messaging.conditional.MessageConditionalTextRepository;
import edu.iu.terracotta.dao.repository.messaging.piped.PipedTextItemRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import edu.iu.terracotta.service.app.messaging.MessageSendService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class MessageSendServiceImpl implements MessageSendService {

    private final LmsUserBatchRepository lmsUserBatchRepository;
    private final LtiUserRepository ltiUserRepository;
    private final MessageConditionalTextRepository conditionalTextRepository;
    private final ParticipantRepository participantRepository;
    private final PipedTextItemRepository pipedTextItemRepository;
    private final MessageRuleComparisonService ruleComparisonService;
    private final ApiClient apiClient;
    private final LmsUtils lmsUtils;

    @PersistenceContext private EntityManager entityManager;

    @Value("${app.participant.batch.size:500}")
    private int batchSize;

    @Override
    public List<LtiUserEntity> getRecipients(Message message) throws ApiException, IOException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();

        apiClient.listUsersForCourse(
            LmsGetUsersInCourseOptions.builder()
                .batchId(batchId)
                .contextId(message.getExperiment().getLtiContextEntity().getContextId())
                .batchSize(batchSize)
                .enrollmentState(Arrays.asList(EnrollmentState.ACTIVE, EnrollmentState.INVITED))
                .enrollmentType(Arrays.asList(EnrollmentType.STUDENT))
                .lmsCourseId(lmsUtils.parseCourseId(message.getPlatformDeployment(), message.getExperiment().getLtiContextEntity().getContext_memberships_url()))
                .build(),
            message.getOwner()
        );

        Map<String, List<LmsSubmission>> lmsSubmissions;

        try {
            lmsSubmissions = ruleComparisonService.getLmsSubmissions(message);
        } catch (Exception e) {
            throw new ApiException(String.format("Error retrieving LMS submissions for message ID: [%s]", message.getId()), e);
        }


        List<LtiUserEntity> recipients = new ArrayList<>();

        int participantsPage = 0;
        PageRequest participantsPageRequest = PageRequest.of(participantsPage, batchSize);

        AtomicReference<List<Participant>> participants = new AtomicReference<>(participantRepository.findByExperiment_ExperimentId(message.getExperimentId(), participantsPageRequest));

        if (CollectionUtils.isEmpty(participants.get())) {
            log.info("No participants found for the experiment with ID: [{}]", message.getExperimentId());
            return List.of();
        }

        while (CollectionUtils.isNotEmpty(participants.get())) {
            List<LmsUserBatchEmailProjection> batchEmails = lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailIn(
                batchId,
                participants.get().stream()
                    .map(p -> p.getLtiUserEntity().getEmail())
                    .toList(),
                PageRequest.of(0, batchSize)
            );

            // user's LMS user ID is not always available in the database; try with email and deployment key ID
            Map<String, LtiUserEntity> ltiUserEntitiesByEmail = ltiUserRepository.findAllByEmailInAndPlatformDeployment_KeyId(
                    batchEmails.stream().map(LmsUserBatchEmailProjection::getEmail).toList(),
                    message.getPlatformDeployment().getKeyId()
                ).stream()
                .collect(Collectors.toMap(LtiUserEntity::getEmail, ltiUserEntity -> ltiUserEntity, (first, second) -> first));

            recipients.addAll(
                batchEmails.stream()
                    .map(
                        batchEmail -> {
                            LtiUserEntity ltiUserEntity = ltiUserEntitiesByEmail.get(batchEmail.getEmail());

                            if (ltiUserEntity == null || (ltiUserEntity.getLmsUserId() != null && !Strings.CI.equals(batchEmail.getLmsUserId(), ltiUserEntity.getLmsUserId()))) {
                                // wrong LMS user found; don't add message log
                                log.info(
                                    "No Terracotta user found with email: [{}] and platform deployment key ID: [{}]. Cannot add to recipients list.",
                                    batchEmail.getEmail(),
                                    message.getPlatformDeployment().getKeyId());
                                return null;
                            }

                            if (ltiUserEntity.getLmsUserId() == null) {
                                // set LMS user ID for the user; needed for LMS create conversation request
                                log.info("Setting LMS user ID: [{}] to Terracotta user ID: [{}]", batchEmail.getLmsUserId(), ltiUserEntity.getUserId());
                                ltiUserEntity.setLmsUserId(batchEmail.getLmsUserId());
                                ltiUserEntity = ltiUserRepository.save(ltiUserEntity);
                            }

                            long userId = ltiUserEntity.getUserId();

                            Optional<Participant> participant = participants.get().stream()
                                .filter(p -> p.getLtiUserEntity().getUserId() == userId)
                                .findFirst();

                            if (participant.isEmpty()) {
                                // no participant found for user; don't add to recipient list
                                return null;
                            }

                            participant.get().setLtiUserEntity(ltiUserEntity);

                            if (message.isToConsentedOnly() && BooleanUtils.isNotTrue(participant.get().getConsent())) {
                                // is send to consented only and user has not consented; don't add to recipients list
                                return null;
                            }

                            if (!message.getContainer().isSingleVersion()) {
                                // this is not a single-version message; check group
                                if (participant.get().getGroup() == null && !message.isDefaultMessage()) {
                                    // no group assigned to participant and is not a default condition message; don't add to recipient list
                                    return null;
                                }

                                if (participant.get().getGroup() != null && !participant.get().getGroup().getGroupId().equals(message.getExposureGroupCondition().getGroup().getGroupId())) {
                                    // group assigned to participant does not match message's exposure group condition; don't add to recipient list
                                    return null;
                                }
                            }

                            // find all submissions for this recipient
                            Map<String, List<LmsSubmission>> participantSubmissions = lmsSubmissions.entrySet().stream()
                                .collect(
                                    Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().stream()
                                            .filter(lmsSubmission -> Strings.CI.equals(lmsSubmission.getUserId(), participant.get().getLtiUserEntity().getLmsUserId()))
                                            .toList()
                                    )
                                );

                            if (MessageRecipientMatchType.EXCLUDE == message.getConfiguration().getRecipientMatchType() && ruleComparisonService.recipientRuleSetsMatch(message.getRuleSets(), participantSubmissions)) {
                                // recipient rule sets match but recipient match type is exclude; don't add to recipient list
                                return null;
                            }

                            if (MessageRecipientMatchType.INCLUDE == message.getConfiguration().getRecipientMatchType() && !ruleComparisonService.recipientRuleSetsMatch(message.getRuleSets(), participantSubmissions)) {
                                // recipient rule sets do not match and recipient match type is include; don't add to recipient list
                                return null;
                            }

                            // all checks passed; add recipient to the list
                            return ltiUserEntity;
                        }
                    )
                    .filter(Objects::nonNull)
                    .toList()
            );

            // flush and clear for this batch
            entityManager.flush();
            entityManager.clear();

            // get next batch of participants
            participantsPageRequest = PageRequest.of(++participantsPage, batchSize);
            participants.set(participantRepository.findByExperiment_ExperimentId(message.getExperimentId(), participantsPageRequest));
        }

        return recipients;
    }

    @Override
    public String parseMessageBody(Message message, LtiUserEntity recipient, Map<String, List<LmsSubmission>> lmsSubmissions) throws MessageBodyParseException {
        return parseMessageBody(message, recipient, lmsSubmissions, false);
    }

    @Override
    public String parseMessageBody(Message message, LtiUserEntity recipient, Map<String, List<LmsSubmission>> lmsSubmissions, boolean isPreview) throws MessageBodyParseException {
        return parseMessageBody(message, recipient, lmsSubmissions, isPreview, new HashMap<>(), new HashMap<>());
    }

    @Override
    public String parseMessageBody(Message message, LtiUserEntity recipient, Map<String, List<LmsSubmission>> lmsSubmissions, boolean isPreview,
            Map<String, MessageConditionalText> conditionalTextCache, Map<String, MessagePipedTextItem> pipedTextItemCache) throws MessageBodyParseException {
        Document document = Jsoup.parse(message.getContent().getHtml());
        List<Element> conditionalTextElements = document.getElementsByTag(MessageContentBodyHtmlElement.TAG_CONDITIONAL_TEXT.getValue());
        AtomicBoolean hasParseError = new AtomicBoolean(false);
        List<String> errors = new ArrayList<>();

        for (Element element : conditionalTextElements) {
            String dataId = element.attr(MessageContentBodyHtmlElement.ATTR_DATA_ID.getValue());
            MessageConditionalText conditionalText;

            try {
                if (isPreview) {
                    conditionalText = message.getContent().getConditionalTexts().stream()
                        .filter(
                            ct -> Strings.CI.equals(ct.getUuid().toString(), dataId)
                        )
                        .findFirst()
                        .orElseThrow(() -> new MessageBodyParseException(String.format("Conditional text with UUID: [%s] not found", dataId)));
                } else {
                    conditionalText = conditionalTextCache.get(dataId);

                    if (conditionalText == null) {
                        conditionalText = conditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(
                            UUID.fromString(dataId),
                            message.getContent().getUuid(),
                            message.getOwner().getLmsUserId()
                        )
                        .orElseThrow(() -> new MessageBodyParseException(String.format("Conditional text with UUID: [%s] not found", dataId)));

                        conditionalTextCache.put(dataId, conditionalText);
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing conditional text with data-id [{}]", dataId, e);
                errors.add(
                    String.format(
                        "Error parsing conditional text '%s'",
                        StringUtils.stripStart(
                            element.attr(MessageContentBodyHtmlElement.ATTR_DATA_LABEL.getValue()),
                            MessageContentBodyHtmlElement.LABEL_PREPEND_CONDITIONAL_TEXT.getValue()
                        )
                    )
                );
                hasParseError.set(true);
                continue;
            }

            if (!ruleComparisonService.conditionalTextRuleSetsMatch(conditionalText, lmsSubmissions)) {
                // conditional text rules do not match; remove the element
                element.remove();
            } else {
                List<Node> replacements = Jsoup.parse(conditionalText.getResult().getHtml()).selectFirst("body").childNodes();
                element.empty();
                element.appendChildren(replacements);
            }
        }

        // process piped text after conditional text, as conditional text may have inserted pipedtext elements
        List<Element> pipedTextElements = document.getElementsByTag(MessageContentBodyHtmlElement.TAG_PIPED_TEXT.getValue());

        for (Element element : pipedTextElements) {
            String dataId = element.attr(MessageContentBodyHtmlElement.ATTR_DATA_ID.getValue());
            MessagePipedTextItem pipedTextItem;

            try {
                if (isPreview) {
                    pipedTextItem = message.getContent().getPipedText().getItems().stream()
                        .filter(item -> Strings.CI.equals(item.getUuid().toString(), dataId))
                        .findFirst()
                        .orElseThrow(() -> new MessageBodyParseException(String.format("Piped text item with UUID: [%s] not found", dataId)));
                } else {
                    pipedTextItem = pipedTextItemCache.get(dataId);

                    if (pipedTextItem == null) {
                        pipedTextItem = pipedTextItemRepository.findByUuidAndPipedText_UuidAndPipedText_Content_UuidAndPipedText_Content_Message_Container_Owner_LmsUserId(
                            UUID.fromString(dataId),
                            message.getContent().getPipedText().getUuid(),
                            message.getContent().getUuid(),
                            message.getOwner().getLmsUserId()
                        )
                        .orElseThrow(() -> new MessageBodyParseException(String.format("Piped text with UUID: [%s] not found", dataId)));

                        pipedTextItemCache.put(dataId, pipedTextItem);
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing piped text with data-id [{}]", dataId, e);
                errors.add(
                    String.format(
                        "Error parsing piped text '%s'",
                        StringUtils.stripStart(
                            element.attr(MessageContentBodyHtmlElement.ATTR_DATA_LABEL.getValue()),
                            MessageContentBodyHtmlElement.LABEL_PREPEND_PIPED_TEXT.getValue()
                        )
                    )
                );
                hasParseError.set(true);
                continue;
            }

            Optional<MessagePipedTextItemValue> pipedTextItemValue = pipedTextItem.getValues().stream()
                .filter(value -> value.getUser().getUserId() == recipient.getUserId())
                .findFirst();

            if (pipedTextItemValue.isEmpty()) {
                log.warn("No piped text value found for user ID [{}] in piped text item with uuid [{}] for message ID: [{}]", recipient.getUserId(), dataId, message.getId());
                element.remove();
            } else {
                Node replacement = Jsoup.parse(pipedTextItemValue.get().getValue()).selectFirst("body").firstChild();
                element.replaceWith(replacement);
            }
        }

        if (hasParseError.get()) {
            // TODO send email to instructor informing of errors
            String errorMessage = String.format("There were errors parsing the message body: %s", String.join(", ", errors));
            log.error(errorMessage);

            return null;
        }

        return MessageType.EMAIL == message.getType() ? document.outerHtml() : document.wholeText();
    }

}
