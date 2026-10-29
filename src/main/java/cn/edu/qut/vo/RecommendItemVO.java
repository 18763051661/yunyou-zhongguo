package cn.edu.qut.vo;

import java.util.List;

/**
 * 智能推荐结果项: 景点信息 + 匹配分 + 推荐理由。
 */
public class RecommendItemVO {

    private SpotVO spot;
    private int score;
    private String matchLevel;
    private List<String> reasons;

    public RecommendItemVO(SpotVO spot, int score, String matchLevel, List<String> reasons) {
        this.spot = spot;
        this.score = score;
        this.matchLevel = matchLevel;
        this.reasons = reasons;
    }

    public SpotVO getSpot() { return spot; }
    public int getScore() { return score; }
    public String getMatchLevel() { return matchLevel; }
    public List<String> getReasons() { return reasons; }
}
