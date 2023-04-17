package edu.iu.terracotta.runner;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.FileSubmissionLocal;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.model.enums.EncryptionMethod;

@Slf4j
@Component
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ConsentDocumentConversionRunner implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.consent.documents.conversion.enabled:false}")
    private boolean enabled;

    @Value("${upload.path}")
    private String uploadDir;

    @Value("${consent.file.local.path.root}")
    private String consentFileLocalPathRoot;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH");

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!enabled) {
            return;
        }

        Thread thread = new Thread(
            () ->
                {
                    // fix experiment started date; if has an assignment started, mark experiment started, else set to null
                    List<ConsentDocument> consentDocuments = allRepositories.consentDocumentRepository.findAll();

                    log.info("Starting conversion of consent documents...");
                    AtomicInteger processed = new AtomicInteger(0);

                    CollectionUtils.emptyIfNull(consentDocuments).stream()
                        .filter(consentDocument -> consentDocument.getFilePointer() != null)
                        .filter(consentDocument -> consentDocument.getExperiment() != null)
                        .filter(consentDocument -> !consentDocument.isCompressed())
                        .forEach(
                            consentDocument -> {
                                log.info("Converting consent document '{}' for experiment ID: '{}'", consentDocument.getTitle(), consentDocument.getExperiment().getExperimentId());

                                try {
                                    File oldFile = new File(String.format("%s/%s/consent/consent.pdf", uploadDir, consentDocument.getExperiment().getExperimentId()));

                                    // get file creation time
                                    BasicFileAttributes attr = Files.readAttributes(oldFile.toPath(), BasicFileAttributes.class);
                                    String path = attr.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_FORMATTER);

                                    // create the upload directory
                                    Files.createDirectories(Paths.get(String.format("%s/%s", consentFileLocalPathRoot, path)));
                                    // assign random UUID as file name
                                    String filePath = String.format("%s/%s", path, UUID.randomUUID().toString());

                                    while (Files.exists(Paths.get(filePath))) {
                                        // ensure no file name clashes
                                        filePath = String.format("%s/%s", path, UUID.randomUUID().toString());
                                    }

                                    // copy the file to the directory
                                    Files.copy(
                                        oldFile.toPath(),
                                        Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)),
                                        StandardCopyOption.REPLACE_EXISTING
                                    );

                                    String encryptionPhrase = UUID.randomUUID().toString();

                                    FileSubmissionLocal fileSubmissionLocal =  new FileSubmissionLocal(
                                        filePath,
                                        fileStorageService.compressFile(
                                            Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)).toString(),
                                            encryptionPhrase,
                                            ConsentDocument.COMPRESSED_FILE_EXTENSION
                                        ),
                                        EncryptionMethod.AES.toString(),
                                        encryptionPhrase
                                    );

                                    // delete the original file
                                    Files.deleteIfExists(Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)));

                                    consentDocument.setEncryptionMethod(fileSubmissionLocal.getEncryptionMethod());
                                    consentDocument.setEncryptionPhrase(encryptionPhrase);
                                    consentDocument.setFileUri(fileSubmissionLocal.getFilePath());

                                    allRepositories.consentDocumentRepository.save(consentDocument);

                                    log.info(
                                        "Converted consent document '{}' for experiment ID: '{}'. Old path: '{}'. New path: '{}'",
                                        consentDocument.getTitle(),
                                        consentDocument.getExperiment().getExperimentId(),
                                        oldFile.toPath().toString(),
                                        String.format("%s/%s%s", consentFileLocalPathRoot, filePath, ConsentDocument.COMPRESSED_FILE_EXTENSION)
                                    );
                                } catch (IOException e) {
                                    log.error("Error converting consent document '{}' for experiment ID: '{}", consentDocument.getTitle(), consentDocument.getExperiment().getExperimentId(), e);
                                }

                                processed.incrementAndGet();
                        });

                    log.info("Consent document conversion complete! {} consent documents processed.", processed);
                }
        );

        thread.start();
    }

}
