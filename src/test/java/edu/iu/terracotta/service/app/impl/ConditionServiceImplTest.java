package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ConditionService;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Test is broken and needs to be updated")
class ConditionServiceImplTest {

    /*
    This is a sample unit/integration test for the ConditionServiceImpl class. Only a few methods are shown as an example for future implementation.
    Currently, there is one experiment with id 1, and two conditions with ids 1 and 2 being preloaded into the database. More information can be found on these
    preloaded entities under the main/resources/test_data directory.
     */

    @Autowired
    ConditionService conditionService;

    @Autowired
    AllRepositories allRepositories;

    @Test
    @Order(1)
    @DisplayName("findAllyByExperimentId() happy path. Should return a list of 2 conditions.")
    void test_happy_findAllByExperimentId() {
        List<ConditionDto> conditions = conditionService.findAllByExperimentId(1L);
        assertEquals(2, conditions.size());
    }

    @Test
    @DisplayName("toDto() happy path. Should return a condition dto that matches the values listed in the `expected` column")
    void test_happy_toDto() {
        ConditionDto conditionDto = conditionService.toDto(allRepositories.conditionRepository.findByConditionId(1L));
        assertEquals("Condition A", conditionDto.getName());
        assertEquals(1, conditionDto.getConditionId());
        assertEquals(1, conditionDto.getExperimentId());
        assertEquals(50, conditionDto.getDistributionPct());
    }

    @Test
    @DisplayName("fromDto() happy path. should return a condition that has the same values as the condition dto.")
    void test_happy_fromDto() throws DataServiceException {
        ConditionDto conditionDto = new ConditionDto();
        conditionDto.setDistributionPct(25);
        conditionDto.setName("Condition C");
        conditionDto.setExperimentId(1L);
        conditionDto.setDefaultCondition(false);

        Condition condition = conditionService.fromDto(conditionDto);
        assertNull(condition.getConditionId());
        assertEquals(1, condition.getExperiment().getExperimentId());
        assertEquals("Condition C", condition.getName());
        assertEquals(25, condition.getDistributionPct());
        assertFalse(condition.getDefaultCondition());
    }

    @Test
    @DisplayName("fromDto() invalid path. Should throw a DataServiceException because the experiment does not exist.")
    void test_invalid_fromDto(){
        ConditionDto conditionDto = new ConditionDto();
        conditionDto.setDistributionPct(25);
        conditionDto.setName("Condition C");
        conditionDto.setExperimentId(4L);
        conditionDto.setDefaultCondition(false);
        assertThrows(DataServiceException.class, () -> conditionService.fromDto(conditionDto));
    }

    @Test
    @DisplayName("save() happy path. Should save a condition to the condition repository.")
    void test_happy_save(){
        Condition condition = new Condition();
        condition.setName("Condition C");
        condition.setDefaultCondition(false);
        condition.setDistributionPct(25F);
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(1L);
        if(experiment.isPresent()){
            condition.setExperiment(experiment.get());
        } else{
            fail("experiment not found");
        }
        conditionService.save(condition);
        assertEquals(3, conditionService.findAllByExperimentId(1L).size());
    }

    @Test
    @DisplayName("findById() happy path. The condition with id 2 should be found.")
    void test_happy_findById() {
        Optional<Condition> condition = conditionService.findById(2L);
        assertTrue(condition.isPresent());
    }

    @Test
    @DisplayName("findById() invalid path. No condition with id 6 should exist")
    void test_invalid_findById() {
        Optional<Condition> condition = conditionService.findById(6L);
        assertFalse(condition.isPresent(), "no condition should be found.");
    }

    @Test
    @DisplayName("updateCondition() invalid path. Should update the defaultCondition, name, and distributionPct attributes, but not the ids.")
    void test_invalid_updateCondition() {
        ConditionDto conditionDto = new ConditionDto();
        conditionDto.setConditionId(7L);
        conditionDto.setExperimentId(5L);
        conditionDto.setDefaultCondition(true);
        conditionDto.setName("new name");
        conditionDto.setDistributionPct(75F);

        Condition condition = allRepositories.conditionRepository.findByConditionId(2L);
        Map<Condition, ConditionDto> map = new HashMap<>();
        map.put(condition,conditionDto);
        conditionService.updateCondition(map);
        assertEquals(1, condition.getExperiment().getExperimentId());
        assertEquals(2, condition.getConditionId());
        assertTrue(condition.getDefaultCondition());
        assertEquals(75, condition.getDistributionPct());
        assertEquals("new name", condition.getName());
    }

    @Test
    @DisplayName("nameAlreadyExists() invalid path. Should return true because the name 'Condition A' already exists")
    void test_invalid_nameAlreadyExists() {
        assertTrue(conditionService.nameAlreadyExists("Condition A", 1L, 3L));
    }
}
