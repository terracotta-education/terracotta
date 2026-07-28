package edu.iu.terracotta.service.app.messaging.impl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;

public class MessageRuleAssignmentServiceImplTest extends BaseTest {

    @InjectMocks private MessageRuleAssignmentServiceImpl messageRuleAssignmentService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testToDtoPoints() {
        when(lmsAssignment.getGradingType()).thenReturn("points");

        MessageRuleAssignmentDto dto = messageRuleAssignmentService.toDto(lmsAssignment);

        assertEquals("1", dto.getLmsId());
        assertEquals("points", dto.getGradingType());
        assertEquals(ASSIGNMENT_TITLE, dto.getTitle());
        assertEquals(8, dto.getComparisons().size());
        assertTrue(dto.getComparisons().stream().anyMatch(comparison -> comparison.getId() == MessageRuleComparison.GREATER_THAN));
    }

    @Test
    public void testToDtoPassFail() {
        when(lmsAssignment.getGradingType()).thenReturn("pass_fail");

        MessageRuleAssignmentDto dto = messageRuleAssignmentService.toDto(lmsAssignment);

        assertEquals(3, dto.getComparisons().size());
        assertTrue(dto.getComparisons().stream().allMatch(comparison -> !comparison.isRequiresValue()));
    }

    @Test
    public void testToDtoUnknownGradingTypeDefaultsToPassFail() {
        when(lmsAssignment.getGradingType()).thenReturn("not_a_real_type");

        MessageRuleAssignmentDto dto = messageRuleAssignmentService.toDto(lmsAssignment);

        assertEquals(3, dto.getComparisons().size());
    }

    @Test
    public void testToDtoComparisonFieldsCopiedFromEnum() {
        when(lmsAssignment.getGradingType()).thenReturn("points");

        MessageRuleAssignmentDto dto = messageRuleAssignmentService.toDto(lmsAssignment);

        dto.getComparisons().forEach(comparison -> {
            assertEquals(comparison.getId().getLabel(), comparison.getLabel());
            assertEquals(comparison.getId().isRequiresValue(), comparison.isRequiresValue());
        });
    }

    @Test
    public void testToDtoList() {
        when(lmsAssignment.getGradingType()).thenReturn("points");

        List<MessageRuleAssignmentDto> dtos = messageRuleAssignmentService.toDto(List.of(lmsAssignment, lmsAssignment));

        assertEquals(2, dtos.size());
    }

    @Test
    public void testToDtoListNull() {
        List<MessageRuleAssignmentDto> dtos = messageRuleAssignmentService.toDto((List<LmsAssignment>) null);

        assertTrue(dtos.isEmpty());
    }

    @Test
    public void testFromDto() {
        MessageRuleAssignmentDto dto = MessageRuleAssignmentDto.builder().lmsId("42").build();

        String lmsId = messageRuleAssignmentService.fromDto(dto);

        assertEquals("42", lmsId);
    }

}
