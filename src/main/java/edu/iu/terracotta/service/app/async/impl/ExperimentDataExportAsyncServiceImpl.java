package edu.iu.terracotta.service.app.async.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.dao.repository.export.data.ExperimentDataExportRepository;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportException;
import edu.iu.terracotta.service.app.ExportService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.async.ExperimentDataExportAsyncService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class ExperimentDataExportAsyncServiceImpl implements ExperimentDataExportAsyncService {

    @Autowired private ExperimentDataExportRepository experimentDataExportRepository;
    @Autowired private ExportService exportService;
    @Autowired private FileStorageService fileStorageService;

    @Async
    @Override
    @Transactional(rollbackFor = { IOException.class })
    public void process(long experimentDataExportId, SecuredInfo securedInfo) throws ExperimentDataExportException, NumberFormatException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, TerracottaConnectorException {
        log.info("Processing experiment data export with ID: [{}]", experimentDataExportId);

        ExperimentDataExport experimentDataExport = experimentDataExportRepository.findById(experimentDataExportId)
                .orElseThrow(() -> new ExperimentDataExportException(String.format("Experiment data export with ID: [%s] not found", experimentDataExportId)));

        try {
            experimentDataExport.setFileName(
                String.format(
                    ExperimentDataExport.FILE_NAME,
                    StringUtils.replace(experimentDataExport.getExperimentTitle(), " ", "_"),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH-mm").format(experimentDataExport.getCreatedAt())
                )
            );
            File zipFile = generateZipFile(exportService.getFiles(experimentDataExport.getExperimentId(), securedInfo));
            fileStorageService.saveExperimentDataExport(experimentDataExport, zipFile);
            experimentDataExport.setStatus(ExperimentDataExportStatus.READY);
            experimentDataExportRepository.save(experimentDataExport);
        } catch (Exception e) {
            experimentDataExport.setStatus(ExperimentDataExportStatus.ERROR);
            experimentDataExportRepository.save(experimentDataExport);
            log.error("Error processing experiment data export with ID: [{}].", experimentDataExport.getId(), e);
            throw new ExperimentDataExportException("Error processing experiment data export", e);
        }

        log.info("Processing experiment data export with ID: [{}] COMPLETE!", experimentDataExport.getId());
    }

    private File generateZipFile(Map<String, String> files) throws IOException {
        File file = Files.createTempFile("export." + UUID.randomUUID().toString(), null).toFile();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
            // .csv files
            for (Map.Entry<String, String> mapEntry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(mapEntry.getKey());
                zipOutputStream.putNextEntry(zipEntry);

                try (FileInputStream fileInputStream = new FileInputStream(mapEntry.getValue())) {
                    byte[] bytes = new byte[1024];
                    int length;

                    while ((length = fileInputStream.read(bytes)) >= 0) {
                        zipOutputStream.write(bytes, 0, length);
                    }
                }

                Files.deleteIfExists(Paths.get(mapEntry.getValue()));

                zipOutputStream.closeEntry();
            }
        }

        return file;
    }

}
