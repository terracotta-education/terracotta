package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.AnswerEssaySubmission;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
import edu.iu.terracotta.dao.entity.AnswerMcSubmission;
import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
import edu.iu.terracotta.dao.entity.integrations.AnswerIntegrationSubmission;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerSubmissionDto;
import edu.iu.terracotta.dao.model.dto.FileResponseDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.utils.TextConstants;

public class AnswerSubmissionServiceImplTest extends BaseTest {

    private static final String TEMP_FILE_PATH = System.getProperty("java.io.tmpdir") + File.separator + "answer-submission-service-impl-test-file.txt";

    @InjectMocks private AnswerSubmissionServiceImpl answerSubmissionService;

    @Mock private AnswerFileSubmission answerFileSubmission;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(questionSubmission));
        when(questionSubmissionRepository.findByQuestionSubmissionId(anyLong())).thenReturn(questionSubmission);
        when(answerMcRepository.findById(anyLong())).thenReturn(Optional.of(answerMc));
        when(answerMcSubmission.getQuestionSubmission()).thenReturn(questionSubmission);
        when(answerFileSubmissionRepository.findByAnswerFileSubmissionId(anyLong())).thenReturn(answerFileSubmission);
        when(answerFileSubmission.getQuestionSubmission()).thenReturn(questionSubmission);
        when(answerFileSubmission.getAnswerFileSubmissionId()).thenReturn(1L);
    }

    @AfterEach
    public void afterEach() {
        new File(TEMP_FILE_PATH).delete();
    }

    @Test
    public void testGetAnswerSubmissionsMC() throws DataServiceException, IOException {
        List<AnswerSubmissionDto> result = answerSubmissionService.getAnswerSubmissions(1L, "MC");

        assertEquals(1, result.size());
    }

    @Test
    public void testGetAnswerSubmissionsEssay() throws DataServiceException, IOException {
        List<AnswerSubmissionDto> result = answerSubmissionService.getAnswerSubmissions(1L, "ESSAY");

        assertEquals(1, result.size());
    }

    @Test
    public void testGetAnswerSubmissionsFile() throws DataServiceException, IOException {
        when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(List.of(answerFileSubmission));

        List<AnswerSubmissionDto> result = answerSubmissionService.getAnswerSubmissions(1L, "FILE");

        assertEquals(1, result.size());
    }

    @Test
    public void testGetAnswerSubmissionsInvalidTypeThrows() {
        assertThrows(DataServiceException.class, () -> answerSubmissionService.getAnswerSubmissions(1L, "BOGUS"));
    }

    @Test
    public void testGetAnswerSubmissionMC() throws DataServiceException, IOException {
        when(answerMcSubmissionRepository.findByAnswerMcSubId(anyLong())).thenReturn(answerMcSubmission);
        when(answerMcSubmission.getAnswerMcSubId()).thenReturn(9L);

        AnswerSubmissionDto result = answerSubmissionService.getAnswerSubmission(1L, "MC");

        assertEquals(9L, result.getAnswerSubmissionId());
        assertEquals(1L, result.getAnswerId());
    }

    @Test
    public void testGetAnswerSubmissionEssay() throws DataServiceException, IOException {
        when(answerEssaySubmissionRepository.findByAnswerEssaySubmissionId(anyLong())).thenReturn(answerEssaySubmission);
        when(answerEssaySubmission.getAnswerEssaySubmissionId()).thenReturn(9L);
        when(answerEssaySubmission.getResponse()).thenReturn("resp");

        AnswerSubmissionDto result = answerSubmissionService.getAnswerSubmission(1L, "ESSAY");

        assertEquals(9L, result.getAnswerSubmissionId());
        assertEquals("resp", result.getResponse());
    }

    @Test
    public void testGetAnswerSubmissionFile() throws DataServiceException, IOException {
        when(answerFileSubmission.getAnswerFileSubmissionId()).thenReturn(9L);

        AnswerSubmissionDto result = answerSubmissionService.getAnswerSubmission(1L, "FILE");

        assertEquals(9L, result.getAnswerSubmissionId());
    }

    @Test
    public void testGetAnswerSubmissionInvalidTypeThrows() {
        assertThrows(DataServiceException.class, () -> answerSubmissionService.getAnswerSubmission(1L, "BOGUS"));
    }

    @Test
    public void testPostAnswerSubmissionIdInPostExceptionThrows() {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(1L).build();

        Exception exception = assertThrows(IdInPostException.class, () -> answerSubmissionService.postAnswerSubmission(dto, 1L));

        assertEquals(TextConstants.ID_IN_POST_ERROR, exception.getMessage());
    }

    @Test
    public void testPostAnswerSubmissionMCSuccess() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        AnswerMcSubmission saved = AnswerMcSubmission.builder().answerMcSubId(2L).questionSubmission(questionSubmission).answerMc(answerMc).build();
        when(answerMcSubmissionRepository.save(any(AnswerMcSubmission.class))).thenReturn(saved);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerId(1L).build();

        AnswerSubmissionDto result = answerSubmissionService.postAnswerSubmission(dto, 1L);

        assertEquals(2L, result.getAnswerSubmissionId());
        assertEquals(1L, result.getAnswerId());
        verify(answerMcSubmissionRepository).save(any(AnswerMcSubmission.class));
    }

    @Test
    public void testPostAnswerSubmissionMCFromDtoThrowsWrapsException() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(answerMcRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerId(1L).build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.postAnswerSubmission(dto, 1L));

        assertEquals("Error 105: Unable to create answer submission: The MC answer for the answer submission does not exist.", exception.getMessage());
    }

    @Test
    public void testPostAnswerSubmissionEssaySuccess() throws Exception {
        AnswerEssaySubmission saved = AnswerEssaySubmission.builder().answerEssaySubmissionId(2L).questionSubmission(questionSubmission).response("resp").build();
        when(answerEssaySubmissionRepository.save(any(AnswerEssaySubmission.class))).thenReturn(saved);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().response("resp").build();

        AnswerSubmissionDto result = answerSubmissionService.postAnswerSubmission(dto, 1L);

        assertEquals(2L, result.getAnswerSubmissionId());
        assertEquals("resp", result.getResponse());
    }

    @Test
    public void testPostAnswerSubmissionEssayThrows() {
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().response("resp").build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.postAnswerSubmission(dto, 1L));

        assertEquals("Error 105: Unable to create answer submission: Question submission for answer submission does not exist.", exception.getMessage());
    }

    @Test
    public void testPostAnswerSubmissionFileSuccess() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        AnswerFileSubmission saved = AnswerFileSubmission.builder().answerFileSubmissionId(3L).questionSubmission(questionSubmission).fileName("f.txt").mimeType("text/plain").build();
        when(answerFileSubmissionRepository.save(any(AnswerFileSubmission.class))).thenReturn(saved);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().fileContent("aGVsbG8=").fileName("f.txt").mimeType("text/plain").build();

        AnswerSubmissionDto result = answerSubmissionService.postAnswerSubmission(dto, 1L);

        assertEquals(3L, result.getAnswerSubmissionId());
        assertEquals("f.txt", result.getFileName());
    }

    @Test
    public void testPostAnswerSubmissionFileThrows() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.postAnswerSubmission(dto, 1L));

        assertEquals("Error 105: Unable to create file submission: ", exception.getMessage());
    }

    @Test
    public void testPostAnswerSubmissionIntegrationSuccess() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().build();

        AnswerSubmissionDto result = answerSubmissionService.postAnswerSubmission(dto, 1L);

        assertNull(result.getAnswerSubmissionId());
        assertEquals(1L, result.getQuestionSubmissionId());
    }

    @Test
    public void testPostAnswerSubmissionIntegrationThrows() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION);
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.postAnswerSubmission(dto, 1L));

        assertEquals("Error 105: Unable to create file submission: ", exception.getMessage());
    }

    @Test
    public void testPostAnswerSubmissionUnsupportedTypeThrows() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.PAGE_BREAK);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().build();

        assertThrows(TypeNotSupportedException.class, () -> answerSubmissionService.postAnswerSubmission(dto, 1L));
    }

    @Test
    public void testPostAnswerSubmissionsIdMissingThrows() {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().build();

        Exception exception = assertThrows(IdMissingException.class, () -> answerSubmissionService.postAnswerSubmissions(List.of(dto)));

        assertEquals(TextConstants.ID_MISSING, exception.getMessage());
    }

    @Test
    public void testPostAnswerSubmissionsExceedingLimitThrows() {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).build();

        Exception exception = assertThrows(ExceedingLimitException.class, () -> answerSubmissionService.postAnswerSubmissions(List.of(dto)));

        assertEquals("Error 145: Multiple choice and essay questions can only have one answer submission.", exception.getMessage());
    }

    @Test
    public void testPostAnswerSubmissionsSuccess() throws Exception {
        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());
        AnswerEssaySubmission saved = AnswerEssaySubmission.builder().answerEssaySubmissionId(2L).questionSubmission(questionSubmission).response("hi").build();
        when(answerEssaySubmissionRepository.save(any(AnswerEssaySubmission.class))).thenReturn(saved);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).response("hi").build();

        List<AnswerSubmissionDto> result = answerSubmissionService.postAnswerSubmissions(List.of(dto));

        assertEquals(1, result.size());
        assertEquals("hi", result.get(0).getResponse());
    }

    @Test
    public void testPostAnswerSubmissionsFileTypeSuccess() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());
        AnswerFileSubmission saved = AnswerFileSubmission.builder().answerFileSubmissionId(1L).questionSubmission(questionSubmission).build();
        when(answerFileSubmissionRepository.save(any(AnswerFileSubmission.class))).thenReturn(saved);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).fileContent("aGVsbG8=").build();

        List<AnswerSubmissionDto> result = answerSubmissionService.postAnswerSubmissions(List.of(dto));

        assertEquals(1, result.size());
    }

    @Test
    public void testUpdateAnswerSubmissionMC() throws AnswerNotMatchingException, DataServiceException {
        when(answerMcSubmissionRepository.findByAnswerMcSubId(anyLong())).thenReturn(answerMcSubmission);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerId(1L).build();

        answerSubmissionService.updateAnswerSubmission(dto, 1L, "MC");

        verify(answerMcSubmissionRepository).saveAndFlush(answerMcSubmission);
    }

    @Test
    public void testUpdateAnswerSubmissionEssay() throws AnswerNotMatchingException, DataServiceException {
        when(answerEssaySubmissionRepository.findByAnswerEssaySubmissionId(anyLong())).thenReturn(answerEssaySubmission);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().response("resp").build();

        answerSubmissionService.updateAnswerSubmission(dto, 1L, "ESSAY");

        verify(answerEssaySubmissionRepository).saveAndFlush(answerEssaySubmission);
    }

    @Test
    public void testUpdateAnswerSubmissionFile() throws AnswerNotMatchingException, DataServiceException {
        when(answerFileSubmissionRepository.findByAnswerFileSubmissionId(anyLong())).thenReturn(answerFileSubmission);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().fileContent("aGVsbG8=").build();

        answerSubmissionService.updateAnswerSubmission(dto, 1L, "FILE");

        verify(answerFileSubmissionRepository).saveAndFlush(answerFileSubmission);
    }

    @Test
    public void testUpdateAnswerSubmissionIntegrationThrows() {
        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.updateAnswerSubmission(answerSubmissionDto, 1L, "INTEGRATION"));

        assertEquals("Error 103: Answer type not supported.", exception.getMessage());
    }

    @Test
    public void testDeleteAnswerSubmissionMC() throws DataServiceException {
        answerSubmissionService.deleteAnswerSubmission(1L, "MC");

        verify(answerMcSubmissionRepository).deleteByAnswerMcSubId(1L);
    }

    @Test
    public void testDeleteAnswerSubmissionEssay() throws DataServiceException {
        answerSubmissionService.deleteAnswerSubmission(1L, "ESSAY");

        verify(answerEssaySubmissionRepository).deleteByAnswerEssaySubmissionId(1L);
    }

    @Test
    public void testDeleteAnswerSubmissionFile() throws DataServiceException {
        answerSubmissionService.deleteAnswerSubmission(1L, "FILE");

        verify(answerFileSubmissionRepository).deleteByAnswerFileSubmissionId(1L);
    }

    @Test
    public void testDeleteAnswerSubmissionIntegration() throws DataServiceException {
        answerSubmissionService.deleteAnswerSubmission(1L, "INTEGRATION");

        verify(answerIntegrationSubmissionRepository).deleteById(1L);
    }

    @Test
    public void testDeleteAnswerSubmissionInvalidTypeThrows() {
        assertThrows(DataServiceException.class, () -> answerSubmissionService.deleteAnswerSubmission(1L, "BOGUS"));
    }

    @Test
    public void testGetAnswerMcSubmissionsEmpty() {
        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());

        List<AnswerSubmissionDto> result = answerSubmissionService.getAnswerMcSubmissions(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testToDtoMCWithAnswer() {
        AnswerMcSubmission entity = AnswerMcSubmission.builder().answerMcSubId(4L).questionSubmission(questionSubmission).answerMc(answerMc).build();

        AnswerSubmissionDto dto = answerSubmissionService.toDtoMC(entity);

        assertEquals(4L, dto.getAnswerSubmissionId());
        assertEquals(1L, dto.getAnswerId());
        assertEquals(1L, dto.getQuestionSubmissionId());
    }

    @Test
    public void testToDtoMCWithoutAnswer() {
        AnswerMcSubmission entity = AnswerMcSubmission.builder().answerMcSubId(4L).questionSubmission(questionSubmission).build();

        AnswerSubmissionDto dto = answerSubmissionService.toDtoMC(entity);

        assertNull(dto.getAnswerId());
    }

    @Test
    public void testFromDtoMCSuccess() throws DataServiceException {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(5L).answerId(1L).questionSubmissionId(1L).build();

        AnswerMcSubmission result = answerSubmissionService.fromDtoMC(dto);

        assertEquals(5L, result.getAnswerMcSubId());
        assertEquals(answerMc, result.getAnswerMc());
        assertEquals(questionSubmission, result.getQuestionSubmission());
    }

    @Test
    public void testFromDtoMCAnswerIdNullSkipsLookup() throws DataServiceException {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).build();

        AnswerMcSubmission result = answerSubmissionService.fromDtoMC(dto);

        assertNull(result.getAnswerMc());
        verify(answerMcRepository, never()).findById(any());
    }

    @Test
    public void testFromDtoMCAnswerNotFoundThrows() {
        when(answerMcRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerId(1L).questionSubmissionId(1L).build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.fromDtoMC(dto));

        assertEquals("The MC answer for the answer submission does not exist.", exception.getMessage());
    }

    @Test
    public void testFromDtoMCQuestionSubmissionNotFoundThrows() {
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.fromDtoMC(dto));

        assertEquals("The question submission for the answer submission does not exist.", exception.getMessage());
    }

    @Test
    public void testUpdateAnswerMcSubmissionSuccess() throws AnswerNotMatchingException {
        when(answerMcSubmissionRepository.findByAnswerMcSubId(anyLong())).thenReturn(answerMcSubmission);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerId(1L).build();

        answerSubmissionService.updateAnswerMcSubmission(1L, dto);

        verify(answerMcSubmission).setAnswerMc(answerMc);
        verify(answerMcSubmissionRepository).saveAndFlush(answerMcSubmission);
    }

    @Test
    public void testUpdateAnswerMcSubmissionNotFoundThrows() {
        when(answerMcSubmissionRepository.findByAnswerMcSubId(anyLong())).thenReturn(answerMcSubmission);
        when(answerMcRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(AnswerNotMatchingException.class, () -> answerSubmissionService.updateAnswerMcSubmission(1L, answerSubmissionDto));

        assertEquals(TextConstants.ANSWER_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testGetAnswerEssaySubmissionsEmpty() {
        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());

        List<AnswerSubmissionDto> result = answerSubmissionService.getAnswerEssaySubmissions(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testToDtoEssay() {
        AnswerEssaySubmission entity = AnswerEssaySubmission.builder().answerEssaySubmissionId(4L).questionSubmission(questionSubmission).response("resp").build();

        AnswerSubmissionDto dto = answerSubmissionService.toDtoEssay(entity);

        assertEquals(4L, dto.getAnswerSubmissionId());
        assertEquals("resp", dto.getResponse());
    }

    @Test
    public void testFromDtoEssaySuccess() throws DataServiceException {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(5L).response("resp").questionSubmissionId(1L).build();

        AnswerEssaySubmission result = answerSubmissionService.fromDtoEssay(dto);

        assertEquals(5L, result.getAnswerEssaySubmissionId());
        assertEquals("resp", result.getResponse());
        assertEquals(questionSubmission, result.getQuestionSubmission());
    }

    @Test
    public void testFromDtoEssayQuestionSubmissionNotFoundThrows() {
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.fromDtoEssay(dto));

        assertEquals("Question submission for answer submission does not exist.", exception.getMessage());
    }

    @Test
    public void testUpdateAnswerEssaySubmission() {
        when(answerEssaySubmissionRepository.findByAnswerEssaySubmissionId(anyLong())).thenReturn(answerEssaySubmission);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().response("new response").build();

        answerSubmissionService.updateAnswerEssaySubmission(1L, dto);

        verify(answerEssaySubmission).setResponse("new response");
        verify(answerEssaySubmissionRepository).saveAndFlush(answerEssaySubmission);
    }

    @Test
    public void testGetAnswerType() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);

        assertEquals("MC", answerSubmissionService.getAnswerType(1L));
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders headers = answerSubmissionService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L, 3L, 4L, 5L, 6L, 7L);

        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/1/conditions/2/treatments/3/assessments/4/submissions/5/question_submissions/6/answer_submissions/7"));
    }

    @Test
    public void testToDtoFile() throws IOException {
        when(answerFileSubmission.getMimeType()).thenReturn("text/plain");
        when(answerFileSubmission.getFileName()).thenReturn("f.txt");

        AnswerSubmissionDto dto = answerSubmissionService.toDtoFile(answerFileSubmission);

        assertEquals(1L, dto.getAnswerSubmissionId());
        assertEquals("text/plain", dto.getMimeType());
        assertEquals("f.txt", dto.getFileName());
        assertNull(dto.getFileContent());
    }

    @Test
    public void testFromDtoFileSuccess() throws DataServiceException {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder()
            .answerSubmissionId(5L)
            .fileContent("aGVsbG8=")
            .fileName("f.txt")
            .mimeType("text/plain")
            .questionSubmissionId(1L)
            .build();

        AnswerFileSubmission result = answerSubmissionService.fromDtoFile(dto);

        assertEquals(5L, result.getAnswerFileSubmissionId());
        assertArrayEquals("aGVsbG8=".getBytes(StandardCharsets.UTF_8), result.getFileContent());
        assertEquals("f.txt", result.getFileName());
        assertEquals(questionSubmission, result.getQuestionSubmission());
    }

    @Test
    public void testFromDtoFileQuestionSubmissionNotFoundThrows() {
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.fromDtoFile(dto));

        assertEquals("Question submission for answer submission does not exist.", exception.getMessage());
    }

    @Test
    public void testGetAnswerFileSubmissions() throws IOException {
        when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(List.of(answerFileSubmission));

        List<AnswerSubmissionDto> result = answerSubmissionService.getAnswerFileSubmissions(1L);

        assertEquals(1, result.size());
    }

    @Test
    public void testGetFileResponseDto() throws IOException {
        when(answerFileSubmission.getFileName()).thenReturn("f.txt");
        when(answerFileSubmission.getMimeType()).thenReturn("text/plain");
        when(fileStorageService.getFileSubmissionLocal(anyLong())).thenReturn(file);

        FileResponseDto result = answerSubmissionService.getFileResponseDto(1L);

        assertEquals("f.txt", result.getFileName());
        assertEquals("text/plain", result.getMimeType());
        assertEquals(file, result.getFile());
    }

    @Test
    public void testUpdateAnswerFileSubmission() {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().fileContent("aGVsbG8=").build();

        answerSubmissionService.updateAnswerFileSubmission(1L, dto);

        verify(answerFileSubmission).setFileContent(any(byte[].class));
        verify(answerFileSubmissionRepository).saveAndFlush(answerFileSubmission);
    }

    private void stubFileUpload(FileSubmissionLocal fileSubmissionLocal) throws IOException {
        Resource resource = mock(Resource.class);
        when(resource.getFilename()).thenReturn("test.txt");
        when(multipartFile.getResource()).thenReturn(resource);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getName()).thenReturn(TEMP_FILE_PATH);
        when(multipartFile.getBytes()).thenReturn("hello".getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.saveFileSubmissionLocal(any(MultipartFile.class))).thenReturn(fileSubmissionLocal);

        AnswerFileSubmission saved = AnswerFileSubmission.builder().answerFileSubmissionId(1L).questionSubmission(questionSubmission).build();
        when(answerFileSubmissionRepository.save(any(AnswerFileSubmission.class))).thenReturn(saved);
    }

    @Test
    public void testHandleFileAnswerSubmissionCompressedSuccess() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        stubFileUpload(FileSubmissionLocal.builder().filePath("/tmp/file/path").compressed(true).encryptionMethod("AES").encryptionPhrase("phrase").build());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).build();

        AnswerSubmissionDto result = answerSubmissionService.handleFileAnswerSubmission(dto, multipartFile);

        assertNotNull(result);
        assertEquals("test.txt", dto.getFileName());
        assertEquals("/tmp/file/path", dto.getFileUri());
        assertEquals("AES", dto.getEncryptionMethod());
        assertEquals("phrase", dto.getEncryptionPhrase());
    }

    @Test
    public void testHandleFileAnswerSubmissionNotCompressed() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        stubFileUpload(FileSubmissionLocal.builder().filePath("/tmp/file/path").compressed(false).build());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).build();

        AnswerSubmissionDto result = answerSubmissionService.handleFileAnswerSubmission(dto, multipartFile);

        assertNotNull(result);
        assertNull(dto.getEncryptionMethod());
        assertNull(dto.getEncryptionPhrase());
    }

    @Test
    public void testHandleFileAnswerSubmissionUpdateNoExistingSubmissions() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());
        stubFileUpload(FileSubmissionLocal.builder().filePath("/tmp/file/path").compressed(false).build());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(5L).questionSubmissionId(1L).build();

        AnswerSubmissionDto result = answerSubmissionService.handleFileAnswerSubmissionUpdate(dto, multipartFile);

        assertNotNull(result);
        verify(answerFileSubmissionRepository, never()).delete(any());
    }

    @Test
    public void testHandleFileAnswerSubmissionUpdateDeletesExisting() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        File existingFile = File.createTempFile("existing-file", ".txt");
        when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(List.of(answerFileSubmission));
        when(fileStorageService.getFileSubmissionLocal(anyLong())).thenReturn(existingFile);
        stubFileUpload(FileSubmissionLocal.builder().filePath("/tmp/file/path").compressed(false).build());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).build();

        AnswerSubmissionDto result = answerSubmissionService.handleFileAnswerSubmissionUpdate(dto, multipartFile);

        assertNotNull(result);
        verify(answerFileSubmissionRepository).delete(answerFileSubmission);
        assertFalse(existingFile.exists());
    }

    @Test
    public void testHandleFileAnswerSubmissionUpdateDeleteThrowsIsCaught() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        Path dir = Files.createTempDirectory("non-empty-dir");
        Path child = dir.resolve("child.txt");
        Files.createFile(child);
        when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(List.of(answerFileSubmission));
        when(fileStorageService.getFileSubmissionLocal(anyLong())).thenReturn(dir.toFile());
        stubFileUpload(FileSubmissionLocal.builder().filePath("/tmp/file/path").compressed(false).build());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).build();

        try {
            AnswerSubmissionDto result = answerSubmissionService.handleFileAnswerSubmissionUpdate(dto, multipartFile);

            assertNotNull(result);
            verify(answerFileSubmissionRepository).delete(answerFileSubmission);
        } finally {
            Files.deleteIfExists(child);
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void testFromDtoIntegrationSuccess() throws DataServiceException {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().questionSubmissionId(1L).build();

        AnswerIntegrationSubmission result = answerSubmissionService.fromDtoIntegration(dto);

        assertEquals(questionSubmission, result.getQuestionSubmission());
    }

    @Test
    public void testFromDtoIntegrationQuestionSubmissionNotFoundThrows() {
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> answerSubmissionService.fromDtoIntegration(dto));

        assertEquals("Question submission for answer submission does not exist.", exception.getMessage());
    }

}
