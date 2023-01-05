package edu.iu.terracotta.controller.app;

import com.google.common.net.HttpHeaders;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.FileStorageService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

@Slf4j
@Controller
@RequestMapping("files")
public class FileDownloadController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private APIJWTService apijwtService;

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId,
                                                 @RequestParam(name = "token") String token,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        if (!apijwtService.validateFileToken(token, fileId)){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Resource resource = fileStorageService.getFileAsResource(fileId);
        String contentType = null;

        try {
            contentType = req.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        }catch (IOException ex) {
            log.error("Could not determine file type.");
        }

        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", resource.getFilename()))
                .body(resource);
    }

}
