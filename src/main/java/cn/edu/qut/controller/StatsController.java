package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        return Result.ok(statsService.overview());
    }
}
