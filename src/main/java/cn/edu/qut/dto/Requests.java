package cn.edu.qut.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * 全部请求体 DTO 集中定义为不可变 record, 并忽略前端可能多传的字段。
 */
public final class Requests {

    private Requests() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginRequest(String username, String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegisterRequest(String username, String password, String nickname) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfileRequest(String nickname, String preferenceTags) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentRequest(Long spotId, Integer rating, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecommendRequest(List<String> interests,
                                   Integer durationMinutes,
                                   BigDecimal budget,
                                   String crowd,
                                   String energy,
                                   String province,
                                   String region) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatRequest(String message, String sessionId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StartTourRequest(Long routeId, String sessionId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TourVisitRequest(String sessionId, Long routeId, Long spotId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TourProgressRequest(String sessionId, Long routeId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FavoriteRequest(Long spotId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateOrderRequest(Long spotId, Integer quantity,
                                     String contactName, String contactPhone,
                                     String visitDate, String remark) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdminLoginRequest(String username, String password) {
    }
}
