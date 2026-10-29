package cn.edu.qut.support;

import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片服务: 每个景点都按其专属 prompt(景点名+所在地+景观类型)
 * 生成与真实景点一一对应的实景照片, 杜绝随机图库图。
 *
 * 图片来源: trae 文生图 API (text_to_image), 返回真实摄影质感的景点实景图:
 *   主图  = 景点实景 16:9
 *   画廊  = 全景远景 / 核心景观特写 / 黄昏氛围 三张不同角度
 */
@Service
public class ImageService {

    private static final String TRAE_BASE =
            "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image";

    /** 统一追加的真实摄影风格词, 保证出图是实景照片而非插画 */
    private static final String PHOTO_STYLE =
            "真实旅游实景照片,高清风光摄影,自然光,风景明信片质感,无水印无文字";

    /**
     * 生成文生图 URL。
     *
     * @param prompt 景点专属描述(景点名,所在地,景观类型)
     * @param size   图像尺寸 landscape_16_9 / landscape_4_3 等
     */
    public String image(String prompt, String size) {
        return buildUrl(prompt, null, size);
    }

    /** 内部统一构造: 规范化景点描述 + 场景视角词 + 尺寸 */
    private String buildUrl(String rawPrompt, String sceneSuffix, String size) {
        return TRAE_BASE + "?prompt=" + encode(photoPrompt(rawPrompt, sceneSuffix)) + "&image_size=" + size;
    }

    /** 景点主图: 真实实景 16:9 */
    public String mainImage(String prompt) {
        return image(prompt, "landscape_16_9");
    }

    /**
     * 带确定性 seed 的主图 URL。
     * seed 仅用于保证同一景点稳定, 出图内容仍由景点专属 prompt 决定。
     */
    public String mainImage(String prompt, String seed) {
        return mainImage(prompt);
    }

    /** 景点画廊: 同一景点的三张不同视角实景图 */
    public List<String> gallery(String prompt) {
        List<String> urls = new ArrayList<>(3);
        urls.add(buildUrl(prompt, "全景远景航拍视角", "landscape_16_9"));
        urls.add(buildUrl(prompt, "核心景观近距离特写细节", "landscape_4_3"));
        urls.add(buildUrl(prompt, "黄昏金色时刻灯光氛围", "landscape_16_9"));
        return urls;
    }

    /** 带 seed 的画廊(兼容旧签名, 内容与 {@link #gallery(String)} 一致) */
    public List<String> gallery(String prompt, String seed) {
        return gallery(prompt);
    }

    /**
     * 兜底画廊: 无具体景点 prompt 时使用通用中国山水实景图。
     */
    public List<String> galleryFallback(String seed) {
        List<String> urls = new ArrayList<>(3);
        urls.add(buildUrl("中国著名景区", "全景远景航拍视角", "landscape_16_9"));
        urls.add(buildUrl("中国著名景区", "核心景观近距离特写", "landscape_4_3"));
        urls.add(buildUrl("中国著名景区", "黄昏金色时刻氛围", "landscape_16_9"));
        return urls;
    }

    /**
     * 将数据库里的 image_prompt 规范化为"真实实景照片"描述。
     * 原始格式: "故宫博物院,北京北京,历史古迹,中国著名旅游景点,高清风光摄影,风景明信片质感"
     * 取前三段(景点名/所在地/景观类型), 去掉重复的通用尾巴, 统一追加摄影风格词。
     */
    private String photoPrompt(String raw, String sceneSuffix) {
        String base;
        if (raw == null || raw.isBlank()) {
            base = "中国山水景区";
        } else {
            String[] parts = raw.split("[,，]");
            StringBuilder sb = new StringBuilder(parts[0].trim());
            for (int i = 1; i < Math.min(parts.length, 3); i++) {
                String seg = parts[i].trim();
                if (!seg.isEmpty()) {
                    sb.append('，').append(seg);
                }
            }
            base = sb.toString();
        }
        StringBuilder p = new StringBuilder(base).append('，').append(PHOTO_STYLE);
        if (sceneSuffix != null && !sceneSuffix.isBlank()) {
            p.append('，').append(sceneSuffix);
        }
        return p.toString();
    }

    private String encode(String text) {
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }
}
