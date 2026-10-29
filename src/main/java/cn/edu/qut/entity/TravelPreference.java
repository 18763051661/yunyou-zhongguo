package cn.edu.qut.entity;

/** 用户出游偏好问卷(智能规划表单) */
public class TravelPreference {
    private Long id;
    private String nickname;
    private Integer days;
    private String provinces;   // 逗号分隔
    private String categories;  // 逗号分隔
    private String budget;
    private String companions;
    private String transport;
    private String focus;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public String getProvinces() { return provinces; }
    public void setProvinces(String provinces) { this.provinces = provinces; }
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }
    public String getCompanions() { return companions; }
    public void setCompanions(String companions) { this.companions = companions; }
    public String getTransport() { return transport; }
    public void setTransport(String transport) { this.transport = transport; }
    public String getFocus() { return focus; }
    public void setFocus(String focus) { this.focus = focus; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
