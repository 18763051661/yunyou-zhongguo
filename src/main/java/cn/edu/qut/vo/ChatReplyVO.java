package cn.edu.qut.vo;

import java.util.List;

/**
 * AI 助教回复。
 * type: text / spot / route / recommend, 便于前端做快捷跳转。
 * engine: llm(真实大模型 API) / local(本地知识库兜底)。
 */
public class ChatReplyVO {

    private String answer;
    private List<String> suggestions;
    private String type;
    private Long spotId;
    private Long routeId;
    private String engine;

    public ChatReplyVO(String answer, List<String> suggestions, String type, Long spotId, Long routeId) {
        this(answer, suggestions, type, spotId, routeId, "local");
    }

    public ChatReplyVO(String answer, List<String> suggestions, String type, Long spotId, Long routeId, String engine) {
        this.answer = answer;
        this.suggestions = suggestions;
        this.type = type;
        this.spotId = spotId;
        this.routeId = routeId;
        this.engine = engine;
    }

    public String getAnswer() { return answer; }
    public List<String> getSuggestions() { return suggestions; }
    public String getType() { return type; }
    public Long getSpotId() { return spotId; }
    public Long getRouteId() { return routeId; }
    public String getEngine() { return engine; }
}
