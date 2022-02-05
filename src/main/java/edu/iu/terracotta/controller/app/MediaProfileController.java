package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.app.dto.media.MediaEventDto;
import org.imsglobal.caliper.events.MediaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = MediaProfileController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class MediaProfileController {

    static final Logger log = LoggerFactory.getLogger(GroupController.class);
    static final String REQUEST_ROOT = "api/media";


//    @RequestMapping(value = "/{experiment_id}/groups", method = RequestMethod.GET, produces = "application/json")
//    @ResponseBody
//    public ResponseEntity<List<GroupDto>> getAllMediaEvents(@PathVariable("experiment_id") Long experimentId,
//                                                                HttpServletRequest req)
//            throws ExperimentNotMatchingException, BadTokenException {
//        MediaEvent
//
//
//    }

//    @RequestMapping(value = "/{experiment_id}/groups/{group_id}", method = RequestMethod.GET, produces = "application/json")
//    @ResponseBody
//    public ResponseEntity<MediaEvent> getMediaEvent(@PathVariable("experiment_id") long experimentId,
//                                             @PathVariable("group_id") long groupId,
//                                             HttpServletRequest req)
//            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException {
//
//
//    }

    @RequestMapping(value = "/{media_event_id}/events", method = RequestMethod.POST)
    public ResponseEntity<MediaEvent> postMediaEvent(@PathVariable("media_event_id") Long mediaEventId,
                                                     @RequestBody MediaEventDto mediaEventDto,
                                                     UriComponentsBuilder ucBuilder,
                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, IdInPostException, DataServiceException {
        try {
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
//
//    @RequestMapping(value = "/{experiment_id}/groups/{group_id}", method = RequestMethod.PUT)
//    public ResponseEntity<Void> updateMediaEvent(@PathVariable("experiment_id") Long experimentId,
//                                            @PathVariable("group_id") Long groupId,
//                                            @RequestBody GroupDto groupDto,
//                                            HttpServletRequest req)
//            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, TitleValidationException {
//
//    }
//
//    @RequestMapping(value = "/{experiment_id}/groups/{group_id}", method = RequestMethod.DELETE)
//    public ResponseEntity<Void> deleteMediaEvent(@PathVariable("experiment_id") Long experimentId,
//                                            @PathVariable("group_id") Long groupId,
//                                            HttpServletRequest req)
//            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, ExperimentLockedException {
//
//
//    }
}
