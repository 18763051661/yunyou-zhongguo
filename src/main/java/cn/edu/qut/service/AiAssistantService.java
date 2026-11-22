package cn.edu.qut.service;

import cn.edu.qut.vo.ChatReplyVO;

import java.util.List;

/**
 * AI 助教 Service
 */
public interface AiAssistantService {

    /**
     * 与 AI 助教对话: 优先调用真实大模型 API 并以平台数据做 RAG 增强,
     * 大模型不可用时回退本地结构化知识引擎, 服务永不中断。
     *
     * @param raw 用户输入
     * @return 回复内容(含建议、跳转类型、关联景点/路线 ID、来源标记)
     */
    ChatReplyVO chat(String raw);

    /**
     * 与 AI 助教对话, 可指定首选大模型供应商。
     *
     * @param raw      用户输入
     * @param provider 首选供应商名称(deepseek/qwen/doubao…), null 则按配置顺序自动故障转移
     * @return 回复内容
     */
    ChatReplyVO chat(String raw, String provider);

    /** 当前已配置可用的大模型供应商名称列表。 */
    List<String> providers();

    /**
     * 景点深度讲解: 基于平台数据(含周边景点、经纬度等)做 RAG 增强,
     * 让大模型产出结构化、有历史文化背景的丰富讲解; 不可用时回退本地结构化讲解。
     *
     * @param spotId 景点 ID
     * @return 景点讲解回复
     */
    ChatReplyVO explain(Long spotId);

    /**
     * AI 一句话游记: 以游客第一人称生成朋友圈风格的游作文案,
     * RAG 约束只写真实景点资料中的看点; 大模型不可用时回退本地模板。
     *
     * @param spotId 景点 ID
     * @return 游记文案
     */
    ChatReplyVO journal(Long spotId);
}
