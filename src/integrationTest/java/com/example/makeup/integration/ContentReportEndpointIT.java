package com.example.makeup.integration;

import com.example.makeup.auth.User;
import com.example.makeup.news.NewsService;
import com.example.makeup.video.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-жалобы на новости и видео: успешная жалоба, идемпотентный повтор,
 * запрет жаловаться на свой контент, 404 на несуществующий и 403 анониму.
 */
class ContentReportEndpointIT extends AbstractIntegrationTest {

    @Autowired
    private NewsService newsService;
    @Autowired
    private VideoService videoService;

    private long createNews(User author) {
        return newsService.createNews("Title", "Body", null, author.getUsername(), imageFile());
    }

    private long createVideo(User author) {
        return videoService.uploadVideo(videoFile(), "Video", "desc", imageFile(),
                author.getUsername()).getId();
    }

    private String body(String type, long id) {
        return "{\"contentType\":\"" + type + "\",\"contentId\":" + id
                + ",\"reason\":\"SPAM\",\"comment\":\"x\"}";
    }

    @Test
    void reportsNewsAndVideoAndRepeatsIdempotently() throws Exception {
        User owner = createUser("report_owner");
        User reporter = createUser("report_reporter");
        long news = createNews(owner);
        long video = createVideo(owner);

        mockMvc.perform(post("/api/reports").with(auth(reporter))
                        .contentType(MediaType.APPLICATION_JSON).content(body("NEWS", news)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("NEWS"))
                .andExpect(jsonPath("$.status").value("NEW"));

        // Повторная жалоба на тот же контент не создаёт дубль.
        mockMvc.perform(post("/api/reports").with(auth(reporter))
                        .contentType(MediaType.APPLICATION_JSON).content(body("NEWS", news)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reports").with(auth(reporter))
                        .contentType(MediaType.APPLICATION_JSON).content(body("VIDEO", video)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("VIDEO"));
    }

    @Test
    void ownContentCannotBeReported() throws Exception {
        User owner = createUser("report_self");
        long news = createNews(owner);
        long video = createVideo(owner);

        mockMvc.perform(post("/api/reports").with(auth(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body("NEWS", news)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/reports").with(auth(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body("VIDEO", video)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownContentReturnsNotFound() throws Exception {
        User reporter = createUser("report_missing");

        mockMvc.perform(post("/api/reports").with(auth(reporter))
                        .contentType(MediaType.APPLICATION_JSON).content(body("NEWS", 999_999)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/reports").with(auth(reporter))
                        .contentType(MediaType.APPLICATION_JSON).content(body("VIDEO", 999_999)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousCannotReport() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON).content(body("NEWS", 1)))
                .andExpect(status().isForbidden());
    }
}
