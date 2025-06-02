package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.ConversationExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.ConversationReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.ConversationWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.impl.ConversationImpl;
import edu.ksu.canvas.model.Conversation;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.CreateConversationOptions;
import edu.ksu.canvas.requestOptions.GetSingleConversationOptions;

public class ConversationExtendedImpl extends BaseImpl<ConversationExtended, ConversationReaderExtended, ConversationWriterExtended> implements ConversationReaderExtended, ConversationWriterExtended {

    ConversationImpl conversationImpl = null;

    public ConversationExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.conversationImpl = new ConversationImpl(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<ConversationExtended> createConversation(CreateConversationOptions createonversationOptions) throws IOException {
        return parseList(conversationImpl.createConversation(createonversationOptions));
    }

    @Override
    public Optional<ConversationExtended> getSingleConversation(GetSingleConversationOptions getSingleConversationOptions) throws IOException {
        return parseOptional(conversationImpl.getSingleConversation(getSingleConversationOptions));
    }

    @Override
    protected Type listType() {
        return new TypeToken<List<Conversation>>() {}.getType();
    }

    @Override
    protected Class<ConversationExtended> objectType() {
        return ConversationExtended.class;
    }

    private List<ConversationExtended> parseList(List<Conversation> conversations) {
        List<ConversationExtended> conversationExtendedList = new ArrayList<>();

        conversations.forEach(
            conversation ->
                conversationExtendedList.add(
                    ConversationExtended.builder()
                        .conversation(conversation)
                        .build()
                )
        );

        return conversationExtendedList;
    }

    private Optional<ConversationExtended> parseOptional(Optional<Conversation> conversation) {
        return Optional.of(
            ConversationExtended.builder()
                .conversation(conversation.get())
                .type(Conversation.class)
                .build()
        );
    }

}
