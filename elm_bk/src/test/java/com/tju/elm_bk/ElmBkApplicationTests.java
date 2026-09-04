package com.tju.elm_bk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.scheduling.enabled=false",
                "jwt.secret=unit-test-only-secret-key-with-at-least-32-characters"
        }
)
class ElmBkApplicationTests {

    @Test
    void contextLoads() {
    }

}
