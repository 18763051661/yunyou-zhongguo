package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.dto.Requests;
import cn.edu.qut.entity.User;
import cn.edu.qut.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Requests.RegisterRequest request, HttpSession session) {
        Map<String, Object> view = userService.register(request.username(), request.password(), request.nickname());
        User loginUser = userService.getById(((Number) view.get("id")).longValue());
        session.setAttribute("loginUser", loginUser);
        return Result.ok(view);
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Requests.LoginRequest request, HttpSession session) {
        User user = userService.login(request.username(), request.password());
        session.setAttribute("loginUser", user);
        return Result.ok(userService.toView(user));
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpSession session) {
        session.invalidate();
        return Result.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public Result<?> me(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return Result.ok(user == null ? null : userService.toView(user));
    }

    @PostMapping("/profile")
    public Result<?> profile(@RequestBody Requests.ProfileRequest request, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Result.fail(401, "请先登录");
        }
        Map<String, Object> view = userService.updateProfile(user.getId(), request.nickname(), request.preferenceTags());
        session.setAttribute("loginUser", userService.getById(user.getId()));
        return Result.ok(view);
    }
}
