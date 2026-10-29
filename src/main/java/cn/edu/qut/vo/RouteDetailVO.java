package cn.edu.qut.vo;

import java.util.List;

/**
 * 路线详情: 路线信息 + 分步游览列表 + 封面。
 */
public class RouteDetailVO {

    private Long id;
    private String name;
    private String subtitle;
    private String description;
    private int durationMinutes;
    private int difficulty;
    private List<String> tags;
    private List<String> suitableFor;
    private String coverUrl;
    private int totalSpots;
    private int totalStayMinutes;
    private List<RouteStepVO> spots;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<String> getSuitableFor() { return suitableFor; }
    public void setSuitableFor(List<String> suitableFor) { this.suitableFor = suitableFor; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public int getTotalSpots() { return totalSpots; }
    public void setTotalSpots(int totalSpots) { this.totalSpots = totalSpots; }
    public int getTotalStayMinutes() { return totalStayMinutes; }
    public void setTotalStayMinutes(int totalStayMinutes) { this.totalStayMinutes = totalStayMinutes; }
    public List<RouteStepVO> getSpots() { return spots; }
    public void setSpots(List<RouteStepVO> spots) { this.spots = spots; }
}
