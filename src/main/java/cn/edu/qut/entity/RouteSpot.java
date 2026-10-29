package cn.edu.qut.entity;

import java.math.BigDecimal;

/**
 * 路线中的一个景点步骤。route_spot 字段 + 关联景点的展示字段。
 */
public class RouteSpot {
    private Long id;
    private Long routeId;
    private Long spotId;
    private Integer dayNo;
    private Integer stepNo;
    private Integer stayMinutes;
    private String narration;

    // 关联景点冗余展示字段
    private String spotName;
    private String spotSummary;
    private String spotImagePrompt;
    private String spotProvince;
    private String spotCity;
    private Double spotLng;
    private Double spotLat;
    private String spotOpenTime;
    private Integer spotDifficulty;
    private Integer spotDurationMinutes;
    private BigDecimal spotPrice;
    private Double spotRating;
    private String spotTags;
    private Long spotCategoryId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public Long getSpotId() { return spotId; }
    public void setSpotId(Long spotId) { this.spotId = spotId; }
    public Integer getDayNo() { return dayNo; }
    public void setDayNo(Integer dayNo) { this.dayNo = dayNo; }
    public Integer getStepNo() { return stepNo; }
    public void setStepNo(Integer stepNo) { this.stepNo = stepNo; }
    public Integer getStayMinutes() { return stayMinutes; }
    public void setStayMinutes(Integer stayMinutes) { this.stayMinutes = stayMinutes; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }
    public String getSpotSummary() { return spotSummary; }
    public void setSpotSummary(String spotSummary) { this.spotSummary = spotSummary; }
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
    public String getSpotOpenTime() { return spotOpenTime; }
    public void setSpotOpenTime(String spotOpenTime) { this.spotOpenTime = spotOpenTime; }
    public Integer getSpotDifficulty() { return spotDifficulty; }
    public void setSpotDifficulty(Integer spotDifficulty) { this.spotDifficulty = spotDifficulty; }
    public Integer getSpotDurationMinutes() { return spotDurationMinutes; }
    public void setSpotDurationMinutes(Integer spotDurationMinutes) { this.spotDurationMinutes = spotDurationMinutes; }
    public BigDecimal getSpotPrice() { return spotPrice; }
    public void setSpotPrice(BigDecimal spotPrice) { this.spotPrice = spotPrice; }
    public Double getSpotRating() { return spotRating; }
    public void setSpotRating(Double spotRating) { this.spotRating = spotRating; }
    public String getSpotTags() { return spotTags; }
    public void setSpotTags(String spotTags) { this.spotTags = spotTags; }
    public Long getSpotCategoryId() { return spotCategoryId; }
    public void setSpotCategoryId(Long spotCategoryId) { this.spotCategoryId = spotCategoryId; }
}
