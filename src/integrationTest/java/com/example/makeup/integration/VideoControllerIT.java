package com.example.makeup.integration;

import com.example.makeup.entity.Video;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-уровень эндпоинтов видео: защита, загрузка multipart, список,
 * просмотры, стриминг и presigned URL.
 */
class VideoControllerIT extends AbstractIntegrationTest {

    private static final String BEARER = "Bearer ";

    @Autowired
    private com.example.makeup.repository.VideoRepository videoRepository;

    @Test
    void uploadVideoWithoutTokenReturnsForbidden() throws Exception {
        mockMvc.perform(multipart("/api/videos/upload")
                        .file(videoFile())
                        .param("title", "T"))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadVideoReturnsVideoResponse() throws Exception {
        String token = registerUser("author1");

        mockMvc.perform(multipart("/api/videos/upload")
                        .file(videoFile())
                        .param("title", "My Movie")
                        .param("description", "Great film")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.title").value("My Movie"))
                .andExpect(jsonPath("$.url", containsString("/api/videos/stream/")))
                .andExpect(jsonPath("$.uploadedBy").value("author1"));
    }

    @Test
    void uploadVideoWithThumbnailReturnsThumbnailUrlInResponse() throws Exception {
        String token = registerUser("thumb-author");

        mockMvc.perform(multipart("/api/videos/upload")
                        .file(videoFile())
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "thumbnail", "preview.jpeg", "image/jpeg", new byte[]{9, 9, 9}))
                        .param("title", "With thumb")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thumbnailUrl", containsString("/api/videos/thumbnail/")));
    }

    @Test
    void getAllVideosReturnsUploadedVideos() throws Exception {
        String token = registerUser("author2");
        upload(token, "Video 1");
        upload(token, "Video 2");

        mockMvc.perform(get("/api/videos")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getVideosWithoutTokenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk());
    }

    @Test
    void getVideoIncrementsViews() throws Exception {
        String token = registerUser("author3");
        long id = upload(token, "Count views");

        mockMvc.perform(get("/api/videos/" + id).header("Authorization", BEARER + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/videos/" + id).header("Authorization", BEARER + token))
                .andExpect(status().isOk());

        Video video = videoRepository.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(video.getViews()).isEqualTo(2);
    }

    @Test
    void streamVideoReturnsFile() throws Exception {
        String token = registerUser("author4");
        long id = upload(token, "Stream me");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/stream/" + fileName)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk());
    }

    @Test
    void getVideoPresignedUrlReturnsUrl() throws Exception {
        String token = registerUser("author5");
        long id = upload(token, "Url me");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/url/" + fileName)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsString("presigned")));
    }

    private long upload(String token, String title) throws Exception {
        String body = mockMvc.perform(multipart("/api/videos/upload")
                        .file(videoFile())
                        .param("title", title)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void likeAndUnlikeChangeLikesCount() throws Exception {
        String token = registerUser("liker");
        long id = upload(token, "Like endpoint");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/videos/" + id + "/like").header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1));

        mockMvc.perform(delete("/api/videos/" + id + "/like").header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(0));
    }

    @Test
    void deleteVideoWithoutTokenReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/videos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteVideoAsOwnerReturnsNoContent() throws Exception {
        String token = registerUser("owner");
        long id = upload(token, "Remove me");

        mockMvc.perform(delete("/api/videos/" + id).header("Authorization", BEARER + token))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(videoRepository.findById(id)).isEmpty();
    }

    @Test
    void deleteVideoAsOtherUserReturnsForbidden() throws Exception {
        String ownerToken = registerUser("owner2");
        String otherToken = registerUser("other2");
        long id = upload(ownerToken, "Not yours");

        mockMvc.perform(delete("/api/videos/" + id).header("Authorization", BEARER + otherToken))
                .andExpect(status().isForbidden());

        org.assertj.core.api.Assertions.assertThat(videoRepository.findById(id)).isPresent();
    }

    @Test
    void deleteVideoWithUnknownIdReturnsNotFound() throws Exception {
        String token = registerUser("owner3");

        mockMvc.perform(delete("/api/videos/999").header("Authorization", BEARER + token))
                .andExpect(status().isNotFound());
    }
}