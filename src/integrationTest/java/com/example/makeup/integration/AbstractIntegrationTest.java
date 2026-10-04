package com.example.makeup.integration;

import com.example.makeup.auth.Role;
import com.example.makeup.auth.User;
import com.example.makeup.integration.containers.TestContainersRegistry;
import com.example.makeup.auth.UserRepository;
import com.example.makeup.media.NewsImageStorage;
import com.example.makeup.media.ThumbnailGeneratorService;
import com.example.makeup.media.ThumbnailStorage;
import com.example.makeup.media.VideoStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * База для всех интеграционных тестов makeup-бэкенда:
 * - поднимает полный Spring-контекст с MockMvc (реальные security-фильтры);
 * - БД — PostgreSQL в контейнере (testcontainers), схему создаёт Hibernate;
 * - MinIO и генерацию превью замокаем: тестируем бизнес-логику, а не S3-хранилище;
 * - очищает таблицы между тестами для изоляции.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserDetailsService userDetailsService;

    @MockitoBean
    protected VideoStorage videoStorage;

    @MockitoBean
    protected ThumbnailStorage thumbnailStorage;

    @MockitoBean
    protected NewsImageStorage newsImageStorage;

    @MockitoBean
    protected ThumbnailGeneratorService thumbnailGeneratorService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainersRegistry.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainersRegistry.POSTGRES::getUsername);
        registry.add("spring.datasource.password", TestContainersRegistry.POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @BeforeEach
    @AfterEach
    void cleanDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "TRUNCATE TABLE media_jobs, video_likes, news, videos, users RESTART IDENTITY CASCADE"
            );
        }
    }

    @BeforeEach
    void stubExternalServices() {
        when(videoStorage.upload(any(java.io.InputStream.class), anyLong(), any(), anyString()))
                .thenAnswer(inv -> inv.getArgument(3, String.class) + ".mp4");
        when(thumbnailStorage.upload(any(MultipartFile.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1, String.class));
        when(thumbnailStorage.upload(any(java.awt.image.BufferedImage.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1, String.class) + ".jpeg");
        when(newsImageStorage.upload(any(), any())).thenAnswer(inv -> inv.getArgument(1, String.class) + ".jpeg");
        when(newsImageStorage.get(any())).thenReturn(
                new InputStreamResource(new ByteArrayInputStream(new byte[]{1, 2, 3})));
        when(thumbnailStorage.get(any())).thenReturn(
                new InputStreamResource(new ByteArrayInputStream(new byte[]{1, 2, 3})));
        when(videoStorage.get(any())).thenReturn(
                new InputStreamResource(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}))
        );
        when(videoStorage.get(anyString(), anyLong(), anyLong())).thenReturn(
                new InputStreamResource(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}))
        );
        StatObjectResponse videoStat = mock(StatObjectResponse.class);
        when(videoStat.size()).thenReturn(4L);
        when(videoStat.contentType()).thenReturn("video/mp4");
        when(videoStorage.stat(anyString())).thenReturn(videoStat);
        when(videoStorage.presignedUrl(any())).thenReturn("http://localhost:9000/presigned/video");
        when(thumbnailStorage.presignedUrl(any())).thenReturn("http://localhost:9000/presigned/thumbnail");
    }

    /**
     * Удешевляем BCrypt в тестах (strength 4 вместо 10 по умолчанию),
     * чтобы регистрация/логин не тратили сотни миллисекунд на каждый вызов.
     */
    @TestConfiguration
    static class WeakPasswordEncoderConfig {
        @Bean
        @Primary
        PasswordEncoder testPasswordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }

    protected String registerUser(String username) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", "password123",
                "fullName", "Test " + username
        ));
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    protected User createUser(String username) {
        return createUser(username, "password123");
    }

    protected User createUser(String username, String rawPassword) {
        User user = User.builder()
                .username(username)
                .email(username + "@example.com")
                .password(passwordEncoder.encode(rawPassword))
                .fullName("Test " + username)
                .role(Role.USER)
                .isActive(true)
                .build();
        return userRepository.save(user);
    }

    protected UserDetails loadUser(String username) {
        return userDetailsService.loadUserByUsername(username);
    }

    protected MockMultipartFile imageFile() {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
    }

    protected MockMultipartFile videoFile() {
        return new MockMultipartFile("file", "sample.mp4", "video/mp4", new byte[]{1, 2, 3, 4});
    }
}