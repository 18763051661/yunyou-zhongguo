package cn.edu.qut.service;

import cn.edu.qut.vo.SpotVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真 360° 全景素材实现。
 * 素材为真实航拍全景(每面单文件立方体 _f/_r/_b/_l/_u/_d 六面), 本地存放于 static/pano/ 下,
 * 按景点名称关键字匹配(白名单), 不做模糊猜配 —— 没有素材就如实返回 available:false。
 */
@Service
public class PanoServiceImpl implements PanoService {

    /** 名称关键字 → 本地全景目录 (素材来源: AirPano.com 实拍航拍全景, 免费用于学习演示)。 */
    private static final Map<String, String> KEYWORD_TO_DIR = new LinkedHashMap<>();

    static {
        KEYWORD_TO_DIR.put("八达岭长城", "great-wall");
        KEYWORD_TO_DIR.put("慕田峪长城", "great-wall");
        KEYWORD_TO_DIR.put("桂林漓江", "guilin");
        KEYWORD_TO_DIR.put("黄山", "huangshan");
        KEYWORD_TO_DIR.put("张家界", "zhangjiajie");
        KEYWORD_TO_DIR.put("九寨沟", "jiuzhaigou");
    }

    private final SpotService spotService;

    public PanoServiceImpl(SpotService spotService) {
        this.spotService = spotService;
    }

    @Override
    public Map<String, Object> findBySpot(Long spotId) {
        Map<String, Object> miss = new LinkedHashMap<>();
        miss.put("available", false);
        if (spotId == null) {
            return miss;
        }
        try {
            SpotVO spot = spotService.detail(spotId, null);
            if (spot == null || spot.getName() == null) {
                return miss;
            }
            for (Map.Entry<String, String> e : KEYWORD_TO_DIR.entrySet()) {
                if (spot.getName().contains(e.getKey())) {
                    String dir = e.getValue();
                    List<String> faces = new ArrayList<>();
                    for (String f : new String[]{"_f", "_r", "_b", "_l", "_u", "_d"}) {
                        faces.add("static/pano/" + dir + "/" + f + ".jpg");
                    }
                    Map<String, Object> hit = new LinkedHashMap<>();
                    hit.put("available", true);
                    hit.put("title", dirTitle(dir));
                    hit.put("source", "AirPano.com");
                    hit.put("faces", faces);
                    return hit;
                }
            }
        } catch (Exception ignore) {
            // 查询失败按无素材处理, 不影响 VR 其它模式
        }
        return miss;
    }

    @Override
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            for (SpotVO s : spotService.all()) {
                if (s.getName() == null) {
                    continue;
                }
                for (Map.Entry<String, String> e : KEYWORD_TO_DIR.entrySet()) {
                    if (s.getName().contains(e.getKey())) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("spotId", s.getId());
                        item.put("name", s.getName());
                        item.put("title", dirTitle(e.getValue()));
                        item.put("source", "AirPano.com");
                        result.add(item);
                        break;
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    private String dirTitle(String dir) {
        switch (dir) {
            case "great-wall": return "长城 · 航拍全景";
            case "guilin": return "桂林漓江 · 航拍全景";
            case "huangshan": return "黄山 · 航拍全景";
            case "zhangjiajie": return "张家界 · 航拍全景";
            case "jiuzhaigou": return "九寨沟 · 航拍全景";
            default: return "360° 全景";
        }
    }
}
