package edu.iu.terracotta.service.app.messaging.impl.rules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.enums.WorkflowState;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRule;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRuleSet;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleComparisonDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import edu.iu.terracotta.dao.model.enums.messaging.rule.match.RuleMatch;
import edu.iu.terracotta.dao.model.enums.messaging.rule.match.RuleSetMatch;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.messaging.MessageRuleComparisonService;
import io.jsonwebtoken.lang.Collections;

@Service
public class MessageRuleComparisonServiceImpl implements MessageRuleComparisonService {

    @Autowired private SubmissionService submissionService;
    @Autowired private LmsUtils lmsUtils;

    @Override
    public MessageRuleComparisonDto toDto(MessageRuleComparison ruleComparisonMessage) {
        if (ruleComparisonMessage == null) {
            return null;
        }

        return MessageRuleComparisonDto.builder()
            .id(ruleComparisonMessage)
            .label(ruleComparisonMessage.getLabel())
            .requiresValue(ruleComparisonMessage.isRequiresValue())
            .build();
    }

    @Override
    public MessageRuleComparison fromDto(MessageRuleComparisonDto recipientRuleComparisonDto) {
        if (recipientRuleComparisonDto == null) {
            return null;
        }

        return recipientRuleComparisonDto.getId();
    }

    @Override
    public Map<String, List<LmsSubmission>> getLmsSubmissions(Message message) throws ApiException, TerracottaConnectorException, IOException {
        List<String> lmsAssignmentIds = new ArrayList<>();

        // add all LMS assignment IDs from recipient rules
        for (MessageRecipientRuleSet ruleSet : message.getRuleSets()) {
            for (MessageRecipientRule rule : ruleSet.getRules()) {
                if (!lmsAssignmentIds.contains(rule.getLmsAssignmentId())) {
                    lmsAssignmentIds.add(rule.getLmsAssignmentId());
                }
            }
        }

        // add all LMS assignment IDs from conditional text rules
        for (MessageConditionalText conditionalText : message.getContent().getConditionalTexts()) {
            for (MessageConditionalTextRuleSet ruleSet : conditionalText.getRuleSets()) {
                for (MessageConditionalTextRule rule : ruleSet.getRules()) {
                    if (!lmsAssignmentIds.contains(rule.getLmsAssignmentId())) {
                        lmsAssignmentIds.add(rule.getLmsAssignmentId());
                    }
                }
            }
        }

        if (lmsAssignmentIds.isEmpty()) {
            // no LMS assignment IDs to retrieve submissions; skip fetching submissions
            return Collections.emptyMap();
        }

        return submissionService.getAllSubmissionsForMultipleAssignments(
            message.getOwner(),
            lmsUtils.parseCourseId(message.getPlatformDeployment(), message.getExperiment().getLtiContextEntity().getContext_memberships_url()),
            lmsAssignmentIds
        );
    }

    @Override
    public boolean conditionalTextRuleSetsMatch(MessageConditionalText conditionalText, Map<String, List<LmsSubmission>> lmsSubmissions) {
        boolean noLmsSubmissions = lmsSubmissions.values().stream()
            .allMatch(lmsSubmission -> CollectionUtils.isEmpty(lmsSubmission));

        if (noLmsSubmissions) {
            // no LMS submissions to match against; return false
            return false;
        }

        List<RuleSetMatch> ruleSetMatches = new ArrayList<>();

        for (MessageConditionalTextRuleSet conditionalTextRuleSet : conditionalText.getRuleSets()) {
            ruleSetMatches.add(
                RuleSetMatch.builder()
                    .ruleSetUuid(conditionalTextRuleSet.getUuid())
                    .operator(conditionalTextRuleSet.getOperator())
                    .build()
            );

            for (MessageConditionalTextRule conditionalTextRule : conditionalTextRuleSet.getRules()) {
                RuleMatch ruleMatch = RuleMatch.builder()
                    .ruleUuid(conditionalTextRule.getUuid())
                    .comparison(conditionalTextRule.getComparison())
                    .operator(conditionalTextRule.getOperator())
                    .value(conditionalTextRule.getValue())
                    .build();
                ruleSetMatches.getLast().addRuleMatch(ruleMatch);

                // get all submissions for this rule; ordered by attempt descending order (last to first)
                List<LmsSubmission> submissionsForRule = lmsSubmissions.get(conditionalTextRule.getLmsAssignmentId()).stream()
                    .sorted(Comparator.comparingLong(LmsSubmission::getAttempt).reversed())
                    .toList();

                ruleMatches(ruleMatch, submissionsForRule);
            }

            ruleSetMatches(ruleSetMatches.getLast());
        }

        return allRuleSetsMatch(ruleSetMatches);
    }

