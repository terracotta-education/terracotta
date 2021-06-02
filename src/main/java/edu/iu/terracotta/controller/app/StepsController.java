package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.model.app.dto.StepDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value = StepsController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class StepsController {

    static final Logger log = LoggerFactory.getLogger(StepsController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    ExposureService exposureService;

    @Autowired
    ParticipantService participantService;

    @Autowired
    GroupService groupService;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    ExperimentService experimentService;

    final static String EXPOSURE_TYPE = "exposure_type";
    final static String PARTICIPATION_TYPE = "participation_type";
    final static String DISTRIBUTION_TYPE = "distribution_type";
    final static String DISTRIBUTION_CUSTOM = "distribution_type_custom";


    @RequestMapping(value = "/{experiment_id}/step", method = RequestMethod.POST)
    public ResponseEntity<ExposureDto> postExposure(@PathVariable("experiment_id") Long experimentId,
                                                    @RequestBody StepDto stepDto,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException, ParticipantNotUpdatedException, ExperimentStartedException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        switch (stepDto.getStep()) {
            case EXPOSURE_TYPE: // We create the exposures.
                if(apijwtService.isInstructorOrHigher(securityInfo)) {
                    exposureService.createExposures(experimentId);
                }else {
                    return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }
                return new ResponseEntity(HttpStatus.OK);
            case PARTICIPATION_TYPE: //We prepare the participants with the right consent and consent related dates.
                if(apijwtService.isInstructorOrHigher(securityInfo)) {
                    participantService.prepareParticipation(experimentId, securityInfo);
                }else {
                    return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }
                return new ResponseEntity(HttpStatus.OK);
            case DISTRIBUTION_TYPE: //We prepare the groups once the distribution type is selected.
                if(apijwtService.isInstructorOrHigher(securityInfo)) {
                    groupService.createAndAssignGroupsToConditionsAndExposures(experimentId,securityInfo,false);
                }else {
                    return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }
                return new ResponseEntity(HttpStatus.OK);
            case DISTRIBUTION_CUSTOM: //For the custom case, we prepare the groups and assign people based on the custom percents.
                /*if(apijwtService.isInstructorOrHigher(securityInfo)) {
                    groupService.createAndAssignGroupsToConditionsAndExposures(experimentId,securityInfo,true);
                }else {
                    return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }*/
                //NOT DOING ANYTHING NOW...
                return new ResponseEntity(HttpStatus.OK);
            default:
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }
}
