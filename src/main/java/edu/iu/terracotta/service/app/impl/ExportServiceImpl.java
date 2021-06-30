package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    AllRepositories allRepositories;

    @Override
    public Map<String, List<String[]>> getCsvFiles(Long experimentId){
        Map<String, List<String[]>> csvFiles = new HashMap<>();
        List<Condition> conditions = allRepositories.conditionRepository.findByExperiment_ExperimentId(experimentId);
        List<String[]> conditionNames = new ArrayList<>();
        conditionNames.add(new String[]{"Name", "Id"});
        for(Condition condition : conditions){
            conditionNames.add(new String[]{ condition.getName(), condition.getConditionId().toString()});
        }
        csvFiles.put("conditions.csv", conditionNames);

        List<Participant> participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        List<String[]> participantNames = new ArrayList<>();
        participantNames.add(new String[]{"Name", "Id"});
        for(Participant participant : participants){
            participantNames.add(new String[] {participant.getLtiUserEntity().getDisplayName(), participant.getParticipantId().toString()});
        }
        csvFiles.put("participants.csv", participantNames);
        return csvFiles;
    }
}
