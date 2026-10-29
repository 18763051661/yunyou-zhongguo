package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.dto.Requests;
import cn.edu.qut.service.RecommendService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    /**
     * 智能推荐: 提交兴趣/时长/预算/人群/体力, 返回带理由的排序结果。
     */
    @PostMapping("/recommend")
    public Result<?> recommend(@RequestBody(required = false) Requests.RecommendRequest request) {
        Requests.RecommendRequest body = request == null
                ? new Requests.RecommendRequest(null, null, null, null, null, null, null)
                : request;
        return Result.ok(recommendService.recommend(body));
    }
}
