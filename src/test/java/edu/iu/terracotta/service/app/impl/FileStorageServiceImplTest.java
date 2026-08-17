package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
import edu.iu.terracotta.dao.model.distribute.export.Export;
import edu.iu.terracotta.dao.model.distribute.export.ExperimentExport;
import edu.iu.terracotta.dao.model.dto.FileInfoDto;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.dao.repository.export.data.ExperimentDataExportRepository;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.exceptions.app.FileStorageException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;

public class FileStorageServiceImplTest extends BaseTest {

    @Mock private AssignmentFileArchiveRepository assignmentFileArchiveRepository;
    @Mock private ExperimentDataExportRepository exportDataRepository;
    @Mock private ApiJwtService apijwtService;

    private FileStorageServiceImpl fileStorageService;

    @TempDir private Path tempDir;

    private Path consentRoot;
    private Path submissionsRoot;
    private Path archiveRoot;
    private Path dataExportRoot;
    private Path experimentExportRoot;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        fileStorageService = new FileStorageServiceImpl(
            answerFileSubmissionRepository,
            assignmentFileArchiveRepository,
            consentDocumentRepository,
            experimentImportRepository,
            experimentRepository,
            exportDataRepository,
            lmsUtils,
            ltiUserRepository,
            apiClient,
            apijwtService
        );

