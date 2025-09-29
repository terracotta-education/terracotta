package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.UserExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.UserReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.UserWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.impl.UserImpl;
import edu.ksu.canvas.model.User;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;

public class UserExtendedImpl extends BaseImpl<UserExtended, UserReaderExtended, UserWriterExtended> implements UserReaderExtended, UserWriterExtended {

    private UserImpl userReader = null;

    public UserExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.userReader = new UserImpl(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<UserExtended> getUsersInCourse(GetUsersInCourseOptions getUsersInCourseOptions) throws IOException {
        return parseList(userReader.getUsersInCourse(getUsersInCourseOptions));
    }

    @Override
    protected Type listType() {
        return new TypeToken<List<UserExtended>>() {}.getType();
    }

    @Override
    protected Class<UserExtended> objectType() {
        return UserExtended.class;
    }

    private List<UserExtended> parseList(List<User> users) {
        List<UserExtended> userExtendedList = new ArrayList<>();

        users.stream()
            .forEach(
                user -> userExtendedList.add(
                    UserExtended.builder()
                        .user(user)
                        .type(User.class)
                        .build()
                )
            );

            return userExtendedList;
    }

}
