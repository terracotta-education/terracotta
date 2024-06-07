package edu.iu.terracotta.model.oauth2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LTIToken {

    private String access_token;
    private String token_type;
    private int expires_in;
    private String scope;

}
