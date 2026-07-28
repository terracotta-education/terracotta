package edu.iu.terracotta.runner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.ConsentDocument;

public class ConsentDocumentConversionRunnerTest extends BaseTest {

    private ConsentDocumentConversionRunner consentDocumentConversionRunner;

    @TempDir private Path uploadDirPath;
    @TempDir private Path consentFileLocalPathRootPath;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        consentDocumentConversionRunner = new ConsentDocumentConversionRunner(consentDocumentRepository, fileStorageService);

        ReflectionTestUtils.setField(consentDocumentConversionRunner, "uploadDir", uploadDirPath.toString());
        ReflectionTestUtils.setField(consentDocumentConversionRunner, "consentFileLocalPathRoot", consentFileLocalPathRootPath.toString());
    }

    private ConsentDocument validConsentDocument() {
        return ConsentDocument.builder()
            .title("Consent Title")
            .filePointer("file-pointer")
            .experiment(experiment)
            .build();
    }

    private Path createConsentFile(long experimentId, String content) throws Exception {
        Path consentDir = Files.createDirectories(uploadDirPath.resolve(String.valueOf(experimentId)).resolve("consent"));
        Path consentFile = consentDir.resolve("consent.pdf");
        Files.writeString(consentFile, content);

        return consentFile;
    }

    // enabled = false

    @Test
    void testDisabledDoesNotTouchRepository() {
        ReflectionTestUtils.setField(consentDocumentConversionRunner, "enabled", false);

        consentDocumentConversionRunner.onApplicationEvent(null);

        verifyNoInteractions(consentDocumentRepository);
        verifyNoInteractions(fileStorageService);
    }

    // enabled = true, empty result set

    @Test
    void testEnabledWithNoConsentDocumentsDoesNothing() {
        ReflectionTestUtils.setField(consentDocumentConversionRunner, "enabled", true);
        when(consentDocumentRepository.findAll()).thenReturn(Collections.emptyList());

        consentDocumentConversionRunner.onApplicationEvent(null);

        verify(consentDocumentRepository, timeout(2000)).findAll();
        // negative assertion on work done by the background thread; timeout(...).times(0) polls
        // for the full duration instead of racing a fixed sleep against the thread's completion
        verify(consentDocumentRepository, timeout(2000).times(0)).save(any());
        verifyNoInteractions(fileStorageService);
    }

    // filtering

    @Test
    void testAlreadyCompressedConsentDocumentIsSkipped() {
        ReflectionTestUtils.setField(consentDocumentConversionRunner, "enabled", true);
        ConsentDocument compressed = ConsentDocument.builder()
            .title("Already Compressed")
            .filePointer("file-pointer")
            .experiment(experiment)
            .encryptionMethod("AES")
            .encryptionPhrase("phrase")
            .fileUri("uri")
            .build();
        when(consentDocumentRepository.findAll()).thenReturn(List.of(compressed));

        consentDocumentConversionRunner.onApplicationEvent(null);

        verify(consentDocumentRepository, timeout(2000)).findAll();
        verify(consentDocumentRepository, timeout(2000).times(0)).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void testNullFilePointerConsentDocumentIsSkipped() {
        ReflectionTestUtils.setField(consentDocumentConversionRunner, "enabled", true);
        ConsentDocument noFilePointer = ConsentDocument.builder()
            .title("No File Pointer")
            .filePointer(null)
            .experiment(experiment)
            .build();
        when(consentDocumentRepository.findAll()).thenReturn(List.of(noFilePointer));

        consentDocumentConversionRunner.onApplicationEvent(null);

        verify(consentDocumentRepository, timeout(2000)).findAll();
        verify(consentDocumentRepository, timeout(2000).times(0)).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void testNullExperimentConsentDocumentIsSkipped() {
        ReflectionTestUtils.setField(consentDocumentConversionRunner, "enabled", true);
        ConsentDocument noExperiment = ConsentDocument.builder()
            .title("No Experiment")
            .filePointer("file-pointer")
            .experiment(null)
            .build();
        when(consentDocumentRepository.findAll()).thenReturn(List.of(noExperiment));

        consentDocumentConversionRunner.onApplicationEvent(null);

        verify(consentDocumentRepository, timeout(2000)).findAll();
        verify(consentDocumentRepository, timeout(2000).times(0)).save(any());
        verifyNoInteractions(fileStorageService);
    }

    // successful conversion

    @Test
    void testValidConsentDocumentIsConvertedAndSaved() throws Exception {
        ReflectionTestUtils.setField(consentDocumentConversionRunner, "enabled", true);
        long experimentId = experiment.getExperimentId();
        Path originalFile = createConsentFile(experimentId, "dummy consent content");
        ConsentDocument consentDocument = validConsentDocument();
        when(consentDocumentRepository.findAll()).thenReturn(List.of(consentDocument));
        when(fileStorageService.compressFile(anyString(), anyString(), eq(ConsentDocument.COMPRESSED_FILE_EXTENSION))).thenReturn(true);

        consentDocumentConversionRunner.onApplicationEvent(null);

        verify(fileStorageService, timeout(2000)).compressFile(anyString(), anyString(), eq(ConsentDocument.COMPRESSED_FILE_EXTENSION));
        verify(consentDocumentRepository, timeout(2000)).save(
            argThat(saved ->
                saved.getFileUri() != null
                    && saved.getEncryptionMethod() != null
                    && saved.getEncryptionPhrase() != null)
        );
        // the class only ever deletes the relocated pre-compression copy under consentFileLocalPathRoot;
        // the original file under uploadDir is intentionally left untouched by this runner
        assertTrue(Files.exists(originalFile));
    }

    // IOException handling

    @Test
    void testMissingSourceFileIsCaughtAndLoggedWithoutSaving() {
        ReflectionTestUtils.setField(consentDocumentConversionRunner, "enabled", true);
        ConsentDocument consentDocument = validConsentDocument();
        when(consentDocumentRepository.findAll()).thenReturn(List.of(consentDocument));

        assertDoesNotThrow(() -> consentDocumentConversionRunner.onApplicationEvent(null));

        verify(consentDocumentRepository, timeout(2000)).findAll();
        verify(consentDocumentRepository, timeout(2000).times(0)).save(any());
        verifyNoInteractions(fileStorageService);
    }

}
