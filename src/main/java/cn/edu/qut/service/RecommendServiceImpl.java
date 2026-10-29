package cn.edu.qut.service;

import cn.edu.qut.dto.Requests;
import cn.edu.qut.vo.RecommendItemVO;
import cn.edu.qut.vo.SpotVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 智能推荐引擎: 多因子加权评分模型。
 * 因子: 兴趣标签匹配(35) 评分口碑(20) 热度(15) 人群适配(10) 时长适配(8) 预算适配(7) + 难度硬约束。
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    private final SpotService spotService;

    public RecommendServiceImpl(SpotService spotService) {
        this.spotService = spotService;
    }

    @Override
    public List<RecommendItemVO> recommend(Requests.RecommendRequest request) {
        List<String> interests = request.interests() == null ? List.of()
                : request.interests().stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        int available = request.durationMinutes() == null || request.durationMinutes() <= 0 ? 120 : request.durationMinutes();
        BigDecimal budget = request.budget() == null ? new BigDecimal("9999") : request.budget();
        String crowd = request.crowd() == null ? "" : request.crowd().trim();
        String province = request.province() == null ? "" : request.province().trim();
        String region = request.region() == null ? "" : request.region().trim();
        String energy = request.energy() == null ? "" : request.energy().trim();
        int maxDifficulty;
        if ("轻松".equals(energy)) maxDifficulty = 2;
        else if ("挑战".equals(energy)) maxDifficulty = 5;
        else maxDifficulty = 3;

        List<SpotVO> all = spotService.all();
        int maxPopularity = all.stream().mapToInt(SpotVO::getPopularity).max().orElse(1);

        List<RecommendItemVO> items = new ArrayList<>();
        for (SpotVO spot : all) {
            // 体力硬约束: 超过可接受难度直接不推荐
            if (spot.getDifficulty() > maxDifficulty) {
                continue;
            }

            double score = 0;
            List<String> reasons = new ArrayList<>();

            // 1. 兴趣标签匹配 35 分
            if (!interests.isEmpty()) {
                List<String> matched = new ArrayList<>(spot.getTags());
                matched.retainAll(interests);
                double interestRatio = (double) matched.size() / interests.size();
                score += 35 * interestRatio;
                if (!matched.isEmpty()) {
                    reasons.add("匹配兴趣标签：" + String.join("、", matched));
                }
            }

            // 2. 评分口碑 20 分
            score += 20 * spot.getRating() / 5.0;
            if (spot.getRating() >= 4.8) {
                reasons.add("游客评分高达 " + trimRating(spot.getRating()) + " 分，口碑极佳");
            }

            // 3. 热度 15 分
            double popRatio = spot.getPopularity() * 1.0 / maxPopularity;
            score += 15 * popRatio;
            if (popRatio >= 0.9) {
                reasons.add("景区人气 TOP 热门打卡点");
            }

            // 4. 人群适配 10 分
            if (!crowd.isEmpty() && spot.getSuitableFor().contains(crowd)) {
                score += 10;
                reasons.add("特别适合" + crowd + "出行");
            }

            // 4.5 目的地偏好: 省份 +12, 同区域 +6
            if (!province.isEmpty() && province.equals(spot.getProvince())) {
                score += 12;
                reasons.add("正好位于你心仪的" + province);
            } else if (!region.isEmpty() && region.equals(spot.getRegion())) {
                score += 6;
                reasons.add("位于你想去的" + region + "地区");
            }

            // 5. 时长适配 8 分
            if (spot.getDurationMinutes() <= available) {
                score += 8;
                reasons.add("建议游玩约 " + spot.getDurationMinutes() + " 分钟，时间安排合适");
            } else {
                score += Math.max(0, 8 - (spot.getDurationMinutes() - available) / 30.0 * 4);
            }

            // 6. 预算适配 7 分
            if (spot.getPrice() == null || spot.getPrice().compareTo(budget) <= 0) {
                score += 7;
                if (spot.getPrice() != null && spot.getPrice().signum() == 0) {
                    reasons.add("无需另购门票，性价比高");
                } else {
                    reasons.add("票价 " + spot.getPrice() + " 元，在你的预算内");
                }
            } else {
                score -= 8;
            }

            int finalScore = (int) Math.round(Math.max(0, Math.min(100, score)));
            if (reasons.isEmpty()) {
                reasons.add("综合体验评价良好，值得顺路一游");
            }
            String level = finalScore >= 85 ? "极力推荐" : finalScore >= 70 ? "非常推荐" : finalScore >= 55 ? "推荐" : "可以一试";
            items.add(new RecommendItemVO(spot, finalScore, level, reasons));
        }

        items.sort(Comparator.comparingInt(RecommendItemVO::getScore).reversed());
        return items.stream().limit(6).toList();
    }

    private String trimRating(double rating) {
        return String.format("%.1f", rating);
    }
}
