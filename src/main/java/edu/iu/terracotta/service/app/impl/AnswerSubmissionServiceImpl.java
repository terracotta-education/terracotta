package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerFileSubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AnswerSubmissionServiceImpl implements AnswerSubmissionService {

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private QuestionSubmissionService questionSubmissionService;

    /*
    general methods
     */
    @Override
    public List<AnswerSubmissionDto> getAnswerSubmissions(long questionSubmissionId, String answerType) throws DataServiceException, IOException {
        switch(answerType){
            case "MC":
                return getAnswerMcSubmissions(questionSubmissionId);
            case "ESSAY":
                return getAnswerEssaySubmissions(questionSubmissionId);
            case "FILE":
                return getAnswerFileSubmissions(questionSubmissionId);
            default: throw new DataServiceException("Error 103: Answer type not supported.");
        }
    }

    @Override
    public AnswerSubmissionDto getAnswerSubmission(long answerSubmissionId, String answerType) throws DataServiceException, IOException {
        switch(answerType){
            case "MC":
                return toDtoMC(getAnswerMcSubmission(answerSubmissionId));
            case "ESSAY":
                return toDtoEssay(getAnswerEssaySubmission(answerSubmissionId));
            case "FILE":
                return toDtoFile(getAnswerFileSubmission(answerSubmissionId));
            default: throw new DataServiceException("Error 103: Answer type not supported.");
        }
    }

    @Override
    public AnswerSubmissionDto postAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, long questionSubmissionId) throws IdInPostException, DataServiceException, TypeNotSupportedException, IOException {
        if(answerSubmissionDto.getAnswerSubmissionId() != null){
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }
        answerSubmissionDto.setQuestionSubmissionId(questionSubmissionId);
        String answerType = getAnswerType(questionSubmissionId);
        switch(answerType){
            case "MC":
                AnswerMcSubmission answerMcSubmission;
                try {
                    answerMcSubmission = fromDtoMC(answerSubmissionDto);
                } catch (DataServiceException ex) {
                    throw new DataServiceException("Error 105: Unable to create answer submission: " + ex.getMessage());
                }
                return (toDtoMC(saveMC(answerMcSubmission)));
            case "ESSAY":
                AnswerEssaySubmission answerEssaySubmission;
                try{
                    answerEssaySubmission = fromDtoEssay(answerSubmissionDto);
                } catch (DataServiceException ex) {
                    throw new DataServiceException("Error 105: Unable to create answer submission: " + ex.getMessage());
                }
                return (toDtoEssay(saveEssay(answerEssaySubmission)));
            case "FILE":
                AnswerFileSubmission answerFileSubmission;

                try{
                    answerFileSubmission = fromDtoFile(answerSubmissionDto);
                } catch (DataServiceException ex) {
                    throw new DataServiceException("Error 105: Unable to create file submission: ", ex);
                }

                return toDtoFile(saveFile(answerFileSubmission));
            default: throw new TypeNotSupportedException("Error 103: Answer type not supported.");
        }
    }

    public List<AnswerSubmissionDto> postAnswerSubmissions(List<AnswerSubmissionDto> answerSubmissionDtoList)
            throws IdMissingException, ExceedingLimitException, TypeNotSupportedException, IdInPostException,
            DataServiceException, IOException {

        List<AnswerSubmissionDto> returnedDtoList = new ArrayList<>();

        for (AnswerSubmissionDto answerSubmissionDto : answerSubmissionDtoList) {
            if (answerSubmissionDto.getQuestionSubmissionId() == null) {
                throw new IdMissingException(TextConstants.ID_MISSING);
            }
            if (existsByQuestionSubmissionId(answerSubmissionDto.getQuestionSubmissionId())) {
                throw new ExceedingLimitException(
                        "Error 145: Multiple choice and essay questions can only have one answer submission.");
            }
            AnswerSubmissionDto returnedDto = postAnswerSubmission(answerSubmissionDto,
                    answerSubmissionDto.getQuestionSubmissionId());
            returnedDtoList.add(returnedDto);
        }

        return returnedDtoList;
    }

    private boolean existsByQuestionSubmissionId(Long questionSubmissionId) throws TypeNotSupportedException {
        String answerType = getAnswerType(questionSubmissionId);
        switch (answerType) {
            case "MC":
                List<AnswerMcSubmission> existingMcSubmissions = findByQuestionSubmissionIdMC(questionSubmissionId);
                return !existingMcSubmissions.isEmpty();
            case "ESSAY":
                List<AnswerEssaySubmission> existingEssaySubmissions = findAllByQuestionSubmissionIdEssay(
                        questionSubmissionId);
                return !existingEssaySubmissions.isEmpty();
            default:
                throw new TypeNotSupportedException("Error 103: Answer type not supported.");
        }
    }

    @Override
    public void updateAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, long answerSubmissionId, String answerType) throws AnswerNotMatchingException, DataServiceException {
        switch (answerType) {
            case "MC":
                updateAnswerMcSubmission(answerSubmissionId, answerSubmissionDto);
                break;
            case "ESSAY":
                updateAnswerEssaySubmission(answerSubmissionId, answerSubmissionDto);
                break;
            case "FILE":
                updateAnswerFileSubmission(answerSubmissionId, answerSubmissionDto);
                break;
            default: throw new DataServiceException("Error 103: Answer type not supported.");
        }
    }

    @Override
    public void deleteAnswerSubmission(long answerSubmissionId, String answerType) throws DataServiceException {
        switch(answerType){
            case "MC":
                deleteByIdMC(answerSubmissionId);
                break;
            case "ESSAY":
                deleteByIdEssay(answerSubmissionId);
                break;
            case "FILE":
                deleteByIdFile(answerSubmissionId);
                break;
            default: throw new DataServiceException("Error 103: Answer type not supported.");
        }
    }

    /*
    MULTIPLE CHOICE SUBMISSION METHODS
     */

    @Override
    public List<AnswerMcSubmission> findByQuestionSubmissionIdMC(Long questionSubmissionId){
        return allRepositories.answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmissionId);
    }

    @Override
    public List<AnswerSubmissionDto> getAnswerMcSubmissions(Long questionSubmissionId){
        List<AnswerMcSubmission> answerMcSubmissions = findByQuestionSubmissionIdMC(questionSubmissionId);
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        for(AnswerMcSubmission answerMcSubmission : answerMcSubmissions){
            answerSubmissionDtoList.add(toDtoMC(answerMcSubmission));
        }
        return answerSubmissionDtoList;
    }

    @Override
    public AnswerSubmissionDto toDtoMC(AnswerMcSubmission answer){
        AnswerSubmissionDto answerSubmissionDto = new AnswerSubmissionDto();
        answerSubmissionDto.setAnswerSubmissionId(answer.getAnswerMcSubId());
        answerSubmissionDto.setQuestionSubmissionId(answer.getQuestionSubmission().getQuestionSubmissionId());
        if(answer.getAnswerMc() != null){
            answerSubmissionDto.setAnswerId(answer.getAnswerMc().getAnswerMcId());
        }
        return answerSubmissionDto;
    }

    @Override
    public AnswerMcSubmission fromDtoMC(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException {
        AnswerMcSubmission answerMcSubmission = new AnswerMcSubmission();
        answerMcSubmission.setAnswerMcSubId(answerSubmissionDto.getAnswerSubmissionId());
        if(answerSubmissionDto.getAnswerId() != null){
            Optional<AnswerMc> answerMc = allRepositories.answerMcRepository.findById(answerSubmissionDto.getAnswerId());
            if(answerMc.isPresent()){
                answerMcSubmission.setAnswerMc(answerMc.get());
            } else {
                throw new DataServiceException("The MC answer for the answer submission does not exist.");
            }
        }
        Optional<QuestionSubmission> questionSubmission = allRepositories.questionSubmissionRepository.findById(answerSubmissionDto.getQuestionSubmissionId());
        if(questionSubmission.isPresent()){
            answerMcSubmission.setQuestionSubmission(questionSubmission.get());
        } else {
            throw new DataServiceException("The question submission for the answer submission does not exist.");
        }
        return answerMcSubmission;
    }

    @Override
    public AnswerMcSubmission saveMC(AnswerMcSubmission answerMcSubmission){ return allRepositories.answerMcSubmissionRepository.save(answerMcSubmission); }

    @Override
    public AnswerMcSubmission getAnswerMcSubmission(Long answerSubmissionId) {
        return allRepositories.answerMcSubmissionRepository.findByAnswerMcSubId(answerSubmissionId);
    }

    @Override
    public void updateAnswerMcSubmission(Long id, AnswerSubmissionDto answerSubmissionDto) throws AnswerNotMatchingException {
        AnswerMcSubmission answerMcSubmission = getAnswerMcSubmission(id);
        Optional<AnswerMc> answerMc = allRepositories.answerMcRepository.findById(answerSubmissionDto.getAnswerId());
        if (answerMc.isPresent()) {
            answerMcSubmission.setAnswerMc(answerMc.get());
        } else{
            throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
        }
        saveAndFlushMC(answerMcSubmission);
    }

    @Override
    public void saveAndFlushMC(AnswerMcSubmission answerMcSubmission){ allRepositories.answerMcSubmissionRepository.saveAndFlush( answerMcSubmission); }

    @Override
    public void deleteByIdMC(Long id) throws EmptyResultDataAccessException{ allRepositories.answerMcSubmissionRepository.deleteByAnswerMcSubId(id); }

    @Override
    public boolean mcAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerMcSubmissionId){
        return allRepositories.answerMcSubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(questionSubmissionId, answerMcSubmissionId);
    }



    /*
    ESSAY SUBMISSION METHODS
     */
    @Override
    public List<AnswerEssaySubmission> findAllByQuestionSubmissionIdEssay(Long questionSubmissionId){
        return allRepositories.answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmissionId);
    }

    @Override
    public List<AnswerSubmissionDto> getAnswerEssaySubmissions(Long questionSubmissionId){
        List<AnswerEssaySubmission> answerEssaySubmissions = findAllByQuestionSubmissionIdEssay(questionSubmissionId);
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        for(AnswerEssaySubmission answerEssaySubmission : answerEssaySubmissions){
            answerSubmissionDtoList.add(toDtoEssay(answerEssaySubmission));
        }
        return answerSubmissionDtoList;
    }

    @Override
    public AnswerSubmissionDto toDtoEssay(AnswerEssaySubmission answer){
        AnswerSubmissionDto answerSubmissionDto = new AnswerSubmissionDto();
        answerSubmissionDto.setAnswerSubmissionId(answer.getAnswerEssaySubmissionId());
        answerSubmissionDto.setQuestionSubmissionId(answer.getQuestionSubmission().getQuestionSubmissionId());
        answerSubmissionDto.setResponse(answer.getResponse());
        return answerSubmissionDto;
    }

    @Override
    public AnswerEssaySubmission fromDtoEssay(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException{
        AnswerEssaySubmission answerEssaySubmission = new AnswerEssaySubmission();
        answerEssaySubmission.setAnswerEssaySubmissionId(answerSubmissionDto.getAnswerSubmissionId());
        answerEssaySubmission.setResponse(answerSubmissionDto.getResponse());
        Optional<QuestionSubmission> questionSubmission = questionSubmissionService.findById(answerSubmissionDto.getQuestionSubmissionId());
        if(questionSubmission.isPresent()){
            answerEssaySubmission.setQuestionSubmission(questionSubmission.get());
        } else {
            throw new DataServiceException("Question submission for answer submission does not exist.");
        }
        return answerEssaySubmission;
    }

    @Override
    public AnswerEssaySubmission saveEssay(AnswerEssaySubmission answer){ return allRepositories.answerEssaySubmissionRepository.save(answer); }

    @Override
    public Optional<AnswerEssaySubmission> findByIdEssay(Long id) { return allRepositories.answerEssaySubmissionRepository.findById(id); }

    @Override
    public AnswerEssaySubmission getAnswerEssaySubmission(Long answerSubmissionId){
        return allRepositories.answerEssaySubmissionRepository.findByAnswerEssaySubmissionId(answerSubmissionId);
    }

    @Override
    public void updateAnswerEssaySubmission(Long id, AnswerSubmissionDto answerSubmissionDto){
        AnswerEssaySubmission answerEssaySubmission = getAnswerEssaySubmission(id);
        answerEssaySubmission.setResponse(answerSubmissionDto.getResponse());
        saveAndFlushEssay(answerEssaySubmission);
    }

    @Override
    public void saveAndFlushEssay(AnswerEssaySubmission answer) { allRepositories.answerEssaySubmissionRepository.saveAndFlush(answer); }

    @Override
    public void deleteByIdEssay(Long id) { allRepositories.answerEssaySubmissionRepository.deleteByAnswerEssaySubmissionId(id); }

    @Override
    public boolean essayAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerSubmissionId){
        return allRepositories.answerEssaySubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(questionSubmissionId, answerSubmissionId);
    }

    @Override
    public String getAnswerType(Long questionSubmissionId){
        QuestionSubmission questionSubmission = allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionId);
        return questionSubmission.getQuestion().getQuestionType().toString();
    }


    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long answerSubmissionId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path(
                "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{answer_submission_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, answerSubmissionId).toUri());
        return headers;
    }

    @Override
    public AnswerSubmissionDto toDtoFile(AnswerFileSubmission answerFileSubmission) throws IOException {
        AnswerSubmissionDto answerSubmissionDto = new AnswerSubmissionDto();
        answerSubmissionDto.setAnswerSubmissionId(answerFileSubmission.getAnswerFileSubmissionId());
        answerSubmissionDto.setQuestionSubmissionId(answerFileSubmission.getQuestionSubmission().getQuestionSubmissionId());
        answerSubmissionDto.setMimeType(answerFileSubmission.getMimeType());
        answerSubmissionDto.setFileName(answerFileSubmission.getFileName());

        byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(fileStorageService.getFileSubmissionLocal(answerFileSubmission.getAnswerFileSubmissionId())));
        String fileContent = new String(encoded, StandardCharsets.UTF_8);
        answerSubmissionDto.setFileContent(fileContent);

        return answerSubmissionDto;
    }

    @Override
    public AnswerFileSubmission fromDtoFile(AnswerSubmissionDto answerSubmissionDto) throws DataServiceException {
        AnswerFileSubmission answerFileSubmission = new AnswerFileSubmission();
        answerFileSubmission.setAnswerFileSubmissionId(answerSubmissionDto.getAnswerSubmissionId());
        answerFileSubmission.setFileContent(StringUtils.getBytes(answerSubmissionDto.getFileContent(), StandardCharsets.UTF_8));
        answerFileSubmission.setFileName(answerSubmissionDto.getFileName());
        answerFileSubmission.setMimeType(answerSubmissionDto.getMimeType());
        answerFileSubmission.setFileUri(answerSubmissionDto.getFileUri());
        Optional<QuestionSubmission> questionSubmission = questionSubmissionService.findById(answerSubmissionDto.getQuestionSubmissionId());

        if(!questionSubmission.isPresent()){
            throw new DataServiceException("Question submission for answer submission does not exist.");
        }

        answerFileSubmission.setQuestionSubmission(questionSubmission.get());

        return answerFileSubmission;
    }

    @Override
    public List<AnswerFileSubmission> findAllByQuestionSubmissionIdFile(Long questionSubmissionId) {
        return allRepositories.answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmissionId);
    }

    @Override
    public List<AnswerSubmissionDto> getAnswerFileSubmissions(Long questionSubmissionId) throws IOException {
        List<AnswerFileSubmission> answerFileSubmissions = findAllByQuestionSubmissionIdFile(questionSubmissionId);
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        for(AnswerFileSubmission answerFileSubmission : answerFileSubmissions){
            answerSubmissionDtoList.add(toDtoFile(answerFileSubmission));
        }
        return answerSubmissionDtoList;
    }

    @Override
    public AnswerFileSubmission saveFile(AnswerFileSubmission fileAnswer) {
        return allRepositories.answerFileSubmissionRepository.save(fileAnswer);
    }

    @Override
    public Optional<AnswerFileSubmission> findByIdFile(Long id) {
        return allRepositories.answerFileSubmissionRepository.findById(id);
    }

    @Override
    public AnswerFileSubmission getAnswerFileSubmission(Long answerSubmissionId) {
        return allRepositories.answerFileSubmissionRepository.findByAnswerFileSubmissionId(answerSubmissionId);
    }

    @Override
    public AnswerSubmissionDto getAnswerFileSubmissionDto(long answerSubmissionId) throws IOException {
        AnswerFileSubmission answerFileSubmission = allRepositories.answerFileSubmissionRepository.findByAnswerFileSubmissionId(answerSubmissionId);

        return toDtoFile(answerFileSubmission);
    }

    @Override
    public void updateAnswerFileSubmission(Long id, AnswerSubmissionDto answerSubmissionDto) {
        AnswerFileSubmission answerFileSubmission = getAnswerFileSubmission(id);
        answerFileSubmission.setFileContent(StringUtils.getBytes(answerSubmissionDto.getFileContent(), StandardCharsets.UTF_8));
        saveAndFlushFile(answerFileSubmission);
    }

    @Override
    public void saveAndFlushFile(AnswerFileSubmission answerToChange) {
        allRepositories.answerFileSubmissionRepository.saveAndFlush(answerToChange);
    }

    @Override
    public void deleteByIdFile(Long id) throws EmptyResultDataAccessException {
        allRepositories.answerFileSubmissionRepository.deleteByAnswerFileSubmissionId(id);
    }

    @Override
    public boolean fileAnswerSubmissionBelongsToQuestionSubmission(Long questionSubmissionId, Long answerFileSubmissionId) {
        return allRepositories.answerFileSubmissionRepository.
                existsByQuestionSubmission_QuestionSubmissionIdAndAnswerFileSubmissionId(questionSubmissionId, answerFileSubmissionId);
    }

    public AnswerSubmissionDto handleFileAnswerSubmission(AnswerSubmissionDto answerSubmissionDto, MultipartFile file) throws IdInPostException, DataServiceException, TypeNotSupportedException, IOException {
        String uri = fileStorageService.saveFileSubmissionLocal(file);
        File tempFile = getFile(file, file.getName());
        String fileName = file.getResource().getFilename();
        String mimeType = file.getContentType();
        // byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(tempFile));
        // String fileContent = new String(encoded, StandardCharsets.US_ASCII);

        // answerSubmissionDto.setFileContent(fileContent);
        answerSubmissionDto.setFileName(fileName);
        answerSubmissionDto.setMimeType(mimeType);
        answerSubmissionDto.setFileUri(uri);
        answerSubmissionDto.setFile(tempFile);

        return postAnswerSubmission(answerSubmissionDto, answerSubmissionDto.getQuestionSubmissionId());
    }

    private File getFile(MultipartFile multipartFile, String fileName) {
        File tempFile = new File(fileName);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        } catch (IOException e) {
           log.error("Error while converting Multipart file to file {}",tempFile.getName());
        }

        return tempFile;
    }

}
