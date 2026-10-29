package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.dto.Requests;
import cn.edu.qut.entity.User;
import cn.edu.qut.service.TourService;
import cn.edu.qut.support.web.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tour")
public class TourController {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @PostMapping("/start")
    public Result<?> start(@RequestBody Requests.StartTourRequest request, HttpSession session) {
        User user = SessionUtils.currentUser(session);
        Long userId = user == null ? null : user.getId();
        return Result.ok(tourService.start(request.routeId(), request.sessionId(), userId));
    }

    @PostMapping("/visit")
    public Result<?> visit(@RequestBody Requests.TourVisitRequest request, HttpSession session) {
        User user = SessionUtils.currentUser(session);
        Long userId = user == null ? null : user.getId();
        return Result.ok(tourService.visit(request.sessionId(), request.routeId(), request.spotId(), userId));
    }

    @GetMapping("/progress")
    public Result<?> progress(@RequestParam("sessionId") String sessionId, @RequestParam("routeId") Long routeId) {
        return Result.ok(tourService.progress(sessionId, routeId));
    }

    @GetMapping("/records")
    public Result<?> records(@RequestParam(name = "sessionId", required = false) String sessionId) {
        return Result.ok(tourService.records(sessionId));
    }

    @GetMapping("/footprint")
    public Result<?> footprint(@RequestParam(name = "sessionId", required = false) String sessionId) {
        return Result.ok(tourService.footprint(sessionId));
    }
}