    @Override
    public boolean recipientRuleSetsMatch(List<MessageRecipientRuleSet> ruleSets, Map<String, List<LmsSubmission>> lmsSubmissions) {
        if (CollectionUtils.isEmpty(ruleSets)) {
            // no rule sets to match; return true
            return true;
        }

        boolean allLmsSubmissionsEmpty = lmsSubmissions.values().stream()
            .allMatch(lmsSubmission -> CollectionUtils.isEmpty(lmsSubmission));

        if (allLmsSubmissionsEmpty) {
            // no LMS submissions to match against; return false
            return false;
        }

        List<RuleSetMatch> ruleSetMatches = new ArrayList<>();

        for (MessageRecipientRuleSet ruleSet : ruleSets) {
            ruleSetMatches.add(
                RuleSetMatch.builder()
                    .ruleSetUuid(ruleSet.getUuid())
                    .operator(ruleSet.getOperator())
                    .build()
            );

            for (MessageRecipientRule rule : ruleSet.getRules()) {
                RuleMatch ruleMatch = RuleMatch.builder()
                    .ruleUuid(rule.getUuid())
                    .comparison(rule.getComparison())
                    .operator(rule.getOperator())
                    .value(rule.getValue())
                    .build();
                ruleSetMatches.getLast().addRuleMatch(ruleMatch);

                // get all submissions for this rule; ordered by attempt descending order (last to first)
                List<LmsSubmission> submissionsForRule = lmsSubmissions.get(rule.getLmsAssignmentId()).stream()
                    .sorted(Comparator.comparingLong(LmsSubmission::getAttempt).reversed())
                    .toList();

                ruleMatches(ruleMatch, submissionsForRule);
            }

            ruleSetMatches(ruleSetMatches.getLast());
        }

        return allRuleSetsMatch(ruleSetMatches);
    }

