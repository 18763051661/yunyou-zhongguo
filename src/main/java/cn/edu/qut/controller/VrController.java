package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.service.VrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * VR 环景舱 REST 接口: 游览打卡 + 热度榜 + 足迹。
 */
@RestController
@RequestMapping("/api/vr")
public class VrController {

    private final VrService vrService;

    public VrController(VrService vrService) {
        this.vrService = vrService;
    }

    /** 进入 VR 全景时打卡: body {spotId, sessionId} → 返回该景点累计 VR 游览次数 */
    @PostMapping("/view")
    public Result<Map<String, Object>> view(@RequestBody Map<String, Object> body) {
        Long spotId = body.get("spotId") == null ? null : Long.valueOf(String.valueOf(body.get("spotId")));
        String sessionId = body.get("sessionId") == null ? null : String.valueOf(body.get("sessionId"));
        if (spotId == null) {
            return Result.fail(400, "spotId 不能为空");
        }
        long total = vrService.record(spotId, sessionId);
        return Result.ok(Map.of("spotId", spotId, "vrCount", total));
    }

    /** 某景点累计 VR 游览次数 */
    @GetMapping("/count/{spotId}")
    public Result<Map<String, Object>> count(@PathVariable("spotId") Long spotId) {
        return Result.ok(Map.of("spotId", spotId, "vrCount", vrService.count(spotId)));
    }

    /** VR 热度榜 */
    @GetMapping("/hot")
    public Result<Map<String, Object>> hot(@RequestParam(defaultValue = "6") int n) {
        return Result.ok(Map.of("list", vrService.hot(n)));
    }

    /** 某会话最近的 VR 游览足迹 */
    @GetMapping("/recent")
    public Result<Map<String, Object>> recent(@RequestParam String sessionId,
                                              @RequestParam(defaultValue = "8") int n) {
        return Result.ok(Map.of("list", vrService.recentBySession(sessionId, n)));
    }
}
