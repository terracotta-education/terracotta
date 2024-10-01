package edu.iu.terracotta.base;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseTest extends BaseServiceTest {

    public void setup() {
        try {
            super.setup();
        } catch (Exception e) {
            log.error("Exception occurred in BaseTest setup()", e);
        }
    }

}
