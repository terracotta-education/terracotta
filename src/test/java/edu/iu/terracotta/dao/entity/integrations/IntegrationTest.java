package edu.iu.terracotta.dao.entity.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.Treatment;

/**
 * {@link Integration} is a Lombok {@code @Builder} JPA entity extending {@code BaseUuidEntity}.
 * These tests exercise the two hand-written {@code @Transient} delegation methods:
 * {@code getLaunchUrl()} (delegates to {@code configuration.getLaunchUrl()}) and
 * {@code getLocalUrl()} (delegates through the full
 * question -&gt; assessment -&gt; treatment -&gt; condition -&gt; experiment -&gt; platformDeployment chain).
 */
public class IntegrationTest {

    private static final String LAUNCH_URL = "https://example.com/launch";
    private static final String LOCAL_URL = "https://terracotta.example.com";

    @Test
    public void testGetLaunchUrl() {
        Integration integration = Integration.builder()
            .configuration(
                IntegrationConfiguration.builder()
                    .launchUrl(LAUNCH_URL)
                    .build()
            )
            .build();

        assertEquals(LAUNCH_URL, integration.getLaunchUrl());
    }

    @Test
    public void testGetLocalUrl() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder()
            .localUrl(LOCAL_URL)
            .build();

        Experiment experiment = Experiment.builder()
            .platformDeployment(platformDeployment)
            .build();

        Condition condition = Condition.builder()
            .experiment(experiment)
            .build();

        Treatment treatment = Treatment.builder()
            .condition(condition)
            .build();

        Assessment assessment = Assessment.builder()
            .treatment(treatment)
            .build();

        Question question = Question.builder()
            .assessment(assessment)
            .build();

        Integration integration = Integration.builder()
            .question(question)
            .build();

        assertEquals(LOCAL_URL, integration.getLocalUrl());
    }

}
