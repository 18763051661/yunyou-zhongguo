package cn.edu.qut.service;

import cn.edu.qut.entity.TravelPlan;
import cn.edu.qut.entity.TravelPreference;
import cn.edu.qut.mapper.TravelPlanMapper;
import cn.edu.qut.mapper.TravelPreferenceMapper;
import cn.edu.qut.mapper.SpotMapper;
import cn.edu.qut.support.LlmClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能出游规划实现: 问卷偏好 → 真实景点数据 RAG 约束 → AI 生成 → 存库返回。
 * 三引擎故障转移: DeepSeek 主、通义/豆包备; 全挂时走纯规则兜底方案。
 */
@Service
public class TravelPlannerServiceImpl implements TravelPlannerService {

    private static final Logger log = LoggerFactory.getLogger(TravelPlannerServiceImpl.class);

    private final TravelPreferenceMapper prefMapper;
    private final TravelPlanMapper planMapper;
    private final SpotMapper spotMapper;
    private final LlmClient llm;
    private final ObjectMapper om = new ObjectMapper();

    public TravelPlannerServiceImpl(TravelPreferenceMapper prefMapper, TravelPlanMapper planMapper,
                                    SpotMapper spotMapper, LlmClient llm) {
        this.prefMapper = prefMapper;
        this.planMapper = planMapper;
        this.spotMapper = spotMapper;
        this.llm = llm;
    }

    @Override
    @Transactional
    public TravelPlan generate(TravelPreference pref) {
        // 1. 保存偏好
        prefMapper.insert(pref);

        // 2. 拉取真实景点数据(RAG 约束, AI 只能从这 219 个景点里挑)
        String ragHint = buildRagHint(pref);

        // 3. 尝试 AI 生成
        TravelPlan plan;
        if (llm.available()) {
            plan = tryAiGenerate(pref, ragHint);
        } else {
            plan = null;
        }

        // 4. AI 失败时走规则兜底, 保证永不报错
        if (plan == null) {
            plan = ruleFallback(pref);
        }
        plan.setPrefId(pref.getId());
        planMapper.insert(plan);
        return plan;
    }

    @Override
    public TravelPlan plan(Long id) { return planMapper.findById(id); }

    @Override
    public List<TravelPlan> recentPlans() { return planMapper.recent(); }

    @Override
    public TravelPlan planByPref(Long prefId) { return planMapper.findByPref(prefId); }

    /* ---------- 私有 ---------- */

    private String buildRagHint(TravelPreference pref) {
        // 只把字段名 + 用户选择塞给 AI, 让 AI 自己选景点名; 如果指定了省份就过滤, 否则全量
        StringBuilder sb = new StringBuilder();
        String provinces = pref.getProvinces();
        if (provinces != null && !provinces.isBlank()) {
            String[] arr = provinces.split(",");
            for (String p : arr) {
                List<Map<String, Object>> list = spotMapper.summaryByProvince(p.trim());
                if (list != null && !list.isEmpty()) {
                    for (Map<String, Object> m : list) {
                        sb.append("· ").append(m.get("name"))
                                .append("(").append(m.get("province"))
                                .append(", 门票").append(m.get("price"))
                                .append(", 评分").append(m.get("rating")).append(")\n");
                    }
                }
            }
        }
        // 如果没指定省份或指定省份没数据, 给 AI 一些各省热门景点参考
        if (sb.length() == 0) {
            List<Map<String, Object>> hot = spotMapper.topPopularBrief(30);
            for (Map<String, Object> m : hot) {
                sb.append("· ").append(m.get("name"))
                        .append("(").append(m.get("province"))
                        .append(", 门票").append(m.get("price"))
                        .append(", 评分").append(m.get("rating")).append(")\n");
            }
        }
        return sb.length() > 0 ? sb.toString() : "(暂无景点参考)";
    }

    private TravelPlan tryAiGenerate(TravelPreference pref, String ragHint) {
        String system = """
                你是一个资深旅行规划师。用户会填写一份出游偏好问卷, 你需要结合平台已有的真实景点数据,
                为用户推荐一条完整的个性化出游路线。

                必须严格遵守:
                1. 推荐景点必须来自下面给出的真实景点清单, 不允许编造数据库里没有的景点。
                2. 每个景点必须带原清单里的省份、门票价格、评分, 不允许改写这些字段值。
                3. 输出格式必须是下面的 JSON, 不要附加任何解释文字, 只输出 JSON 本身。
                4. 天数 = 用户指定的 days, 推荐景点数量约为 days * 1.2 左右。
                5. 如果用户指定了意向省份, 所有推荐景点必须来自该省份, 不要跨省推荐。

                输出 JSON 结构:
                {
                  "title": "一句话路线标题(15字以内, 含目的地)",
                  "summary": "一句话推荐理由",
                  "days": 3,
                  "spots": [
                    {"day": 1, "name": "景点中文全名", "province": "省份", "price": 50, "rating": 4.8, "stay_hours": 3}
                  ],
                  "itinerary": "逐日行程 Markdown, 每天一段, 含交通/看点/餐饮贴士"
                }
                """;

        String user = """
                我的出游偏好:
                - 计划天数: %s 天
                - 意向省份: %s
                - 偏好类型: %s
                - 预算: %s
                - 同行: %s
                - 交通方式偏好: %s
                - 特殊关注点: %s

                平台可选真实景点(从中挑选):
                %s
                """.formatted(
                pref.getDays(),
                blankIf(pref.getProvinces(), "不限"),
                blankIf(pref.getCategories(), "不限"),
                blankIf(pref.getBudget(), "不限"),
                blankIf(pref.getCompanions(), "不限"),
                blankIf(pref.getTransport(), "不限"),
                blankIf(pref.getFocus(), "无"),
                ragHint
        );

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        );

