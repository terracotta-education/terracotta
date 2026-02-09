package edu.iu.terracotta.connectors.brightspace.io.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Assignment extends BaseBrightspaceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default private ContentObjectModule contentObjectModule = ContentObjectModule.builder().build();
    @Builder.Default private ContentObjectModuleUpdate contentObjectModuleUpdate = ContentObjectModuleUpdate.builder().build();
    @Builder.Default private ContentObjectTopic contentObjectTopicLtiLink = ContentObjectTopic.builder().build();
    @Builder.Default private ContentObjectTopicUpdate contentObjectTopicLtiLinkUpdate = ContentObjectTopicUpdate.builder().build();
    @Builder.Default private ContentObjectTopic contentObjectTopicDropboxLink = ContentObjectTopic.builder().build();
    @Builder.Default private ContentObjectTopicUpdate contentObjectTopicDropboxLinkUpdate = ContentObjectTopicUpdate.builder().build();
    @Builder.Default private DropboxFolder dropboxFolder = DropboxFolder.builder().build();
    @Builder.Default private DropboxFolderUpdate dropboxFolderUpdate = DropboxFolderUpdate.builder().build();
    @Builder.Default private LtiAdvantageLink dropboxFolderLink = LtiAdvantageLink.builder().build();
    @Builder.Default private LtiAdvantageLinkUpdate dropboxFolderLinkUpdate = LtiAdvantageLinkUpdate.builder().build();
    @Builder.Default private LtiAdvantageQuickLink dropboxFolderQuickLink = LtiAdvantageQuickLink.builder().build();
    @Builder.Default private LtiAdvantageLink ltiAdvantageLink = LtiAdvantageLink.builder().build();
    @Builder.Default private LtiAdvantageLinkUpdate ltiAdvantageLinkUpdate = LtiAdvantageLinkUpdate.builder().build();
    @Builder.Default private LtiAdvantageQuickLink ltiAdvantageQuickLink = LtiAdvantageQuickLink.builder().build();
    @Builder.Default private LineItem lineItem = LineItem.builder().build();
    @Builder.Default private LineItem lineItemUpdate = LineItem.builder().build();
    @Builder.Default private GradeObject gradeObject = GradeObject.builder().build();

}
