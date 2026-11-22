package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.dto.Requests;
import cn.edu.qut.service.AiAssistantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    /**
     * AI 助教对话。
     * request.provider 可指定首选大模型(deepseek/qwen/doubao), 不填则按配置顺序自动故障转移。
     */
    @PostMapping("/chat")
    public Result<?> chat(@RequestBody(required = false) Requests.ChatRequest request) {
        String message = request == null ? "" : request.message();
        String provider = request == null ? null : request.provider();
        return Result.ok(aiAssistantService.chat(message, provider));
    }

    /**
     * 当前可用的大模型供应商列表(供前端模型切换器展示)。
     */
    @GetMapping("/providers")
    public Result<?> providers() {
        return Result.ok(aiAssistantService.providers());
    }

    /**
     * 景点深度讲解: 基于 RAG + 大模型生成结构化丰富讲解词, 供详情页朗读/展示。
     */
    @GetMapping("/explain")
    public Result<?> explain(@RequestParam(name = "spotId") Long spotId) {
        return Result.ok(aiAssistantService.explain(spotId));
    }

    /**
     * AI 一句话游记: 朋友圈风格文案, RAG 约束真实景点, 可一键复制分享。
     */
    @GetMapping("/journal")
    public Result<?> journal(@RequestParam(name = "spotId") Long spotId) {
        return Result.ok(aiAssistantService.journal(spotId));
    }
}
