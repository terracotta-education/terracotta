package edu.iu.terracotta.service.app.messaging.impl.piped;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItemValue;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessagePipedTextKey;
import edu.iu.terracotta.dao.repository.messaging.piped.PipedTextRepository;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextFileUploadException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextValidationException;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextItemService;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class MessagePipedTextServiceImpl implements MessagePipedTextService {

    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private PipedTextRepository pipedTextRepository;
    @Autowired private MessagePipedTextItemService pipedTextItemService;

    @Override
    public void create(MessagePipedTextDto pipedTextDto, MessageContent content) {
        MessagePipedText pipedText = fromDto(
            pipedTextDto,
            MessagePipedText.builder()
                .content(content)
                .build()
        );

        pipedTextItemService.create(pipedTextDto.getItems(), pipedText);

        content.setPipedText(pipedText);
    }

    @Override
    public void update(MessagePipedTextDto pipedTextDto, MessagePipedText pipedText) {
        fromDto(pipedTextDto, pipedText);
        pipedTextItemService.upsert(pipedTextDto.getItems(), pipedText);
    }

    @Override
    public void upsert(MessagePipedTextDto pipedTextDto, MessageContent content) {
        if (pipedTextDto == null) {
            content.setPipedText(null);
            return;
        }

        if (pipedTextDto.getId() == null) {
            create(pipedTextDto, content);
        } else {
            pipedTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(
                pipedTextDto.getId(),
                content.getUuid(),
                content.getMessage().getContainer().getOwner().getLmsUserId()
            )
            .ifPresentOrElse(
                pipedText -> update(pipedTextDto, pipedText),
                () -> create(pipedTextDto, content)
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessagePipedText processPipedTextCsvFile(Message message, MultipartFile file) throws MessagePipedTextFileUploadException {
        try (CSVReader csvReader = csvReader(file.getInputStream())) {
            if (!Strings.CI.equals(file.getContentType(), "text/csv")) {
                log.error(
                    "Invalid file type for file: [{}]. Expected a CSV file. Message ID: [{}]",
                    file.getOriginalFilename(),
                    message.getId()
                );
                throw new MessagePipedTextFileUploadException(
                    String.format(
                        "Invalid file type for file: [%s]. Expected a CSV file. Message ID: [%s]",
                        file.getOriginalFilename(),
                        message.getId()
                    )
                );
            }

            // get existing piped text or create new if not present
            MessagePipedText pipedText = message.getContent().getPipedText() != null ?
                message.getContent().getPipedText()
                :
                MessagePipedText.builder()
                    .content(message.getContent())
                    .build();

            // read the header row
            String[] headers = csvReader.readNext();
            Map<String, Integer> headerColumnMap = new HashMap<>();

            for (int i = 0; i < headers.length; i++) {
                headerColumnMap.put(headers[i], i);
            }

            List<MessagePipedTextItem> items = Arrays.stream(headers)
                .map(
                    header -> {
                        MessagePipedTextItem pipedTextItem = MessagePipedTextItem.builder()
                            .key(header)
                            .pipedText(pipedText)
                            .build();
                        // set the UUID here for placeholder processing later
                        pipedTextItem.setUuid(UUID.randomUUID());

                        return pipedTextItem;
                    }
                )
                .collect(Collectors.toList());
            String[] row;
            int currentRow = 1;

            while ((row = csvReader.readNext()) != null) {
                currentRow++;

                if (row.length != headers.length) {
                    // row does not contain expected column count; throw error
                    log.error("Row [{}] column count does not match header length for piped text upload file: [{}] in message ID: [{}]",
                        currentRow,
                        file.getOriginalFilename(),
                        message.getId()
                    );
                    throw new MessagePipedTextFileUploadException(
                        String.format(
                            "Row [%s] column count [%s] does not match header count [%s]",
                            currentRow,
                            row.length,
                            headers.length
                        )
                    );
                }

                if (StringUtils.isBlank(row[headerColumnMap.get(MessagePipedTextKey.ID.key())])) {
                    // ID column is blank; skip processing this row
                    continue;
                }

                Optional<LtiUserEntity> user = ltiUserRepository.findFirstByLmsUserIdAndPlatformDeployment(row[headerColumnMap.get(MessagePipedTextKey.ID.key())], message.getPlatformDeployment());

                if (user.isEmpty()) {
                    // user not found; throw error
                    log.error(
                        "User with LMS user ID: [{}] not found in platform deployment with key ID: [{}] for message ID: [{}]",
                        row[headerColumnMap.get(MessagePipedTextKey.ID.key())],
                        message.getPlatformDeployment().getKeyId(),
                        message.getId()
                    );
                    throw new MessagePipedTextFileUploadException(
                        String.format(
                            "User with LMS user ID '%s' not found in experiment",
                            row[headerColumnMap.get(MessagePipedTextKey.ID.key())]
                        )
                    );
                }

                for (int i = 0; i < headers.length; i++) {
                    items.get(i).getValues().add(
                        MessagePipedTextItemValue.builder()
                            .item(items.get(i))
                            .user(user.get())
                            .value(row[i])
                            .build()
                    );
                }
            }

            // set the file name
            pipedText.setFileName(file.getOriginalFilename());

            // clear existing items
            pipedText.getItems().clear();

            // set the new items
            pipedText.getItems().addAll(items);

            return pipedText;
        } catch (Exception e) {
            throw new MessagePipedTextFileUploadException(e.getMessage(), e);
        }
    }

    @Override
    public void validatePipedTextFile(Message message, MultipartFile file) throws MessagePipedTextValidationException, MessagePipedTextFileUploadException {
        /*
         * Required keys:
         *
         * - ID
         */

        List<String> errors = new ArrayList<>();

        try (CSVReader csvReader = csvReader(file.getInputStream())) {
            String[] headers = csvReader.readNext();

            if (ArrayUtils.isEmpty(headers)) {
                errors.add("Expected at least 1 column.");
            }

            if (Arrays.stream(headers).anyMatch(StringUtils::isBlank)) {
                errors.add("File contains a blank column header.");
            }

            // Validate that the required headers are as expected
            if (!Arrays.asList(headers).contains(MessagePipedTextKey.ID.key())) {
                errors.add(
                    String.format(
                        "Expected required headers: [%s].",
                        MessagePipedTextKey.ID.key()
                    )
                );
            }

            // Validate that no duplicate headers exist
            Set<String> elements = new HashSet<>();
            List<String> duplicateHeaders = Arrays.asList(headers).stream()
                .filter(n -> !elements.add(n))
                .toList();

            if (CollectionUtils.isNotEmpty(duplicateHeaders)) {
                errors.add(
                    String.format(
                        "Duplicate headers found: [%s].",
                        String.join(", ", duplicateHeaders)
                    )
                );
            }

            if (CollectionUtils.isNotEmpty(errors)) {
                throw new MessagePipedTextValidationException(errors);
            }
        } catch (MessagePipedTextValidationException e) {
            throw new MessagePipedTextValidationException(errors, e);
        } catch (Exception e) {
            log.error(
                "Error validating piped text file: [{}] for message ID: [{}]",
                file.getOriginalFilename(),
                message.getId(),
                e
            );
            throw new MessagePipedTextValidationException(
                String.format(
                    "Error validating piped text file: '%s'",
                    file.getOriginalFilename()
                ),
                e
            );
        }
    }

    @Override
    public void duplicate(MessagePipedText pipedText, MessageContent content) {
        if (pipedText == null) {
            return;
        }

        MessagePipedText newPipedText = MessagePipedText.builder()
            .content(content)
            .fileName(pipedText.getFileName())
            .build();
        newPipedText.setUuid(UUID.randomUUID());

        pipedTextItemService.duplicate(pipedText.getItems(), newPipedText);

        content.setPipedText(newPipedText);
    }

    @Override
    public MessagePipedText fromDto(MessagePipedTextDto pipedTextDto, MessagePipedText pipedText) {
        return fromDto(pipedTextDto, pipedText, false);
    }

    @Override
    public MessagePipedText fromDto(MessagePipedTextDto pipedTextDto, MessagePipedText pipedText, boolean includeItems) {
        if (pipedTextDto == null) {
            return pipedText;
        }

        pipedText.setUuid(pipedTextDto.getId());
        pipedText.setFileName(pipedTextDto.getFileName());

        if (includeItems) {
            pipedText.getItems().clear();
            pipedText.getItems().addAll(
                pipedTextItemService.fromDto(
                    pipedTextDto.getItems(),
                    pipedText,
                    includeItems
                )
            );
        }

        return pipedText;
    }

    @Override
    public MessagePipedTextDto toDto(MessagePipedText pipedText) {
        if (pipedText == null) {
            return null;
        }

        return MessagePipedTextDto.builder()
            .contentId(pipedText.getContent().getUuid())
            .fileName(pipedText.getFileName())
            .id(pipedText.getUuid())
            .items(
                pipedTextItemService.toDto(pipedText.getItems())
            )
            .build();
    }

    private CSVReader csvReader(InputStream inputStream) throws IOException {
        BOMInputStream bomInputStream = BOMInputStream.builder()
            .setInputStream(inputStream)
            .setByteOrderMarks(
                ByteOrderMark.UTF_8,
                ByteOrderMark.UTF_16LE,
                ByteOrderMark.UTF_16BE,
                ByteOrderMark.UTF_32LE,
                ByteOrderMark.UTF_32BE
            )
            .get();

        return new CSVReader(new BufferedReader(new InputStreamReader(bomInputStream, StandardCharsets.UTF_8)));
    }

}
