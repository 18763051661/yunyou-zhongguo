package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.entity.TravelPlan;
import cn.edu.qut.entity.TravelPreference;
import cn.edu.qut.service.TravelPlannerService;
import cn.edu.qut.support.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能出游规划 REST API:
 * POST /api/planner/generate  提交偏好 → AI 生成路线
 * GET  /api/planner/{id}      查某条路线
 * GET  /api/planner/recent    最近 8 条
 * GET  /api/planner/llm-debug 诊断 LlmClient 状态
 */
@RestController
@RequestMapping("/api/planner")
public class TravelPlannerController {

    private final TravelPlannerService planner;
    private final LlmClient llmClient;
    private final ObjectMapper om = new ObjectMapper();

    public TravelPlannerController(TravelPlannerService planner, LlmClient llmClient) {
        this.planner = planner;
        this.llmClient = llmClient;
    }

    /** 诊断: 看 LlmClient 是不是正常可用 */
    @GetMapping("/llm-debug")
    public Result<Map<String, Object>> llmDebug() {
        var m = new java.util.LinkedHashMap<String, Object>();
        m.put("available", llmClient.available());
        // 尝试一次最简单的 AI 调用
        List<Map<String, String>> msgs = List.of(
                Map.of("role", "system", "content", "你是助手"),
                Map.of("role", "user", "content", "只回复OK两个字")
        );
        long t0 = System.currentTimeMillis();
        LlmClient.Reply reply = llmClient.chat(msgs);
        long t1 = System.currentTimeMillis();
        m.put("testTimeMs", t1 - t0);
        m.put("reply", reply == null ? null : reply.content());
        m.put("provider", reply == null ? null : reply.provider());
        return Result.ok(m);
    }

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody TravelPreference pref) {
        if (pref.getDays() == null || pref.getDays() < 1) {
            return Result.fail(400, "请填写计划天数");
        }
        TravelPlan plan = planner.generate(pref);
        return Result.ok(toMap(plan));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        TravelPlan p = planner.plan(id);
        if (p == null) return Result.fail(404, "路线不存在");
        return Result.ok(toMap(p));
    }

    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent() {
        List<TravelPlan> list = planner.recentPlans();
        var out = new java.util.ArrayList<Map<String, Object>>();
        for (TravelPlan p : list) out.add(toMap(p));
        return Result.ok(out);
    }

    /** 把 TravelPlan 里的 JSON 字符串解析后一起返回 */
    private Map<String, Object> toMap(TravelPlan p) {
        var m = new java.util.LinkedHashMap<String, Object>();
        m.put("id", p.getId());
        m.put("title", p.getTitle());
        m.put("summary", p.getSummary());
        m.put("days", p.getDays());
        m.put("provider", p.getProvider());
        m.put("itinerary", p.getItinerary());
        m.put("createdAt", p.getCreatedAt());
        try {
            List<Map<String, Object>> spots = om.readValue(
                    p.getSpotsJson() == null ? "[]" : p.getSpotsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
            m.put("spots", spots);
        } catch (Exception e) {
            m.put("spots", List.of());
        }
        return m;
    }
}
