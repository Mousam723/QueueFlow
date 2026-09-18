package com.queueflow.observability;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class RequestLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLogRequestWithoutAffectingResponse()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/999999")
        )
                .andExpect(status().isNotFound());
    }
}
