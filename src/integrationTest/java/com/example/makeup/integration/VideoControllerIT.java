package com.example.makeup.integration;

import com.example.makeup.auth.User;
import com.example.makeup.video.Video;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-уровень эндпоинтов видео: защита, загрузка multipart, список,
 * просмотры, стриминг и presigned URL.
 */
class VideoControllerIT extends AbstractIntegrationTest {

    @Autowired
    private com.example.makeup.video.VideoRepository videoRepository;

    @Test
    void uploadVideoWithoutTokenReturnsForbidden() throws Exception {
        mockMvc.perform(multipart("/api/videos/upload")
                        .file(videoFile())
                        .param("title", "T"))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadVideoReturnsVideoResponse() throws Exception {
        User author = createUser("author1");

        mockMvc.perform(multipart("/api/videos/upload")
                        .file(videoFile())
                        .param("title", "My Movie")
                        .param("description", "Great film")
                        .with(auth(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.title").value("My Movie"))
                .andExpect(jsonPath("$.url", containsString("/api/videos/stream/")))
                .andExpect(jsonPath("$.uploadedBy").value("author1"));
    }

    @Test
    void uploadVideoWithThumbnailReturnsThumbnailUrlInResponse() throws Exception {
        User author = createUser("thumb-author");

        mockMvc.perform(multipart("/api/videos/upload")
                        .file(videoFile())
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "thumbnail", "preview.jpeg", "image/jpeg", new byte[]{9, 9, 9}))
                        .param("title", "With thumb")
                        .with(auth(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thumbnailUrl", containsString("/api/videos/thumbnail/")));
    }

    @Test
    void getAllVideosReturnsUploadedVideos() throws Exception {
        User author = createUser("author2");
        upload(author, "Video 1");
        upload(author, "Video 2");

        mockMvc.perform(get("/api/videos")
                        .with(auth(author)))
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
        User author = createUser("author3");
        long id = upload(author, "Count views");

        mockMvc.perform(get("/api/videos/" + id).with(auth(author)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/videos/" + id).with(auth(author)))
                .andExpect(status().isOk());

        Video video = videoRepository.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(video.getViews()).isEqualTo(2);
    }

    @Test
    void streamVideoReturnsFile() throws Exception {
        User author = createUser("author4");
        long id = upload(author, "Stream me");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/stream/" + fileName)
                        .with(auth(author)))
                .andExpect(status().isOk());
    }

    @Test
    void streamVideoFullRequestAdvertisesAcceptRanges() throws Exception {
        User author = createUser("range-author-full");
        long id = upload(author, "Full range");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/stream/" + fileName)
                        .with(auth(author)))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().longValue("Content-Length", 4))
                .andExpect(header().string("Content-Type", "video/mp4"));
    }

    @Test
    void streamVideoSupportsByteRange() throws Exception {
        User author = createUser("range-author-1");
        long id = upload(author, "Byte range");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/stream/" + fileName)
                        .with(auth(author))
                        .header("Range", "bytes=0-1"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Content-Range", "bytes 0-1/4"))
                .andExpect(header().longValue("Content-Length", 2));
    }

    @Test
    void streamVideoSupportsSuffixRange() throws Exception {
        User author = createUser("range-author-2");
        long id = upload(author, "Suffix range");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/stream/" + fileName)
                        .with(auth(author))
                        .header("Range", "bytes=-2"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 2-3/4"))
                .andExpect(header().longValue("Content-Length", 2));
    }

    @Test
    void streamVideoOpenEndedRangeReturnsToEnd() throws Exception {
        User author = createUser("range-author-3");
        long id = upload(author, "Open range");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/stream/" + fileName)
                        .with(auth(author))
                        .header("Range", "bytes=2-"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 2-3/4"))
                .andExpect(header().longValue("Content-Length", 2));
    }

    @Test
    void streamVideoUnsatisfiableRangeReturns416() throws Exception {
        User author = createUser("range-author-4");
        long id = upload(author, "Bad range");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/stream/" + fileName)
                        .with(auth(author))
                        .header("Range", "bytes=100-200"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string("Content-Range", "bytes */4"));
    }

    @Test
    void getVideoPresignedUrlReturnsUrl() throws Exception {
        User author = createUser("author5");
        long id = upload(author, "Url me");
        String fileName = videoRepository.findById(id).orElseThrow().getObjectKey();

        mockMvc.perform(get("/api/videos/url/" + fileName)
                        .with(auth(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsString("presigned")));
    }

    private long upload(User author, String title) throws Exception {
        String body = mockMvc.perform(multipart("/api/videos/upload")
                        .file(videoFile())
                        .param("title", title)
                        .with(auth(author)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void likeAndUnlikeChangeLikesCount() throws Exception {
        User liker = createUser("liker");
        long id = upload(liker, "Like endpoint");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/videos/" + id + "/like").with(auth(liker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1));

        mockMvc.perform(delete("/api/videos/" + id + "/like").with(auth(liker)))
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
        User owner = createUser("owner");
        long id = upload(owner, "Remove me");

        mockMvc.perform(delete("/api/videos/" + id).with(auth(owner)))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(videoRepository.findById(id)).isEmpty();
    }

    @Test
    void deleteVideoAsOtherUserReturnsForbidden() throws Exception {
        User owner = createUser("owner2");
        User other = createUser("other2");
        long id = upload(owner, "Not yours");

        mockMvc.perform(delete("/api/videos/" + id).with(auth(other)))
                .andExpect(status().isForbidden());

        org.assertj.core.api.Assertions.assertThat(videoRepository.findById(id)).isPresent();
    }

    @Test
    void deleteVideoWithUnknownIdReturnsNotFound() throws Exception {
        User owner = createUser("owner3");

        mockMvc.perform(delete("/api/videos/999").with(auth(owner)))
                .andExpect(status().isNotFound());
    }
}
