package cn.edu.qut.entity;

/** AI 生成的个性化出游路线推荐 */
public class TravelPlan {
    private Long id;
    private Long prefId;
    private String title;
    private String summary;
    private Integer days;
    private String spotsJson;    // JSON 数组: [{day, sort_no, spotId, name, province, price}]
    private String itinerary;    // AI 生成的逐日 Markdown
    private String provider;     // 命中的 AI 引擎名
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPrefId() { return prefId; }
    public void setPrefId(Long prefId) { this.prefId = prefId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public String getSpotsJson() { return spotsJson; }
    public void setSpotsJson(String spotsJson) { this.spotsJson = spotsJson; }
    public String getItinerary() { return itinerary; }
    public void setItinerary(String itinerary) { this.itinerary = itinerary; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