    private void ruleMatches(RuleMatch ruleMatch, List<LmsSubmission> submissionsForRule) {
        // if a value is required, check for existence of a submission and check the score matches the condition
        if (ruleMatch.getComparison().isRequiresValue()) {
            if (CollectionUtils.isEmpty(submissionsForRule)) {
                // no submissions for the rule, but score is required; set rule matched to false
                ruleMatch.setMatch(false);
                return;
            }

            Optional<LmsSubmission> submissionToCheck = submissionsForRule.stream()
                .filter(LmsSubmission::isGradeMatchesCurrentSubmission)
                .findFirst();

            if (submissionToCheck.isEmpty()) {
                // no submission found that matches the current submission; set rule matched to false
                ruleMatch.setMatch(false);
                return;
            }

            if (submissionToCheck.get().getScore() == null) {
                // no score for the submission; set rule matched to false
                ruleMatch.setMatch(false);
                return;
            }

            // score precision allowed
            double epsilon = 0.000001d;

            switch (ruleMatch.getComparison()) {
                case EQUALS:
                    ruleMatch.setMatch(
                        Precision.equals(
                            submissionToCheck.get().getScore(),
                            Double.parseDouble(ruleMatch.getValue()),
                            epsilon
                        )
                    );
                    break;
                case GREATER_THAN:
                    ruleMatch.setMatch(
                        Precision.compareTo(
                            submissionToCheck.get().getScore(),
                            Double.parseDouble(ruleMatch.getValue()),
                            epsilon
                        ) > 0
                    );
                    break;
                case GREATER_THAN_EQUAL:
                    ruleMatch.setMatch(
                        Precision.compareTo(
                            submissionToCheck.get().getScore(),
                            Double.parseDouble(ruleMatch.getValue()),
                            epsilon
                        ) >= 0
                    );
                    break;
                case LESS_THAN:
                    ruleMatch.setMatch(
                        Precision.compareTo(
                            submissionToCheck.get().getScore(),
                            Double.parseDouble(ruleMatch.getValue()),
                            epsilon
                        ) < 0
                    );
                    break;
                case LESS_THAN_EQUAL:
                    ruleMatch.setMatch(
                        Precision.compareTo(
                            submissionToCheck.get().getScore(),
                            Double.parseDouble(ruleMatch.getValue()),
                            epsilon
                        ) <= 0
                    );
                    break;
                default:
                    break;
            }

            return;
        }

        // no value required; match the condition
        switch (ruleMatch.getComparison()) {
            case IS_NOT_YET_SUBMITTED:
                // true if no submissions for the rule
                ruleMatch.setMatch(
                    CollectionUtils.isEmpty(submissionsForRule)
                        || submissionsForRule.stream().anyMatch(lmsSubmission -> lmsSubmission.getState().equals(WorkflowState.UNSUBMITTED.state()))
                );
                break;
            case IS_SUBMITTED:
                // true if there are submissions for the rule
                ruleMatch.setMatch(
                    CollectionUtils.isNotEmpty(submissionsForRule)
                        && submissionsForRule.stream().allMatch(lmsSubmission -> List.of(WorkflowState.GRADED.state(), WorkflowState.SUBMITTED.state()).contains(lmsSubmission.getState()))
                );
                break;
            case IS_SUBMITTED_BUT_NOT_YET_GRADED:
                // true if there are submissions for the rule but no score for the latest submission
                ruleMatch.setMatch(
                    CollectionUtils.isNotEmpty(submissionsForRule)
                        && submissionsForRule.get(0).isGradeMatchesCurrentSubmission()
                        && submissionsForRule.get(0).getState().equals(WorkflowState.PENDING_REVIEW.state())
                );
                break;
            default:
                break;
        }
    }

    private void ruleSetMatches(RuleSetMatch ruleSetMatch) {
        // calculate the cumulative rules logic for this ruleset
        Iterator<RuleMatch> iterator = ruleSetMatch.getRuleMatches().iterator();
        boolean cumulativeRuleValue = false;

        while (iterator.hasNext()) {
            RuleMatch ruleMatch = iterator.next();
            cumulativeRuleValue = operatorMatch(ruleMatch.getOperator(), ruleMatch.isMatch(), cumulativeRuleValue);
        }

        ruleSetMatch.setMatch(cumulativeRuleValue);
    }

    private boolean allRuleSetsMatch(List<RuleSetMatch> ruleSetMatches) {
        // calculate the cumulative ruleset logic
        Iterator<RuleSetMatch> iterator = ruleSetMatches.iterator();
        boolean cumulativeRuleSetValue = false;

        while (iterator.hasNext()) {
            RuleSetMatch ruleSetMatch = iterator.next();
            cumulativeRuleSetValue = operatorMatch(ruleSetMatch.getOperator(), ruleSetMatch.isMatch(), cumulativeRuleSetValue);
        }

        return cumulativeRuleSetValue;
    }

    private boolean operatorMatch(MessageRuleOperator operator, boolean isMatch, boolean cumulativeValue) {
        switch (operator) {
            case AND:
                return cumulativeValue && isMatch;
            case OR:
                return cumulativeValue || isMatch;
            case NONE:
            default:
                return isMatch;
        }
    }

}
