package cn.edu.qut.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 收藏记录, 附带景点展示字段。
 */
public class Favorite {
    private Long id;
    private Long userId;
    private Long spotId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // 关联景点字段
    private String spotName;
    private String spotSummary;
    private String spotImagePrompt;
    private Double spotRating;
    private Long spotCategoryId;
    private String categoryName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSpotId() { return spotId; }
    public void setSpotId(Long spotId) { this.spotId = spotId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }
    public String getSpotSummary() { return spotSummary; }
    public void setSpotSummary(String spotSummary) { this.spotSummary = spotSummary; }
    public String getSpotImagePrompt() { return spotImagePrompt; }
    public void setSpotImagePrompt(String spotImagePrompt) { this.spotImagePrompt = spotImagePrompt; }
    public Double getSpotRating() { return spotRating; }
    public void setSpotRating(Double spotRating) { this.spotRating = spotRating; }
    public Long getSpotCategoryId() { return spotCategoryId; }
    public void setSpotCategoryId(Long spotCategoryId) { this.spotCategoryId = spotCategoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
