package cn.edu.qut.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 通用 OpenAI 兼容大模型 HTTP 客户端。
 * 按 ai.properties 中 providers 顺序依次尝试(DeepSeek → 通义千问 …);
 * 任一供应商成功即返回; 全部失败返回 null, 由上层回退本地知识库, 保证对话永不报错。
 */
@Component
public class LlmClient implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private record Provider(String name, String base, String key, String model,
                            boolean noThinking, int timeoutMs) {}

    /** 一次成功的大模型回复: content 为文本, provider 为实际命中的供应商名称。 */
    public record Reply(String content, String provider) {}

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final List<Provider> providers = new ArrayList<>();
    private int timeoutMs = 12000;
    private int maxTokens = 700;
    private double temperature = 0.6;

    @Override
    public void afterPropertiesSet() {
        load();
    }

    void load() {
        Properties props = new Properties();
        try (InputStream in = new ClassPathResource("ai.properties").getInputStream()) {
            props.load(in);
        } catch (Exception e) {
            log.warn("未找到 ai.properties, AI 将仅使用本地知识库: {}", e.getMessage());
            return;
        }
        timeoutMs = parseInt(props.getProperty("ai.timeout-ms"), 12000);
        maxTokens = parseInt(props.getProperty("ai.max-tokens"), 700);
        temperature = parseDouble(props.getProperty("ai.temperature"), 0.6);
        String order = props.getProperty("ai.providers", "").trim();
        for (String name : order.split(",")) {
            String n = name.trim();
            if (n.isEmpty() || props.getProperty("ai." + n + ".base") == null) {
                continue;
            }
            String key = props.getProperty("ai." + n + ".key", "").trim();
            if (key.isEmpty()) {
                // 未配置 key 的供应商直接跳过, 避免每次对话都发起一次必然 401 的请求
                log.info("LLM[{}] 未配置 API Key, 暂不启用 (填入 ai.{}.key 即自动生效)", n, n);
                continue;
            }
            providers.add(new Provider(n,
                    props.getProperty("ai." + n + ".base").trim(),
                    key,
                    props.getProperty("ai." + n + ".model", "").trim(),
                    Boolean.parseBoolean(props.getProperty("ai." + n + ".no-thinking", "false").trim()),
                    parseInt(props.getProperty("ai." + n + ".timeout-ms"), timeoutMs)));
        }
        log.info("LLM 供应商顺序: {}", providers.stream().map(Provider::name).toList());
    }

    /**
     * 多轮对话。messages 为 {role, content} 列表。全部供应商失败返回 null。
     */
    public Reply chat(List<Map<String, String>> messages) {
        for (Provider p : providers) {
            String answer = invoke(p, messages);
            if (answer != null && !answer.isBlank()) {
                return new Reply(answer.trim(), p.name());
            }
        }
        return null;
    }

    public boolean available() {
        return !providers.isEmpty();
    }

    private String invoke(Provider p, List<Map<String, String>> messages) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", p.model());
            payload.put("messages", messages);
            payload.put("max_tokens", maxTokens);
            payload.put("temperature", temperature);
            payload.put("stream", false);
            // 火山方舟推理端点默认先输出思维链(reasoning_content), 实测慢 10 倍以上;
            // 显式关闭后该端点不再生成思维链, 延迟从约 29 秒降至约 2 秒。
            if (p.noThinking()) {
                payload.put("thinking", Map.of("type", "disabled"));
            }
            String body = mapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder(URI.create(p.base()))
                    .timeout(Duration.ofMillis(p.timeoutMs()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + p.key())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                log.warn("LLM[{}] HTTP {} : {}", p.name(), resp.statusCode(),
                        resp.body() == null ? "" : resp.body().substring(0, Math.min(200, resp.body().length())));
                return null;
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        } catch (Exception e) {
            log.warn("LLM[{}] 调用失败, 尝试下一个供应商: {}", p.name(), e.getMessage());
            return null;
        }
    }

    private int parseInt(String v, int def) {
        try { return v == null ? def : Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    private double parseDouble(String v, double def) {
        try { return v == null ? def : Double.parseDouble(v.trim()); } catch (Exception e) { return def; }
    }
}
