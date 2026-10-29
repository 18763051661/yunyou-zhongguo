package cn.edu.qut.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 在线游览打卡记录, 附带景点展示字段。
 */
public class TourRecord {
    private Long id;
    private String sessionId;
    private Long userId;
    private Long routeId;
    private Long spotId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime visitedAt;

    private String spotName;
    private String spotImagePrompt;
    private String spotProvince;
    private String spotCity;
    private Double spotLng;
    private Double spotLat;
    private Long spotCategoryId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public Long getSpotId() { return spotId; }
    public void setSpotId(Long spotId) { this.spotId = spotId; }
    public LocalDateTime getVisitedAt() { return visitedAt; }
    public void setVisitedAt(LocalDateTime visitedAt) { this.visitedAt = visitedAt; }
    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }
    public String getSpotImagePrompt() { return spotImagePrompt; }
    public void setSpotImagePrompt(String spotImagePrompt) { this.spotImagePrompt = spotImagePrompt; }
    public String getSpotProvince() { return spotProvince; }
    public void setSpotProvince(String spotProvince) { this.spotProvince = spotProvince; }
    public String getSpotCity() { return spotCity; }
    public void setSpotCity(String spotCity) { this.spotCity = spotCity; }
    public Double getSpotLng() { return spotLng; }
    public void setSpotLng(Double spotLng) { this.spotLng = spotLng; }
    public Double getSpotLat() { return spotLat; }
    public void setSpotLat(Double spotLat) { this.spotLat = spotLat; }
    public Long getSpotCategoryId() { return spotCategoryId; }
    public void setSpotCategoryId(Long spotCategoryId) { this.spotCategoryId = spotCategoryId; }
}
