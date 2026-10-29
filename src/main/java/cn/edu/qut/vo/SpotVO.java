package cn.edu.qut.vo;

import cn.edu.qut.entity.ScenicSpot;
import cn.edu.qut.support.ImageService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 景点展示对象: 在实体基础上补充分类名、标签数组、图片地址。
 */
public class SpotVO {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String province;
    private String city;
    private String region;
    private String alias;
    private String summary;
    private String description;
    private List<String> highlights;
    private List<String> tags;
    private String imageUrl;
    private List<String> gallery;
    private double rating;
    private int ratingCount;
    private int popularity;
    private BigDecimal price;
    private String openTime;
    private int durationMinutes;
    private int difficulty;
    private double lng;
    private double lat;
    private String address;
    private List<String> suitableFor;
    private String tips;
    private boolean favorited;

    public static SpotVO from(ScenicSpot s, String categoryName, ImageService imageService) {
        SpotVO vo = new SpotVO();
        vo.id = s.getId();
        vo.name = s.getName();
        vo.categoryId = s.getCategoryId();
        vo.categoryName = categoryName;
        vo.province = s.getProvince();
        vo.city = s.getCity();
        vo.region = s.getRegion();
        vo.alias = s.getAlias();
        vo.summary = s.getSummary();
        vo.description = s.getDescription();
        vo.highlights = split(s.getHighlights());
        vo.tags = split(s.getTags());
        vo.imageUrl = imageBase(s.getId()) + "&n=0";
        vo.gallery = List.of(imageBase(s.getId()) + "&n=1",
                imageBase(s.getId()) + "&n=2",
                imageBase(s.getId()) + "&n=3");
        vo.rating = s.getRating() == null ? 0 : s.getRating();
        vo.ratingCount = s.getRatingCount() == null ? 0 : s.getRatingCount();
        vo.popularity = s.getPopularity() == null ? 0 : s.getPopularity();
        vo.price = s.getPrice();
        vo.openTime = s.getOpenTime();
        vo.durationMinutes = s.getDurationMinutes() == null ? 0 : s.getDurationMinutes();
        vo.difficulty = s.getDifficulty() == null ? 1 : s.getDifficulty();
        vo.lng = s.getLng() == null ? 0 : s.getLng();
        vo.lat = s.getLat() == null ? 0 : s.getLat();
        vo.address = s.getAddress();
        vo.suitableFor = split(s.getSuitableFor());
        vo.tips = s.getTips();
        return vo;
    }

    private static String imageBase(Long id) {
        // v=2: 强制浏览器丢弃旧版随机/错版图缓存
        return "/img/spot/" + id + "?v=2";
    }

    private static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(text.split("[,，]"))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toList();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getAlias() { return alias; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public List<String> getHighlights() { return highlights; }
    public List<String> getTags() { return tags; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getGallery() { return gallery; }
    public double getRating() { return rating; }
    public int getRatingCount() { return ratingCount; }
    public int getPopularity() { return popularity; }
    public BigDecimal getPrice() { return price; }
    public String getOpenTime() { return openTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getDifficulty() { return difficulty; }
    public double getLng() { return lng; }
    public double getLat() { return lat; }
    public String getAddress() { return address; }
    public List<String> getSuitableFor() { return suitableFor; }
    public String getTips() { return tips; }
    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }
}
