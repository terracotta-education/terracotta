package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.FileExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.FileReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.FileWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.impl.FileImpl;
import edu.ksu.canvas.model.File;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;

public class FileExtendedImpl extends BaseImpl<FileExtended, FileReaderExtended, FileWriterExtended> implements FileReaderExtended, FileWriterExtended {

    private FileImpl fileImpl;

    public FileExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        fileImpl = new FileImpl(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public Optional<FileExtended> getFile(String url) throws IOException {
        return parseOptional(fileImpl.getFile(url));
    }

    @Override
    public List<FileExtended> getFiles(String filesUrl) throws IOException {
        String url = buildCanvasUrl(
            filesUrl,
            Collections.emptyMap()
        );

        return parseList(getListResponseFromCanvas(url));
    }

    private List<FileExtended> parseList(List<Response> responses) {
        List<FileExtended> fileExtendedList = new ArrayList<>();

        responses.stream()
            .forEach(response -> fileExtendedList.addAll(parseResponseList(response)));

        return fileExtendedList;
    }

    private List<FileExtended> parseResponseList(Response response) {
        List<FileExtended> fileExtendedList = responseParser.parseToList(
                listType(),
                response
            );
        List<File> fileList = responseParser.parseToList(
            new TypeToken<List<File>>() {}.getType(),
            response
        );

        AtomicInteger index = new AtomicInteger(0);

        return fileExtendedList.stream()
            .map(
                fileExtended -> {
                    fileExtended.setFile(fileList.get(index.getAndIncrement()));
                    fileExtended.setType(File.class);

                    return fileExtended;
                }
            )
            .toList();
    }

    private List<Response> getListResponseFromCanvas(String url) throws IOException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        List<Response> responses = canvasMessenger.getFromCanvas(oauthToken, url, consumer);
        responseCallback = null;

        return responses;
    }

    @Override
    protected Type listType() {
        return new TypeToken<List<FileExtended>>() {}.getType();
    }

    @Override
    protected Class<FileExtended> objectType() {
        return FileExtended.class;
    }

    private Optional<FileExtended> parseOptional(Optional<File> file) {
        return Optional.of(
            FileExtended.builder()
                .file(file.get())
                .type(File.class)
                .build()
        );
    }

}
