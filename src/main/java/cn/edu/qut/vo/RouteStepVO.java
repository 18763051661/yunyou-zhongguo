package cn.edu.qut.vo;

import cn.edu.qut.entity.RouteSpot;
import cn.edu.qut.support.ImageService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 路线中的单个游览步骤, 含图片与 AI 讲解词。
 */
public class RouteStepVO {

    private Long spotId;
    private int dayNo;
    private int stepNo;
    private int stayMinutes;
    private String narration;
    private String spotName;
    private String spotSummary;
    private String imageUrl;
    private String spotProvince;
    private String spotCity;
    private double spotLng;
    private double spotLat;
    private String spotOpenTime;
    private int spotDifficulty;
    private int spotDurationMinutes;
    private BigDecimal spotPrice;
    private double spotRating;
    private List<String> spotTags;

    public static RouteStepVO from(RouteSpot rs, ImageService imageService) {
        RouteStepVO vo = new RouteStepVO();
        vo.spotId = rs.getSpotId();
        vo.dayNo = rs.getDayNo() == null ? 1 : rs.getDayNo();
        vo.stepNo = rs.getStepNo() == null ? 1 : rs.getStepNo();
        vo.stayMinutes = rs.getStayMinutes() == null ? 40 : rs.getStayMinutes();
        vo.narration = rs.getNarration();
        vo.spotName = rs.getSpotName();
        vo.spotSummary = rs.getSpotSummary();
        vo.imageUrl = "/img/spot/" + rs.getSpotId() + "?n=0&v=2";
        vo.spotProvince = rs.getSpotProvince();
        vo.spotCity = rs.getSpotCity();
        vo.spotLng = rs.getSpotLng() == null ? 0 : rs.getSpotLng();
        vo.spotLat = rs.getSpotLat() == null ? 0 : rs.getSpotLat();
        vo.spotOpenTime = rs.getSpotOpenTime();
        vo.spotDifficulty = rs.getSpotDifficulty() == null ? 1 : rs.getSpotDifficulty();
        vo.spotDurationMinutes = rs.getSpotDurationMinutes() == null ? 60 : rs.getSpotDurationMinutes();
        vo.spotPrice = rs.getSpotPrice();
        vo.spotRating = rs.getSpotRating() == null ? 0 : rs.getSpotRating();
        vo.spotTags = rs.getSpotTags() == null ? Collections.emptyList()
                : Arrays.stream(rs.getSpotTags().split("[,，]")).map(String::trim).filter(t -> !t.isEmpty()).toList();
        return vo;
    }

    public Long getSpotId() { return spotId; }
    public int getDayNo() { return dayNo; }
    public int getStepNo() { return stepNo; }
    public int getStayMinutes() { return stayMinutes; }
    public String getNarration() { return narration; }
    public String getSpotName() { return spotName; }
    public String getSpotSummary() { return spotSummary; }
    public String getImageUrl() { return imageUrl; }
    public String getSpotProvince() { return spotProvince; }
    public String getSpotCity() { return spotCity; }
    public double getSpotLng() { return spotLng; }
    public double getSpotLat() { return spotLat; }
    public String getSpotOpenTime() { return spotOpenTime; }
    public int getSpotDifficulty() { return spotDifficulty; }
    public int getSpotDurationMinutes() { return spotDurationMinutes; }
    public BigDecimal getSpotPrice() { return spotPrice; }
    public double getSpotRating() { return spotRating; }
    public List<String> getSpotTags() { return spotTags; }
}