        LlmClient.Reply reply = llm.chat(messages);
        log.warn("【PLANNER-DEBUG】 reply={} provider={}", reply == null ? "null" : reply.content(), reply == null ? "null" : reply.provider());
        if (reply == null) return null;

        // 尝试把回复 parse 成 JSON 再转 Plan
        try {
            String json = reply.content();
            // 去掉可能的 ```json ... ``` 包裹
            json = json.replaceAll("^(?s)```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
            log.warn("【PLANNER-DEBUG】 cleaned json 前200字: {}", json.length() > 200 ? json.substring(0, 200) : json);
            Map<String, Object> obj = om.readValue(json, new TypeReference<>() {});
            TravelPlan plan = new TravelPlan();
            plan.setTitle(stringOf(obj.get("title"), "我的 AI 路线"));
            plan.setSummary(stringOf(obj.get("summary"), ""));
            plan.setDays(intOf(obj.get("days"), pref.getDays()));
            plan.setProvider(reply.provider());
            plan.setItinerary(stringOf(obj.get("itinerary"), ""));
            List<Map<String, Object>> spotsRaw = castList(obj.get("spots"));
            String spotsJson = om.writeValueAsString(spotsRaw);
            plan.setSpotsJson(spotsJson);
            return plan;
        } catch (Exception e) {
            log.warn("AI 回复解析失败: {}，改用规则兜底", e.getMessage());
            return null;
        }
    }

    /** 规则兜底: 不依赖 AI, 从偏好匹配几个景点硬拼一条路线。永不为空。 */
    private TravelPlan ruleFallback(TravelPreference pref) {
        TravelPlan plan = new TravelPlan();
        plan.setProvider("local");
        plan.setDays(pref.getDays() == null ? 3 : pref.getDays());
        plan.setTitle("我的本地规则路线(" + plan.getDays() + " 天)");
        plan.setSummary("AI 服务暂不可用, 由规则引擎自动匹配生成");

        // 按省份挑 5 个热门景点
        List<Map<String, Object>> picks = new ArrayList<>();
        String provinces = pref.getProvinces();
        if (provinces != null && !provinces.isBlank()) {
            for (String p : provinces.split(",")) {
                List<Map<String, Object>> l = spotMapper.summaryByProvince(p.trim());
                if (l != null && !l.isEmpty()) picks.addAll(l.subList(0, Math.min(2, l.size())));
            }
        }
        if (picks.size() < 3) {
            List<Map<String, Object>> hot = spotMapper.topPopularBrief(20);
            for (Map<String, Object> m : hot) {
                if (picks.size() >= pref.getDays() * 2) break;
                picks.add(m);
            }
        }

        int days = plan.getDays();
        StringBuilder itin = new StringBuilder();
        List<Map<String, Object>> spotList = new ArrayList<>();
        int perDay = Math.max(1, picks.size() / days);
        for (int d = 1; d <= days; d++) {
            itin.append("### 第 ").append(d).append(" 天\n");
            for (int i = 0; i < perDay && !picks.isEmpty(); i++) {
                Map<String, Object> spot = picks.remove(0);
                String name = String.valueOf(spot.get("name"));
                itin.append("- ").append(name).append(" (").append(spot.get("province"))
                        .append(", 评分 ").append(spot.get("rating")).append(")\n");
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("day", d);
                e.put("name", name);
                e.put("province", spot.get("province"));
                e.put("price", spot.get("price"));
                e.put("rating", spot.get("rating"));
                e.put("stay_hours", 3);
                spotList.add(e);
            }
        }
        plan.setItinerary(itin.toString());
        try { plan.setSpotsJson(om.writeValueAsString(spotList)); } catch (Exception e) { plan.setSpotsJson("[]"); }
        return plan;
    }

    /* ---------- 工具 ---------- */

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object o) {
        if (!(o instanceof List)) return new ArrayList<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : (List<?>) o) {
            if (item instanceof Map) out.add((Map<String, Object>) item);
        }
        return out;
    }

    private String blankIf(String s, String def) { return (s == null || s.isBlank()) ? def : s; }
    private String stringOf(Object o, String def) { return o == null ? def : String.valueOf(o); }
    private int intOf(Object o, int def) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }
}
