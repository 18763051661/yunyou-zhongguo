package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.service.RouteService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/routes")
    public Result<?> list() {
        return Result.ok(routeService.list());
    }

    @GetMapping("/routes/{id}")
    public Result<?> detail(@PathVariable("id") Long id) {
        return Result.ok(routeService.detail(id));
    }
}
