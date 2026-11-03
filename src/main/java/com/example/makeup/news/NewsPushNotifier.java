package com.example.makeup.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Сообщает чат-бэкенду о новой новости, чтобы тот разослал push только тем,
 * у кого автор новости есть в контактах.
 *
 * <p>Best-effort: ошибка push не должна ломать публикацию новости.
 */
@Slf4j
@Component
public class NewsPushNotifier {

    private final RestClient client;
    private final String internalToken;

    public NewsPushNotifier(
            @Value("${push.chat-url:https://chat-muzea.su}") String chatUrl,
            @Value("${push.internal-token:}") String internalToken
    ) {
        this.client = RestClient.builder().baseUrl(chatUrl).build();
        this.internalToken = internalToken;
    }

    @Async
    public void notifyNews(String authorUsername, Long newsId, String title, String content) {
        if (internalToken == null || internalToken.isBlank()) {
            log.debug("News push disabled: push.internal-token is empty");
            return;
        }
        String body = content == null ? "" : content;
        if (body.length() > 140) {
            body = body.substring(0, 140) + "...";
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("authorUsername", authorUsername);
        payload.put("newsId", newsId);
        payload.put("title", title == null ? "" : title);
        payload.put("body", body);

        try {
            client.post()
                    .uri("/api/internal/push/news")
                    .header("X-Internal-Token", internalToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send news push for id={}: {}", newsId, e.getMessage());
        }
    }
}
