package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.dto.Requests;
import cn.edu.qut.entity.User;
import cn.edu.qut.service.FavoriteService;
import cn.edu.qut.support.web.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public Result<?> list(HttpSession session) {
        User user = SessionUtils.requireUser(session);
        return Result.ok(favoriteService.list(user.getId()));
    }

    @PostMapping
    public Result<?> toggle(@RequestBody Requests.FavoriteRequest request, HttpSession session) {
        User user = SessionUtils.requireUser(session);
        return Result.ok(favoriteService.toggle(user.getId(), request.spotId()));
    }
}
