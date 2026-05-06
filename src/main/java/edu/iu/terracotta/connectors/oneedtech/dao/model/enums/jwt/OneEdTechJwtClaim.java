package edu.iu.terracotta.connectors.oneedtech.dao.model.enums.jwt;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum OneEdTechJwtClaim {

    ALLOWED_ATTEMPTS(null, "allowed_attempts"),
    ONE_ED_TECH("ONE_ED_TECH", "one_ed_tech"),
    USER_ID("userId", "user_id");

    private String key;
    private String _key;

    public String key() {
        return key;
    }

    public String key(int index) {
        switch (index) {
            case 0: {
                return key;
            }
            case 1: {
                return _key;
            }
            default:
                throw new IllegalArgumentException(String.format("Invalid index: [%s]", index));
        }
    }

}
