package com.queueflow.observability;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/999999")
        )
                .andExpect(status().isNotFound())
                .andExpect(
                        header().exists("X-Correlation-ID")
                );
    }

    @Test
    void shouldPreserveProvidedCorrelationId()
            throws Exception {

        String correlationId =
                "test-correlation-id-123";

        mockMvc.perform(
                get("/api/v1/businesses/999999")
                        .header(
                                "X-Correlation-ID",
                                correlationId
                        )
        )
                .andExpect(status().isNotFound())
                .andExpect(
                        header().string(
                                "X-Correlation-ID",
                                correlationId
                        )
                );
    }
}