        consentRoot = Files.createDirectories(tempDir.resolve("consent"));
        submissionsRoot = Files.createDirectories(tempDir.resolve("submissions"));
        archiveRoot = Files.createDirectories(tempDir.resolve("archive"));
        dataExportRoot = Files.createDirectories(tempDir.resolve("data_export"));
        experimentExportRoot = Files.createDirectories(tempDir.resolve("experiment_export"));

        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.resolve("uploads").toString());
        ReflectionTestUtils.setField(fileStorageService, "uploadSubmissionsLocalPath", "assignment_submissions");
        ReflectionTestUtils.setField(fileStorageService, "uploadSubmissionsLocalPathRoot", submissionsRoot.toString());
        ReflectionTestUtils.setField(fileStorageService, "consentFileLocalPath", "consent");
        ReflectionTestUtils.setField(fileStorageService, "consentFileLocalPathRoot", consentRoot.toString());
        ReflectionTestUtils.setField(fileStorageService, "assignmentFileArchiveLocalPath", "assignment_file_archive");
        ReflectionTestUtils.setField(fileStorageService, "assignmentFileArchiveLocalPathRoot", archiveRoot.toString());
        ReflectionTestUtils.setField(fileStorageService, "experimentDataExportLocalPath", "experiment_data_export");
        ReflectionTestUtils.setField(fileStorageService, "experimentDataExportLocalPathRoot", dataExportRoot.toString());
        ReflectionTestUtils.setField(fileStorageService, "experimentExportLocalPath", "experiment_export");
        ReflectionTestUtils.setField(fileStorageService, "experimentExportLocalPathRoot", experimentExportRoot.toString());

        ReflectionTestUtils.setField(fileStorageService, "decompressedSubmissionFileTempDirectory", Files.createDirectories(tempDir.resolve("decompressed-submission")));
        ReflectionTestUtils.setField(fileStorageService, "decompressedConsentFileTempDirectory", Files.createDirectories(tempDir.resolve("decompressed-consent")));
        ReflectionTestUtils.setField(fileStorageService, "decompressedAssignmentFileArchiveTempDirectory", Files.createDirectories(tempDir.resolve("decompressed-archive")));
        ReflectionTestUtils.setField(fileStorageService, "decompressedExperimentDataExportTempDirectory", Files.createDirectories(tempDir.resolve("decompressed-data-export")));
        ReflectionTestUtils.setField(fileStorageService, "decompressedExperimentImportTempDirectory", Files.createDirectories(tempDir.resolve("decompressed-import")));

        when(apijwtService.unsecureToken(anyString(), any())).thenReturn(jwt);
    }

    @Test
    public void testGetConsentFileNotFoundThrows() {
        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.empty());

        assertThrows(MyFileNotFoundException.class, () -> fileStorageService.getConsentFile(1L));
    }

    @Test
    public void testGetConsentFileNotCompressedReturnsResource() throws IOException {
        Files.createDirectories(consentRoot.resolve("sub"));
        Path file = consentRoot.resolve("sub/plainfile.txt");
        Files.writeString(file, "plain content");

        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.of(consentDocument));
        when(consentDocument.isCompressed()).thenReturn(false);
        when(consentDocument.getFileUri()).thenReturn("sub/plainfile.txt");

        Resource resource = fileStorageService.getConsentFile(1L);

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals("plain content", Files.readString(resource.getFile().toPath()));
    }

    @Test
    public void testGetConsentFileCompressedDecompressesAndReturnsResource() throws IOException {
        Path dir = Files.createDirectories(consentRoot.resolve("sub"));
        Path plain = dir.resolve("secret-consent.txt");
        Files.writeString(plain, "secret consent content");
        String phrase = "consentPhrase";

        assertTrue(fileStorageService.compressFile(plain.toString(), phrase, ".zip"));
        Files.delete(plain);

        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.of(consentDocument));
        when(consentDocument.isCompressed()).thenReturn(true);
        when(consentDocument.getEncryptedFileUri()).thenReturn("sub/secret-consent.txt.zip");
        when(consentDocument.getEncryptionPhrase()).thenReturn(phrase);
        when(consentDocument.getEncodedFileName()).thenReturn("secret-consent.txt");

        Resource resource = fileStorageService.getConsentFile(1L);

        assertTrue(resource.exists());
        assertEquals("secret consent content", Files.readString(resource.getFile().toPath()));
    }

    @Test
    public void testGetConsentFileDecompressionFailureThrowsFileStorageException() {
        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.of(consentDocument));
        when(consentDocument.isCompressed()).thenReturn(true);
        when(consentDocument.getEncryptedFileUri()).thenReturn("does-not-exist.zip");
        when(consentDocument.getEncryptionPhrase()).thenReturn("whatever");

        assertThrows(FileStorageException.class, () -> fileStorageService.getConsentFile(1L));
    }

    @Test
    public void testSaveConsentFileMultipartFileSuccess() throws IOException {
        MockMultipartFile multipart = new MockMultipartFile("file", "my file.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        FileSubmissionLocal result = fileStorageService.saveConsentFile(multipart);

        assertNotNull(result);
        assertTrue(result.compressed());
        assertEquals("AES", result.encryptionMethod());
        assertNotNull(result.encryptionPhrase());
        assertTrue(Files.exists(consentRoot.resolve(result.filePath() + ".zip")));
        assertFalse(Files.exists(consentRoot.resolve(result.filePath())));
    }

    @Test
    public void testSaveConsentFileMultipartFileIoExceptionThrows() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("bad.txt");
        when(multipartFile.getInputStream()).thenThrow(new IOException("boom"));

        assertThrows(FileStorageException.class, () -> fileStorageService.saveConsentFile(multipartFile));
    }

    @Test
    public void testSaveConsentFileInputStreamNullThrows() {
        assertThrows(FileStorageException.class, () -> fileStorageService.saveConsentFile((InputStream) null, "test.txt"));
    }

    @Test
    public void testSaveConsentFileInputStreamInvalidFilenameThrows() {
        InputStream inputStream = new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8));

        assertThrows(FileStorageException.class, () -> fileStorageService.saveConsentFile(inputStream, "../evil.txt"));
    }

    @Test
    public void testSaveConsentFileInputStreamSuccess() {
        InputStream inputStream = new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8));

        FileSubmissionLocal result = fileStorageService.saveConsentFile(inputStream, "note.txt");

        assertTrue(result.compressed());
        assertEquals("AES", result.encryptionMethod());
    }

    @Test
    public void testSaveConsentFileInputStreamIoExceptionThrows() {
        InputStream throwing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failure");
            }
        };

        assertThrows(FileStorageException.class, () -> fileStorageService.saveConsentFile(throwing, "note2.txt"));
    }

    @Test
    public void testUploadConsentFileNewConsentDocumentSendsToLms() throws Exception {
        MockMultipartFile multipart = new MockMultipartFile("file", "consent.pdf", "application/pdf", "consent bytes".getBytes(StandardCharsets.UTF_8));
        when(experiment.getConsentDocument()).thenReturn(null);
        when(apiClient.uploadConsentFile(eq(experiment), any(ConsentDocument.class), eq(ltiUserEntity))).thenReturn(lmsAssignment);
        when(consentDocumentRepository.save(any(ConsentDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileInfoDto result = fileStorageService.uploadConsentFile(1L, "My Consent", multipart, securedInfo);

        assertNotNull(result);
        assertNull(result.getFileSubmissionLocal());
        verify(apiClient).uploadConsentFile(eq(experiment), any(ConsentDocument.class), eq(ltiUserEntity));
        verify(experimentRepository).saveAndFlush(experiment);
    }

    @Test
    public void testUploadConsentFileExistingLmsAssignmentEditsAssignment() throws Exception {
        MockMultipartFile multipart = new MockMultipartFile("file", "consent2.pdf", "application/pdf", "bytes".getBytes(StandardCharsets.UTF_8));
        when(consentDocument.getLmsAssignmentId()).thenReturn("55");
        when(apiClient.listAssignment(eq(ltiUserEntity), anyString(), eq("55"))).thenReturn(Optional.of(lmsAssignment));
        when(consentDocumentRepository.save(any(ConsentDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fileStorageService.uploadConsentFile(1L, "New Title", multipart, securedInfo);

        verify(lmsAssignment).setName("New Title");
        verify(apiClient).editAssignment(eq(ltiUserEntity), eq(lmsAssignment), anyString());
        verify(consentDocument).setTitle("New Title");
    }

    @Test
    public void testUploadConsentFileExistingLmsAssignmentNotFoundThrows() throws Exception {
        MockMultipartFile multipart = new MockMultipartFile("file", "consent3.pdf", "application/pdf", "bytes".getBytes(StandardCharsets.UTF_8));
        when(consentDocument.getLmsAssignmentId()).thenReturn("55");
        when(apiClient.listAssignment(any(LtiUserEntity.class), anyString(), anyString())).thenReturn(Optional.empty());

        assertThrows(AssignmentNotEditedException.class, () -> fileStorageService.uploadConsentFile(1L, "T", multipart, securedInfo));
    }

    @Test
    public void testSendConsentFileToLmsSuccess() throws Exception {
        ConsentDocument doc = new ConsentDocument();
        when(apiClient.uploadConsentFile(eq(experiment), eq(doc), eq(ltiUserEntity))).thenReturn(lmsAssignment);

        fileStorageService.sendConsentFileToLms(doc, experiment, ltiUserEntity);

        assertEquals(lmsAssignment.getId(), doc.getLmsAssignmentId());
        assertNotNull(doc.getResourceLinkId());
    }

    @Test
    public void testSendConsentFileToLmsApiExceptionThrowsAssignmentNotCreated() throws Exception {
        ConsentDocument doc = new ConsentDocument();
        when(apiClient.uploadConsentFile(any(), any(), any())).thenThrow(new ApiException("boom"));

        assertThrows(AssignmentNotCreatedException.class, () -> fileStorageService.sendConsentFileToLms(doc, experiment, ltiUserEntity));
    }

    @Test
    public void testDeleteConsentFileNoConsentDocumentReturnsSilently() {
        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> fileStorageService.deleteConsentFile(1L));
    }

    @Test
    public void testDeleteConsentFileSuccessDeletesFile() throws IOException {
        Path file = consentRoot.resolve("to-delete.txt");
        Files.writeString(file, "bye");
        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.of(consentDocument));
        when(consentDocument.getFileUri()).thenReturn("to-delete.txt");

        fileStorageService.deleteConsentFile(1L);

        assertFalse(Files.exists(file));
    }

    @Test
    public void testDeleteConsentFileIoExceptionThrows() throws IOException {
        Path dir = Files.createDirectories(consentRoot.resolve("nonempty"));
        Files.writeString(dir.resolve("inner.txt"), "x");
        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.of(consentDocument));
        when(consentDocument.getFileUri()).thenReturn("nonempty");

        assertThrows(FileStorageException.class, () -> fileStorageService.deleteConsentFile(1L));
    }

    @Test
    public void testDeleteConsentAssignmentNoConsentDocumentReturnsSilently() {
        when(experiment.getConsentDocument()).thenReturn(null);

        assertDoesNotThrow(() -> fileStorageService.deleteConsentAssignment(1L, securedInfo));
    }

    @Test
    public void testDeleteConsentAssignmentSuccess() throws Exception {
        when(consentDocument.getFileUri()).thenReturn("some/file.txt");
        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.of(consentDocument));
        when(apiClient.listAssignment(eq(ltiUserEntity), anyString(), anyString())).thenReturn(Optional.of(lmsAssignment));

        fileStorageService.deleteConsentAssignment(1L, securedInfo);

        verify(lmsAssignment).setMetadata(consentDocument.getMetadata());
        verify(apiClient).deleteAssignmentInLms(eq(lmsAssignment), anyString(), eq(ltiUserEntity));
        verify(experiment).setConsentDocument(null);
        verify(consentDocumentRepository).deleteById(anyLong());
    }

    @Test
    public void testDeleteConsentAssignmentLmsAssignmentNotPresentSkipsDelete() throws Exception {
        when(consentDocument.getFileUri()).thenReturn("f.txt");
        when(consentDocumentRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Optional.of(consentDocument));
        when(apiClient.listAssignment(any(LtiUserEntity.class), anyString(), anyString())).thenReturn(Optional.empty());

        fileStorageService.deleteConsentAssignment(1L, securedInfo);

        verify(apiClient, never()).deleteAssignmentInLms(any(edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment.class), anyString(), any());
        verify(experiment).setConsentDocument(null);
    }

    @Test
    public void testParseHTMLFilesBlankHtmlReturnsAsIs() {
        assertNull(fileStorageService.parseHTMLFiles(null, "http://localhost"));
        assertEquals("", fileStorageService.parseHTMLFiles("", "http://localhost"));
    }

    @Test
    public void testParseHTMLFilesRewritesSrcAndHrefTokens() throws GeneralSecurityException {
        String localUrl = "http://localhost";
        String html = "<div><img src=\"" + localUrl + "/files/42?token=old\"/>"
            + "<a href=\"" + localUrl + "/api/experiments/7/files/99\">link</a></div>";
        when(apijwtService.buildFileToken(anyString(), anyString())).thenReturn("newtoken");

        String result = fileStorageService.parseHTMLFiles(html, localUrl);

        assertTrue(result.contains("/files/42?token=newtoken"));
        assertTrue(result.contains("/files/99?token=newtoken"));
    }

    @Test
    public void testParseHTMLFilesTokenBuildFailureLeavesElementUnchanged() throws GeneralSecurityException {
        String localUrl = "http://localhost";
        String html = "<img src=\"" + localUrl + "/files/5?token=x\"/>";
        when(apijwtService.buildFileToken(anyString(), anyString())).thenThrow(new GeneralSecurityException("fail"));

        String result = fileStorageService.parseHTMLFiles(html, localUrl);

        assertTrue(result.contains(localUrl + "/files/5?token=x"));
    }

    @Test
    public void testSaveFileSubmissionLocalNullFileThrows() {
        assertThrows(FileStorageException.class, () -> fileStorageService.saveFileSubmissionLocal(null));
    }

    @Test
    public void testSaveFileSubmissionLocalInvalidFilenameThrows() {
        MockMultipartFile multipart = new MockMultipartFile("file", "../evil.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        assertThrows(FileStorageException.class, () -> fileStorageService.saveFileSubmissionLocal(multipart));
    }

    @Test
    public void testSaveFileSubmissionLocalSuccess() {
        MockMultipartFile multipart = new MockMultipartFile("file", "answer.txt", "text/plain", "answer content".getBytes(StandardCharsets.UTF_8));

        FileSubmissionLocal result = fileStorageService.saveFileSubmissionLocal(multipart);

        assertTrue(result.compressed());
        assertTrue(Files.exists(submissionsRoot.resolve(result.filePath() + ".zip")));
    }

    @Test
    public void testSaveFileSubmissionLocalIoExceptionThrows() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("bad.txt");
        when(multipartFile.getInputStream()).thenThrow(new IOException("fail"));

        assertThrows(FileStorageException.class, () -> fileStorageService.saveFileSubmissionLocal(multipartFile));
    }

    @Test
    public void testGetFileSubmissionLocalNotFoundReturnsNull() {
        when(answerFileSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertNull(fileStorageService.getFileSubmissionLocal(1L));
    }

    @Test
    public void testGetFileSubmissionLocalNotCompressedReturnsFile() throws IOException {
        Path f = submissionsRoot.resolve("plain-answer.txt");
        Files.writeString(f, "plain answer");
        AnswerFileSubmission answer = AnswerFileSubmission.builder().fileUri("plain-answer.txt").build();
        when(answerFileSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(answer));

        File result = fileStorageService.getFileSubmissionLocal(1L);

        assertEquals(f.toFile().getCanonicalPath(), result.getCanonicalPath());
    }

    @Test
    public void testGetFileSubmissionLocalCompressedDecompressesAndReturnsFile() throws IOException {
        Path dir = Files.createDirectories(submissionsRoot.resolve("sub"));
        Path plain = dir.resolve("secret-answer.txt");
        Files.writeString(plain, "secret answer content");
        String phrase = "answerPhrase";
        assertTrue(fileStorageService.compressFile(plain.toString(), phrase, ".zip"));
        Files.delete(plain);

        AnswerFileSubmission answer = AnswerFileSubmission.builder()
            .fileUri("sub/secret-answer.txt")
            .encryptionMethod("AES")
            .encryptionPhrase(phrase)
            .build();
        when(answerFileSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(answer));

        File result = fileStorageService.getFileSubmissionLocal(1L);

        assertTrue(result.exists());
        assertEquals("secret answer content", Files.readString(result.toPath()));
    }

    @Test
    public void testDeleteFileSubmissionByIdNotFoundReturnsSilently() {
        when(answerFileSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> fileStorageService.deleteFileSubmission(1L));
    }

    @Test
    public void testDeleteFileSubmissionByIdDelegatesAndDeletesFile() throws IOException {
        Path file = submissionsRoot.resolve("submission-to-delete.txt");
        Files.writeString(file, "bye");
        AnswerFileSubmission answer = AnswerFileSubmission.builder().answerFileSubmissionId(1L).fileUri("submission-to-delete.txt").build();
        when(answerFileSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(answer));

        fileStorageService.deleteFileSubmission(1L);

        assertFalse(Files.exists(file));
    }

    @Test
    public void testDeleteFileSubmissionEntityIoExceptionThrows() throws IOException {
        Path dir = Files.createDirectories(submissionsRoot.resolve("nonempty"));
        Files.writeString(dir.resolve("inner.txt"), "x");
        AnswerFileSubmission answer = AnswerFileSubmission.builder().answerFileSubmissionId(1L).fileUri("nonempty").build();

        assertThrows(FileStorageException.class, () -> fileStorageService.deleteFileSubmission(answer));
    }

    @Test
    public void testSaveAssignmentFileArchiveNullFileThrows() {
        AssignmentFileArchive archive = AssignmentFileArchive.builder().fileName("archive.zip").build();

        assertThrows(FileStorageException.class, () -> fileStorageService.saveAssignmentFileArchive(archive, null));
    }

    @Test
    public void testSaveAssignmentFileArchiveSuccess() throws IOException {
        Path source = Files.createTempFile(tempDir, "src-archive", ".bin");
        Files.writeString(source, "archive payload");
        AssignmentFileArchive archive = AssignmentFileArchive.builder().fileName("archive.zip").build();

        fileStorageService.saveAssignmentFileArchive(archive, source.toFile());

        assertEquals("AES", archive.getEncryptionMethod());
        assertNotNull(archive.getEncryptionPhrase());
        assertEquals(AssignmentFileArchive.MIME_TYPE, archive.getMimeType());
        assertNotNull(archive.getFileUri());
        assertTrue(Files.exists(archiveRoot.resolve(archive.getFileUri() + ".zip")));
    }

    @Test
    public void testGetAssignmentFileArchiveNotFoundReturnsNull() {
        when(assignmentFileArchiveRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertNull(fileStorageService.getAssignmentFileArchive(1L));
    }

    @Test
    public void testGetAssignmentFileArchiveSuccessDecompressesAndReturnsFile() throws IOException {
        Path source = Files.createTempFile(tempDir, "src-archive2", ".bin");
        Files.writeString(source, "archive round trip payload");
        AssignmentFileArchive archive = AssignmentFileArchive.builder().fileName("archive2.zip").build();
        fileStorageService.saveAssignmentFileArchive(archive, source.toFile());
        when(assignmentFileArchiveRepository.findById(anyLong())).thenReturn(Optional.of(archive));

        File result = fileStorageService.getAssignmentFileArchive(1L);

        assertTrue(result.exists());
        assertEquals("archive round trip payload", Files.readString(result.toPath()));
    }

    @Test
    public void testSaveExperimentDataExportNullFileThrows() {
        ExperimentDataExport export = ExperimentDataExport.builder().fileName("export.zip").build();

        assertThrows(FileStorageException.class, () -> fileStorageService.saveExperimentDataExport(export, null));
    }

    @Test
    public void testSaveExperimentDataExportSuccess() throws IOException {
        Path source = Files.createTempFile(tempDir, "src-export", ".bin");
        Files.writeString(source, "export payload");
        ExperimentDataExport export = ExperimentDataExport.builder().fileName("export.zip").build();

        fileStorageService.saveExperimentDataExport(export, source.toFile());

        assertEquals("AES", export.getEncryptionMethod());
        assertNotNull(export.getEncryptionPhrase());
        assertNotNull(export.getFileUri());
        assertTrue(Files.exists(dataExportRoot.resolve(export.getFileUri() + ".zip")));
    }

    @Test
    public void testGetExperimentDataExportNotFoundReturnsNull() {
        when(exportDataRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertNull(fileStorageService.getExperimentDataExport(1L));
    }

    @Test
    public void testGetExperimentDataExportSuccessDecompressesAndReturnsFile() throws IOException {
        Path source = Files.createTempFile(tempDir, "src-export2", ".bin");
        Files.writeString(source, "export round trip payload");
        ExperimentDataExport export = ExperimentDataExport.builder().fileName("export2.zip").build();
        fileStorageService.saveExperimentDataExport(export, source.toFile());
        when(exportDataRepository.findById(anyLong())).thenReturn(Optional.of(export));

        File result = fileStorageService.getExperimentDataExport(1L);

        assertTrue(result.exists());
        assertEquals("export round trip payload", Files.readString(result.toPath()));
    }

    @Test
    public void testCreateExperimentExportFileConsentTypeIncludesConsentDocument() throws IOException {
        Path consentDir = Files.createDirectories(consentRoot.resolve("export-consent"));
        Files.writeString(consentDir.resolve("consent-source.pdf"), "consent pdf bytes");
        when(consentDocumentRepository.findByExperiment_ExperimentId(7L)).thenReturn(Optional.of(consentDocument));
        when(consentDocument.isCompressed()).thenReturn(false);
        when(consentDocument.getFileUri()).thenReturn("export-consent/consent-source.pdf");

        ExperimentExport experimentExport = ExperimentExport.builder().id(7L).participationType(ParticipationTypes.CONSENT).build();
        Export export = Export.builder().experiment(experimentExport).build();
        ExportDto exportDto = ExportDto.builder().build();

        fileStorageService.createExperimentExportFile(exportDto, export, "myexport.zip");

        assertNotNull(exportDto.getFile());
        assertTrue(exportDto.getFile().exists());
    }

    @Test
    public void testCreateExperimentExportFileNonConsentTypeSkipsConsentDocument() throws IOException {
        ExperimentExport experimentExport = ExperimentExport.builder().id(9L).participationType(ParticipationTypes.AUTO).build();
        Export export = Export.builder().experiment(experimentExport).build();
        ExportDto exportDto = ExportDto.builder().build();

        fileStorageService.createExperimentExportFile(exportDto, export, "myexport2.zip");

        assertNotNull(exportDto.getFile());
        assertTrue(exportDto.getFile().exists());
        verify(consentDocumentRepository, never()).findByExperiment_ExperimentId(anyLong());
    }

    @Test
    public void testSaveExperimentImportFileSuccess() throws IOException {
        MockMultipartFile multipart = new MockMultipartFile("file", "import.zip", "application/zip", "zip bytes".getBytes(StandardCharsets.UTF_8));
        ExperimentImport experimentImportEntity = new ExperimentImport();

        fileStorageService.saveExperimentImportFile(multipart, experimentImportEntity);

        assertNotNull(experimentImportEntity.getFileUri());
        assertTrue(Files.exists(experimentExportRoot.resolve(experimentImportEntity.getFileUri())));
    }

    @Test
    public void testGetExperimentImportFileNotFoundReturnsNull() {
        when(experimentImportRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertNull(fileStorageService.getExperimentImportFile(1L));
    }

    @Test
    public void testGetExperimentImportFileSuccessDecompressesAndReturnsDirectory() throws IOException {
        Path folder = Files.createDirectories(experimentExportRoot.resolve("sub/myfolder"));
        Files.writeString(folder.resolve("data.txt"), "import content");
        assertTrue(fileStorageService.compressDirectory(experimentExportRoot.resolve("sub/myfolder").toString(), "", ".zip", false));

        ExperimentImport experimentImportEntity = new ExperimentImport();
        experimentImportEntity.setFileUri("sub/myfolder.zip");
        experimentImportEntity.setFileName("myfolder.zip");
        when(experimentImportRepository.findById(anyLong())).thenReturn(Optional.of(experimentImportEntity));

        File result = fileStorageService.getExperimentImportFile(1L);

        assertTrue(result.exists());
        assertTrue(result.isDirectory());
        assertEquals("import content", Files.readString(result.toPath().resolve("data.txt")));
    }

    // compressDirectory (zip4j addFolder) nests everything inside a folder named after the
    // ORIGINAL export's directory - a re-uploaded file's own name (e.g. a browser-disambiguated
    // "export (1).zip" on a duplicate download, or a manual rename) commonly differs from that,
    // so getExperimentImportFile must locate experiment.json rather than assume the two match.
    @Test
    public void testGetExperimentImportFileFindsJsonFileWhenReuploadedNameDiffersFromZipInternalFolder() throws IOException {
        Path folder = Files.createDirectories(experimentExportRoot.resolve("sub/originalExportName"));
        Files.writeString(folder.resolve(ExperimentImport.JSON_FILE_NAME), "{}");
        assertTrue(fileStorageService.compressDirectory(experimentExportRoot.resolve("sub/originalExportName").toString(), "", ".zip", false));
        Files.move(experimentExportRoot.resolve("sub/originalExportName.zip"), experimentExportRoot.resolve("sub/renamedOnReupload.zip"));

        ExperimentImport experimentImportEntity = new ExperimentImport();
        experimentImportEntity.setFileUri("sub/renamedOnReupload.zip");
        experimentImportEntity.setFileName("renamedOnReupload.zip");
        when(experimentImportRepository.findById(anyLong())).thenReturn(Optional.of(experimentImportEntity));

        File result = fileStorageService.getExperimentImportFile(1L);

        assertTrue(FileUtils.getFile(result, ExperimentImport.JSON_FILE_NAME).isFile());
    }

    @Test
    public void testCompressFileSuccessCreatesEncryptedZip() throws IOException {
        Path file = tempDir.resolve("plain.txt");
        Files.writeString(file, "data");

        boolean result = fileStorageService.compressFile(file.toString(), "phrase", ".zip");

        assertTrue(result);
        assertTrue(Files.exists(tempDir.resolve("plain.txt.zip")));
    }

    @Test
    public void testCompressFileMissingSourceReturnsFalse() {
        boolean result = fileStorageService.compressFile(tempDir.resolve("missing.txt").toString(), "phrase", ".zip");

        assertFalse(result);
    }

    @Test
    public void testCompressFileWithEncryptFalseCreatesZip() throws IOException {
        Path file = tempDir.resolve("plain2.txt");
        Files.writeString(file, "data2");

        boolean result = fileStorageService.compressFile(file.toString(), "", ".zip", false);

        assertTrue(result);
        assertTrue(Files.exists(tempDir.resolve("plain2.txt.zip")));
    }

    @Test
    public void testCompressDirectorySuccessCreatesZip() throws IOException {
        Path dir = Files.createDirectories(tempDir.resolve("dirToZip"));
        Files.writeString(dir.resolve("inner.txt"), "inner content");

        boolean result = fileStorageService.compressDirectory(dir.toString(), "dirPhrase", ".zip");

        assertTrue(result);
        assertTrue(Files.exists(tempDir.resolve("dirToZip.zip")));
    }

    @Test
    public void testCompressDirectoryMissingSourceReturnsFalse() {
        boolean result = fileStorageService.compressDirectory(tempDir.resolve("missingDir").toString(), "phrase", ".zip");

        assertFalse(result);
    }

    @Test
    public void testCompressDirectoryWithEncryptFalseCreatesZip() throws IOException {
        Path dir = Files.createDirectories(tempDir.resolve("dirToZip2"));
        Files.writeString(dir.resolve("inner2.txt"), "inner content 2");

        boolean result = fileStorageService.compressDirectory(dir.toString(), "", ".zip", false);

        assertTrue(result);
        assertTrue(Files.exists(tempDir.resolve("dirToZip2.zip")));
    }

}
