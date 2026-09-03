package com.tju.elm_bk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.scheduling.enabled=false"
)
class ElmBkApplicationTests {

    @Test
    void contextLoads() {
    }

}
