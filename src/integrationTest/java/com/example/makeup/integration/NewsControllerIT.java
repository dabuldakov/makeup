package com.example.makeup.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-уровень эндпоинтов новостей: security (401 без токена), создание,
 * список, детали, изображение.
 */
class NewsControllerIT extends AbstractIntegrationTest {

    private static final String BEARER = "Bearer ";

    @Test
    void getAllNewsWithoutTokenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/news"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createNewsWithoutTokenReturnsForbidden() throws Exception {
        mockMvc.perform(multipart("/api/news")
                        .param("title", "T")
                        .param("content", "C"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createNewsAndListItForAuthenticatedUser() throws Exception {
        String token = registerUser("dabuldakov");

        mockMvc.perform(multipart("/api/news")
                        .param("title", "Beautiful Makeup")
                        .param("content", "Look at this")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())));

        mockMvc.perform(get("/api/news")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Beautiful Makeup"))
                .andExpect(jsonPath("$.content[0].author").value("dabuldakov"));
    }

    @Test
    void createNewsWithImageExposesImageUrlAndServesBytes() throws Exception {
        String token = registerUser("author1");

        String idJson = mockMvc.perform(multipart("/api/news")
                        .file(imageFile())
                        .param("title", "With image")
                        .param("content", "Content")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(idJson).get("id").asLong();

        mockMvc.perform(get("/api/news/" + id)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.imageUrl", not(emptyOrNullString())))
                .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.startsWith("/api/news/image/")));

        String imageUrl = objectMapper.readTree(
                        mockMvc.perform(get("/api/news/" + id)
                                        .header("Authorization", BEARER + token))
                                .andReturn().getResponse().getContentAsString())
                .get("imageUrl").asText();

        mockMvc.perform(get(imageUrl)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void getNewsWithoutTokenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/news/1"))
                .andExpect(status().isForbidden());
    }
}