package dev.example.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {
    @Autowired
    MockMvc mvc;

    @Test
    void analyzesText() throws Exception {
        mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello world\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"lines\":1,\"words\":2,\"codePoints\":11}"));
    }

    @Test
    void analyzesUnicodeText() throws Exception {
        mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello \\uD83C\\uDF0D\\n\\u4F60\\u597D\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").value(2))
                .andExpect(jsonPath("$.words").value(3))
                .andExpect(jsonPath("$.codePoints").value(10));
    }

    @Test
    void acceptsEmptyText() throws Exception {
        mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"lines\":0,\"words\":0,\"codePoints\":0}"));
    }

    @Test
    void rejectsMissingText() throws Exception {
        mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNullText() throws Exception {
        mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidJson() throws Exception {
        mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":"))
                .andExpect(status().isBadRequest());
    }
}
