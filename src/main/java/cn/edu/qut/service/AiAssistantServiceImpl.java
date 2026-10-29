package cn.edu.qut.service;

import cn.edu.qut.entity.Category;
import cn.edu.qut.support.LlmClient;
import cn.edu.qut.vo.ChatReplyVO;
import cn.edu.qut.vo.SpotVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 助教: 优先调用真实大模型 API(DeepSeek/通义千问, 自动故障转移),
 * 并以全国景点知识库做 RAG 增强, 保证回答基于平台真实数据;
 * 大模型不可用时无缝回退本地结构化知识引擎, 服务永不中断。
 */
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);

    private final SpotService spotService;
    private final CategoryService categoryService;
    private final RouteService routeService;
    private final LlmClient llmClient;

    private volatile List<SpotVO> spotCache;

    public AiAssistantServiceImpl(SpotService spotService, CategoryService categoryService,
                                  RouteService routeService, LlmClient llmClient) {
        this.spotService = spotService;
        this.categoryService = categoryService;
        this.routeService = routeService;
        this.llmClient = llmClient;
    }

    private List<SpotVO> spots() {
        if (spotCache == null) {
            spotCache = spotService.all();
        }
        return spotCache;
    }

    @Override
    public ChatReplyVO chat(String raw) {
        String msg = raw == null ? "" : raw.trim();
        // 先计算结构化结果(用于快捷建议、景点/路线跳转与兜底)
        ChatReplyVO local = localChat(msg);

        if (msg.isEmpty() || !llmClient.available()) {
            return local;
        }
        try {
            LlmClient.Reply llmReply = callLlm(msg);
            if (llmReply != null && !llmReply.content().isBlank()) {
                return new ChatReplyVO(llmReply.content(), local.getSuggestions(), local.getType(),
                        local.getSpotId(), local.getRouteId(), llmReply.provider());
            }
        } catch (Exception e) {
            log.warn("大模型对话失败, 使用本地知识库: {}", e.getMessage());
        }
        return local;
    }

    /**
     * 景点深度讲解: 基于平台数据(含周边景点、经纬度等)做 RAG 增强,
     * 让大模型产出结构化、有历史文化背景的丰富讲解; 不可用时回退本地结构化讲解。
     */
    @Override
    public ChatReplyVO explain(Long spotId) {
        SpotVO spot = spots().stream().filter(s -> s.getId().equals(spotId)).findFirst().orElse(null);
        if (spot == null) {
            return new ChatReplyVO("未找到该景点，请返回列表重新选择。",
                    List.of("推荐一条经典路线", "全国热门景点"), "text", null, null);
        }
        if (!llmClient.available()) {
            return localExplain(spot);
        }
        try {
            LlmClient.Reply reply = callLlmExplain(spot);
            if (reply != null && !reply.content().isBlank()) {
                return new ChatReplyVO(reply.content(),
                        List.of(spot.getName() + "门票多少钱",
                                "附近还有什么好玩的",
                                "推荐一条含" + spot.getName() + "的路线"),
                        "spot", spot.getId(), null, reply.provider());
            }
        } catch (Exception e) {
            log.warn("大模型深度讲解失败, 使用本地讲解: {}", e.getMessage());
        }
        return localExplain(spot);
    }

    /**
     * AI 一句话游记: 朋友圈风格文案。RAG 只注入真实景点资料并强约束不编造;
     * 全引擎不可用时走本地模板兜底(按景点 ID 确定性取模板, 同一景点文案稳定)。
     */
    @Override
    public ChatReplyVO journal(Long spotId) {
        SpotVO spot = spots().stream().filter(s -> s.getId().equals(spotId)).findFirst().orElse(null);
        if (spot == null) {
            return new ChatReplyVO("未找到该景点，请返回列表重新选择。",
                    List.of("推荐一条经典路线", "全国热门景点"), "text", null, null);
        }
        if (llmClient.available()) {
            try {
                StringBuilder kb = new StringBuilder();
                kb.append("景点名: ").append(spot.getName())
                        .append("\n位置: ").append(spot.getProvince()).append(spot.getCity())
                        .append("\n类别: ").append(spot.getCategoryName())
                        .append("\n核心看点: ").append(String.join("、", spot.getHighlights()))
                        .append("\n标签: ").append(String.join("、", spot.getTags()))
                        .append("\n简介: ").append(spot.getSummary());
                String system = "你是旅行文案写手。请基于给定的【真实景点资料】, 以游客第一人称写一句朋友圈游记文案。"
                        + "硬性要求: 全文 40-60 字、一句话(可带 1-2 个 emoji); 必须提到资料中至少 1 个真实看点或细节, "
                        + "严禁编造资料里没有的特色; 口语化、有画面感; 只输出正文, 不要引号、标题或解释。";
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", system));
                messages.add(Map.of("role", "user", "content",
                        "【真实景点资料】\n" + kb + "\n\n请为「" + spot.getName() + "」写一句游记。"));
                LlmClient.Reply reply = llmClient.chat(messages);
                if (reply != null && !reply.content().isBlank()) {
                    return new ChatReplyVO(reply.content(), List.of(), "journal", spot.getId(), null, reply.provider());
                }
            } catch (Exception e) {
                log.warn("AI 游记生成失败, 使用本地模板: {}", e.getMessage());
            }
        }
        return localJournal(spot);
    }

    /** 本地一句话游记兜底: 模板按景点 ID 确定性轮换, 同一景点文案稳定不跳变。 */
    private ChatReplyVO localJournal(SpotVO s) {
        String hl = s.getHighlights().isEmpty() ? "湖光山色" : s.getHighlights().get(0);
        String[] tpl = {
                "在" + s.getProvince() + s.getCity() + "遇见「" + s.getName() + "」，最难忘的是" + hl + "，值得专程来一趟 📍",
                "打卡" + s.getName() + " ✅ " + hl + "比想象中更震撼，镜头根本装不下它的美。",
                "把" + s.getName() + "写进旅行清单吧 ✍️ 站在" + hl + "前的那一刻，突然懂了什么叫不虚此行。"
        };
        String text = tpl[(int) (Math.abs(s.getId()) % tpl.length)];
        return new ChatReplyVO(text, List.of(), "journal", s.getId(), null, "local");
    }

    private LlmClient.Reply callLlmExplain(SpotVO spot) {
        StringBuilder kb = new StringBuilder();
        kb.append("当前景点(平台数据库, 以此为准):\n")
                .append("名称: ").append(spot.getName()).append("\n")
                .append("位置: ").append(spot.getProvince()).append(" ").append(spot.getCity())
                .append(" ").append(spot.getAddress()).append("\n")
                .append("经纬度: 东经 ").append(String.format("%.4f", spot.getLng()))
                .append("°, 北纬 ").append(String.format("%.4f", spot.getLat())).append("°\n")
                .append("类别: ").append(spot.getCategoryName()).append("\n")
                .append("门票: ").append(priceText(spot)).append("\n")
                .append("开放时间: ").append(spot.getOpenTime()).append("\n")
                .append("建议游玩: ").append(spot.getDurationMinutes()).append(" 分钟, 强度")
                .append(difficultyText(spot.getDifficulty())).append("\n")
                .append("评分: ").append(String.format("%.1f", spot.getRating()))
                .append(" (").append(spot.getRatingCount()).append(" 人评分)\n")
                .append("简介: ").append(spot.getSummary()).append("\n")
                .append("详细介绍: ").append(spot.getDescription()).append("\n")
                .append("核心看点: ").append(String.join("、", spot.getHighlights())).append("\n")
                .append("标签: ").append(String.join("、", spot.getTags())).append("\n")
                .append("适合人群: ").append(String.join("、", spot.getSuitableFor())).append("\n")
                .append("贴士: ").append(spot.getTips()).append("\n");

        // 注入周边景点, 便于讲解"周边联动"
        try {
            List<Map<String, Object>> nearby = spotService.nearby(spot.getLng(), spot.getLat(), 7);
            List<Map<String, Object>> others = nearby.stream()
                    .filter(x -> {
                        Object ns = x.get("spot");
                        return ns instanceof SpotVO s2 && !s2.getId().equals(spot.getId());
                    })
                    .limit(5).toList();
            if (!others.isEmpty()) {
                kb.append("\n周边景点(直线距离, 可顺带游览):\n");
                for (Map<String, Object> n : others) {
                    SpotVO s2 = (SpotVO) n.get("spot");
                    kb.append("- ").append(s2.getName()).append("（")
                            .append(s2.getProvince()).append(s2.getCity()).append("，约 ")
                            .append(n.get("distanceKm")).append(" km，")
                            .append(priceText(s2)).append("，")
                            .append(String.format("%.1f", s2.getRating())).append(" 分）\n");
                }
            }
        } catch (Exception ignore) {
            // 周边查询失败不影响讲解
        }

        String system = "你是「云游中国」全国景点智能导览系统的金牌讲解员, 名字叫小云, 讲解风格亲切生动、有故事感。"
                + "请基于【平台数据库】中的事实为游客生成一段精炼生动的讲解词, 供语音朗读。"
                + "严格按以下 4 段分节, 每段 1-3 句、有实质内容:\n"
                + "1) 🏛️ 景点概况: 一句话开场+位置、类别与价值\n"
                + "2) 📜 历史文化: 讲 1 个真实历史事件或传说典故\n"
                + "3) 🌟 核心看点: 解读数据库中最重要的看点, 说清它为什么特别\n"
                + "4) 🕐 游览贴士: 开放时间、门票、建议时长与 1 条实用提醒\n"
                + "硬性要求: 全文 300-380 字(为保证 2 秒内生成, 不要超字数), 口语化; "
                + "门票价格/开放时间严格用数据库不编造; 历史故事要真实可考; 段间空行; "
                + "用 emoji 标注小标题; 不要 Markdown 代码块或 # 标题。\n\n【平台数据库】\n" + kb;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        messages.add(Map.of("role", "user", "content",
                "请为「" + spot.getName() + "」生成一段深度讲解词。"));
        return llmClient.chat(messages);
    }

    /** 本地结构化深度讲解(大模型不可用时的兜底, 内容同样丰富)。 */
    private ChatReplyVO localExplain(SpotVO s) {
        StringBuilder sb = new StringBuilder();
        sb.append("🏛️ 景点概况\n").append(s.getName()).append("位于")
                .append(s.getProvince()).append(s.getCity()).append("，")
                .append(s.getSummary()).append("\n\n");
        sb.append("🌟 核心看点\n");
        if (s.getHighlights().isEmpty()) {
            sb.append("自然与人文景观交相辉映，值得细细品味。\n");
        } else {
            for (int i = 0; i < s.getHighlights().size(); i++) {
                sb.append(i + 1).append(". ").append(s.getHighlights().get(i)).append("\n");
            }
        }
        sb.append("\n📖 详细介绍\n").append(s.getDescription()).append("\n\n");
        sb.append("🕐 游览建议\n");
        sb.append("开放时间：").append(s.getOpenTime()).append("；建议游玩 ")
                .append(s.getDurationMinutes()).append(" 分钟，体力强度")
                .append(difficultyText(s.getDifficulty())).append("。\n");
        sb.append("门票：").append(priceText(s)).append("；适合人群：")
                .append(s.getSuitableFor().isEmpty() ? "大众游客" : String.join("、", s.getSuitableFor()))
                .append("。\n\n");
        sb.append("💡 实用小贴士\n").append(s.getTips() == null || s.getTips().isBlank()
                        ? "请遵守景区指引，注意安全，错峰出行体验更佳。" : s.getTips()).append("\n");
        // 周边景点
        try {
            List<Map<String, Object>> nearby = spotService.nearby(s.getLng(), s.getLat(), 7);
            List<Map<String, Object>> others = nearby.stream()
                    .filter(x -> {
                        Object ns = x.get("spot");
                        return ns instanceof SpotVO s2 && !s2.getId().equals(s.getId());
                    })
                    .limit(3).toList();
            if (!others.isEmpty()) {
                sb.append("\n📍 周边可顺带游览\n");
                for (Map<String, Object> n : others) {
                    SpotVO s2 = (SpotVO) n.get("spot");
                    sb.append("- ").append(s2.getName()).append("（约 ")
                            .append(n.get("distanceKm")).append(" km）\n");
                }
            }
        } catch (Exception ignore) {
        }
        return new ChatReplyVO(sb.toString(),
                List.of(s.getName() + "门票多少钱", "附近还有什么好玩的", "推荐一条经典路线"),
                "spot", s.getId(), null);
    }

    // ------------------------------------------------------------------
    // 真实大模型 + RAG
    // ------------------------------------------------------------------

    private LlmClient.Reply callLlm(String msg) {
        SpotVO spot = detectSpot(msg);
        StringBuilder kb = new StringBuilder();
        kb.append("平台数据: 共收录 ").append(spots().size()).append(" 个全国景点, 覆盖 ")
                .append(spots().stream().map(SpotVO::getProvince).distinct().count())
                .append(" 个省级行政区, ").append(routeService.list().size()).append(" 条主题虚拟游览路线。\n");

        if (spot != null) {
            kb.append("用户可能问到的景点(来自平台数据库, 以此为准):\n")
                    .append("名称: ").append(spot.getName()).append("\n")
                    .append("位置: ").append(spot.getProvince()).append(" ").append(spot.getCity()).append("\n")
                    .append("类别: ").append(spot.getCategoryName()).append("\n")
                    .append("门票: ").append(priceText(spot)).append("\n")
                    .append("开放时间: ").append(spot.getOpenTime()).append("\n")
                    .append("建议游玩: ").append(spot.getDurationMinutes()).append(" 分钟\n")
                    .append("评分: ").append(String.format("%.1f", spot.getRating()))
                    .append(" (").append(spot.getRatingCount()).append(" 人评分)\n")
                    .append("简介: ").append(spot.getSummary()).append("\n")
                    .append("介绍: ").append(spot.getDescription()).append("\n")
                    .append("看点: ").append(String.join("、", spot.getHighlights())).append("\n")
                    .append("贴士: ").append(spot.getTips()).append("\n");
        } else {
            kb.append("当前热门景点(名称|省份|评分|门票):\n");
            spotService.topPopular(8).forEach(s -> kb.append("- ").append(s.getName())
                    .append("|").append(s.getProvince()).append("|")
                    .append(String.format("%.1f", s.getRating())).append("|").append(priceText(s)).append("\n"));
        }

        String system = "你是「云游中国」全国景点智能导览系统的AI助教, 名字叫小云。"
                + "你的职责: 景点讲解、门票与开放时间查询、行程路线规划、个性化推荐、交通/美食/住宿/贴士等旅行咨询。"
                + "要求: 1) 优先使用下面【平台数据库】中的事实数据, 不要编造门票价格、开放时间、经纬度; "
                + "2) 数据库没有的信息(如实时天气、余票、当前票价浮动)要如实说明并建议以官方为准; "
                + "3) 回答亲切、条理清晰, 300字以内, 可用序号与emoji, 让讲解生动有画面感; "
                + "4) 介绍景点时适当融入历史文化背景或趣味小知识, 让讲解更丰富; "
                + "5) 用户问的景点不在库中时, 简要介绍常识并推荐平台内相似景点; "
                + "6) 回答末尾可附 1-2 条实用小贴士。\n\n【平台数据库】\n" + kb;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        messages.add(Map.of("role", "user", "content", msg));
        return llmClient.chat(messages);
    }

    // ------------------------------------------------------------------
    // 本地结构化知识引擎(兜底 + 意图结构)
    // ------------------------------------------------------------------

    private ChatReplyVO localChat(String msg) {
        if (msg.isEmpty()) {
            return greeting();
        }
        if (contains(msg, "你好", "您好", "hi", "hello", "在吗", "助教", "小云")) {
            return greeting();
        }
        if (contains(msg, "再见", "拜拜", "bye")) {
            return new ChatReplyVO("祝你旅途愉快、一路平安！别忘了在“我的”页面查看你的全国足迹地图，再见～",
                    List.of("再推荐几个景点", "有什么热门景点"), "text", null, null);
        }
        if (contains(msg, "谢谢", "感谢", "多谢")) {
            return new ChatReplyVO("不客气！能帮你规划旅程我很开心，还有其他问题随时问我～",
                    List.of("推荐一条经典路线", "现在哪里好玩"), "text", null, null);
        }

        SpotVO spot = detectSpot(msg);
        boolean askingRoute = contains(msg, "路线", "攻略", "行程", "安排", "怎么走", "游线");
        boolean askingPrice = contains(msg, "门票", "票价", "多少钱", "收费", "价格", "贵不贵", "费用");
        boolean askingTime = contains(msg, "几点", "开放", "营业", "开门", "关门", "时间");
        boolean askingFood = contains(msg, "吃", "美食", "小吃", "餐厅", "饭店", "特色菜", "好吃");
        boolean askingStay = contains(msg, "住宿", "酒店", "住哪", "民宿", "过夜", "住一");
        boolean askingTraffic = contains(msg, "交通", "停车", "怎么去", "地铁", "公交", "坐车", "自驾", "高铁", "机场");
        boolean askingTips = contains(msg, "注意", "准备", "带什么", "提醒", "穿什么", "必备");
        boolean askingWeather = contains(msg, "天气", "下雨", "温度", "热不热", "冷不冷");

        if (spot != null) {
            List<String> facts = new ArrayList<>();
            if (askingPrice) {
                facts.add(priceFact(spot));
            }
            if (askingTime) {
                facts.add("开放时间 " + spot.getOpenTime() + "，建议游玩 "
                        + spot.getDurationMinutes() + " 分钟");
            }
            if (askingTips) {
                facts.add("小贴士：" + (spot.getTips() == null || spot.getTips().isBlank()
                        ? "遵守景区指引，注意安全" : spot.getTips()));
            }
            if (askingTraffic || (contains(msg, "怎么走") && !askingRoute)) {
                facts.add("位于" + spot.getAddress() + "，可导航前往，节假日建议公共交通出行");
            }
            if (!facts.isEmpty()) {
                return new ChatReplyVO(spot.getName() + "（" + spot.getProvince() + spot.getCity()
                                + "）" + String.join("；", facts) + "。",
                        List.of("介绍一下" + spot.getName(), spot.getName() + "门票多少钱", "附近还有什么好玩的"),
                        "spot", spot.getId(), null);
            }
            return detailOf(spot);
        }

        if (askingRoute) {
            return routeAnswer();
        }
        if (askingFood) {
            return foodAnswer();
        }
        if (askingStay) {
            return stayAnswer();
        }
        if (askingTraffic) {
            return trafficAnswer();
        }
        if (askingTips) {
            return globalTips();
        }
        if (askingWeather) {
            return new ChatReplyVO("中国幅员辽阔，同一季节南北温差很大：出发前请查看目的地城市的实时天气预报。"
                            + "山区景点昼夜温差大需备外套，海岛注意台风预警，东北冬季要做好防寒。",
                    List.of("推荐适合现在玩的景点", "有什么热门景点"), "text", null, null);
        }
        if (contains(msg, "门票", "票价", "大门票")) {
            return globalTicket();
        }
        if (askingTime) {
            return new ChatReplyVO("全国景点开放时间各不相同，大多数景区为 08:00-17:30，"
                            + "主题乐园通常 09:30-21:00，古镇街区多为全天开放。直接问我“某某景点几点开门”即可查到具体时间。",
                    List.of("故宫几点开门", "推荐一条经典路线"), "text", null, null);
        }

        List<SpotVO> filtered = filterByMessage(msg);
        if (!filtered.isEmpty() || contains(msg, "推荐", "好玩", "值得", "去哪", "必去", "适合", "热门")) {
            return recommend(filtered, msg);
        }

        return new ChatReplyVO(
                "我是「云游中国」AI 助教小云，已收录全国 " + spots().size() + " 个景点和 "
                        + routeService.list().size() + " 条主题路线。你可以这样问我：\n"
                        + "· 介绍一下故宫博物院\n· 去黄山门票多少钱、几点开门\n· 带孩子去北京玩什么\n· 推荐一条丝绸之路路线\n· 三亚有什么好玩的",
                List.of("推荐一条经典路线", "全国热门景点", "带孩子适合去哪玩", "九寨沟介绍"),
                "text", null, null);
    }

    private ChatReplyVO greeting() {
        return new ChatReplyVO(
                "你好，我是「云游中国」AI 助教小云！我能为你讲解全国 " + spots().size()
                        + " 个景点（覆盖 31 个省级行政区）、规划 " + routeService.list().size()
                        + " 条主题虚拟游览路线。想去哪里，直接问我吧～",
                List.of("推荐一条经典路线", "全国热门景点", "带孩子适合去哪玩", "九寨沟介绍"),
                "text", null, null);
    }

    private ChatReplyVO detailOf(SpotVO s) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(s.getName()).append("】").append(s.getProvince()).append(s.getCity()).append("\n")
                .append(s.getSummary()).append("\n\n").append(s.getDescription()).append("\n\n");
        if (!s.getHighlights().isEmpty()) {
            sb.append("看点：").append(String.join("、", s.getHighlights())).append("。\n");
        }
        sb.append("门票：").append(priceText(s)).append("；开放：").append(s.getOpenTime())
                .append("；建议游玩 ").append(s.getDurationMinutes()).append(" 分钟，强度")
                .append(difficultyText(s.getDifficulty())).append("。");
        return new ChatReplyVO(sb.toString(),
                List.of(s.getName() + "门票多少钱", s.getName() + "有什么要注意", "推荐附近的景点"),
                "spot", s.getId(), null);
    }

    private String priceFact(SpotVO s) {
        return s.getPrice() != null && s.getPrice().signum() == 0
                ? "免门票"
                : "门票 " + priceNumber(s) + " 元/人";
    }

    private ChatReplyVO globalTicket() {
        long freeCount = spots().stream().filter(s -> s.getPrice() == null || s.getPrice().signum() == 0).count();
        List<SpotVO> paid = spots().stream()
                .filter(s -> s.getPrice() != null && s.getPrice().signum() > 0)
                .sorted(Comparator.comparing(SpotVO::getPrice).reversed())
                .limit(5).toList();
        StringBuilder sb = new StringBuilder();
        sb.append("平台上有 ").append(freeCount).append(" 个景点免门票（如西湖、外滩、夫子庙等）。\n");
        sb.append("票价较高的代表景点：");
        for (int i = 0; i < paid.size(); i++) {
            SpotVO s = paid.get(i);
            sb.append(s.getName()).append(" ").append(priceNumber(s)).append(" 元");
            if (i < paid.size() - 1) {
                sb.append("、");
            }
        }
        sb.append("。\n具体景点票价可直接问我，学生/老人常有优惠，建议线上提前购票。");
        return new ChatReplyVO(sb.toString(),
                List.of("推荐免费又好玩的景点", "上海迪士尼多少钱", "推荐一条经典路线"), "recommend", null, null);
    }

    private ChatReplyVO routeAnswer() {
        List<Map<String, Object>> routes = routeService.list();
        StringBuilder sb = new StringBuilder("为你准备了 ").append(routes.size()).append(" 条覆盖全国的主题路线：\n");
        for (int i = 0; i < Math.min(8, routes.size()); i++) {
            Map<String, Object> r = routes.get(i);
            sb.append("· ").append(r.get("name")).append("：").append(r.get("subtitle")).append("\n");
        }
        sb.append("\n在“在线游览”页面选择路线即可开启图文+AI语音的虚拟游览并逐站打卡。");
        return new ChatReplyVO(sb.toString(),
                List.of("介绍丝绸之路西北七日游", "亲子游怎么安排", "云南怎么玩"),
                "route", null, routes.isEmpty() ? null : (Long) routes.get(0).get("id"));
    }

    private ChatReplyVO foodAnswer() {
        return new ChatReplyVO(
                "中华美食因地而异：北京尝烤鸭炸酱面，西安吃肉夹馍泡馍，成都逛锦里啃火锅，重庆小面与江湖菜，"
                        + "广州饮早茶吃烧腊，长沙臭豆腐小龙虾，云南过桥米线汽锅鸡，新疆大盘鸡烤包子。"
                        + "告诉我你要去的城市，我可以结合当地景点给你推荐美食路线。",
                List.of("西安有什么好玩的", "成都三日游怎么安排", "推荐一条经典路线"),
                "text", null, null);
    }

    private ChatReplyVO stayAnswer() {
        return new ChatReplyVO(
                "住宿建议：① 城市游优先选地铁沿线，出行效率高；② 古镇游住一晚景区内民宿，夜景与清晨最值；"
                        + "③ 海岛度假选亚龙湾、蜈支洲岛周边度假酒店；④ 草原山区可体验毡房、星空帐篷。"
                        + "节假日房源紧张，建议提前 3-7 天预订。",
                List.of("三亚度假怎么安排", "丽江古城介绍", "新疆怎么玩"),
                "text", null, null);
    }

    private ChatReplyVO trafficAnswer() {
        return new ChatReplyVO(
                "跨城旅行优先高铁，东部省份高铁网密集；西北、新疆、西藏景点间距大，建议包车或自驾。"
                        + "每个景点详情页都有地址，可直接导航；热门景区节假日建议停在外围停车场乘接驳车入园。",
                List.of("丝绸之路西北七日游", "新疆环线八日游", "景区几点开门"),
                "text", null, null);
    }

    private ChatReplyVO globalTips() {
        return new ChatReplyVO(
                "出行小贴士：① 带好身份证，多数景区刷证入园；② 山区与高原备薄外套、防滑鞋，高海拔地区避免剧烈运动；"
                        + "③ 海岛做好防晒，水上项目穿救生衣；④ 旺季提前预约购票，热门项目一早先冲；"
                        + "⑤ 尊重宗教场所与少数民族习俗。",
                List.of("推荐一条经典路线", "去西藏要注意什么", "黄山介绍"),
                "text", null, null);
    }

    private ChatReplyVO recommend(List<SpotVO> preFiltered, String msg) {
        List<SpotVO> candidates = preFiltered.isEmpty() ? spots() : preFiltered;
        if (contains(msg, "孩子", "亲子", "遛娃", "小朋友", "宝宝")) {
            candidates = filterCrowd(candidates, "亲子");
        } else if (contains(msg, "老人", "爸妈", "长辈", "父母")) {
            candidates = filterCrowd(candidates, "老人");
        } else if (contains(msg, "情侣", "约会", "女朋友", "男朋友", "浪漫")) {
            candidates = filterCrowd(candidates, "情侣");
        }

        List<SpotVO> top = candidates.stream()
                .sorted(Comparator.comparingDouble(SpotVO::getRating).reversed())
                .limit(5).toList();
        if (top.isEmpty()) {
            return new ChatReplyVO("暂时没有完全匹配的景点，看看全国热门推荐？",
                    List.of("全国热门景点", "推荐一条经典路线"), "recommend", null, null);
        }
        StringBuilder sb = new StringBuilder("根据你的需求，我推荐这几个全国景点：\n");
        for (int i = 0; i < top.size(); i++) {
            SpotVO s = top.get(i);
            sb.append(i + 1).append(". ").append(s.getName())
                    .append("（").append(s.getProvince()).append(" · ")
                    .append(String.format("%.1f", s.getRating())).append(" 分）")
                    .append(s.getSummary()).append("\n");
        }
        sb.append("\n点开景点可看图文详情，也可以在“在线游览”里边听 AI 语音讲解边打卡。");
        return new ChatReplyVO(sb.toString(),
                List.of(top.get(0).getName() + "介绍", "推荐一条经典路线", "附近还有什么好玩的"),
                "recommend", top.get(0).getId(), null);
    }

    // ------------------------------------------------------------------
    private SpotVO detectSpot(String msg) {
        SpotVO best = null;
        int bestLen = 0;
        for (SpotVO s : spots()) {
            if (msg.contains(s.getName()) && s.getName().length() > bestLen) {
                best = s;
                bestLen = s.getName().length();
            }
            if (s.getAlias() != null) {
                for (String alias : s.getAlias().split("[,，]")) {
                    String token = alias.trim();
                    if (token.length() >= 2 && msg.contains(token) && token.length() > bestLen) {
                        best = s;
                        bestLen = token.length();
                    }
                }
            }
        }
        return best;
    }

    private List<SpotVO> filterByMessage(String msg) {
        Set<SpotVO> result = new LinkedHashSet<>();
        for (Category category : categoryService.findAll()) {
            if (msg.contains(category.getName())) {
                spots().stream().filter(s -> s.getCategoryId().equals(category.getId())).forEach(result::add);
            }
        }
        // 省份/城市
        for (SpotVO s : spots()) {
            if ((s.getProvince() != null && s.getProvince().length() >= 2 && msg.contains(s.getProvince()))
                    || (s.getCity() != null && s.getCity().length() >= 2 && msg.contains(s.getCity()))) {
                result.add(s);
            }
        }
        for (SpotVO s : spots()) {
            for (String tag : s.getTags()) {
                if (tag.length() >= 2 && msg.contains(tag)) {
                    result.add(s);
                }
            }
        }
        if (contains(msg, "免费", "免票")) {
            spots().stream().filter(s -> s.getPrice() == null || s.getPrice().signum() == 0).forEach(result::add);
        }
        if (contains(msg, "刺激", "挑战", "胆大")) {
            spots().stream().filter(s -> s.getTags().contains("刺激")).forEach(result::add);
        }
        return new ArrayList<>(result);
    }

    private List<SpotVO> filterCrowd(List<SpotVO> source, String crowd) {
        List<SpotVO> list = source.stream().filter(s -> s.getSuitableFor().contains(crowd)).toList();
        return list.isEmpty() ? source : list;
    }

    private boolean contains(String msg, String... keys) {
        String lower = msg.toLowerCase();
        for (String key : keys) {
            if (lower.contains(key.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String priceText(SpotVO s) {
        return s.getPrice() != null && s.getPrice().signum() == 0 ? "免票" : priceNumber(s) + " 元";
    }

    private String priceNumber(SpotVO s) {
        return s.getPrice() == null ? "0" : s.getPrice().stripTrailingZeros().toPlainString();
    }

    private String difficultyText(int difficulty) {
        switch (difficulty) {
            case 1: return "轻松";
            case 2: return "较轻松";
            case 3: return "中等";
            case 4: return "较费力";
            default: return "有挑战";
        }
    }
}
