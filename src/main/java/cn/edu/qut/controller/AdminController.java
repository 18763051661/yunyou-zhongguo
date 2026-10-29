package cn.edu.qut.controller;

import cn.edu.qut.common.BusinessException;
import cn.edu.qut.common.Result;
import cn.edu.qut.dto.Requests;
import cn.edu.qut.entity.ScenicSpot;
import cn.edu.qut.service.OperationLogService;
import cn.edu.qut.service.OrderService;
import cn.edu.qut.service.SpotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端接口: 简单口令登录(教学演示用, 非生产级鉴权) + 景点管理 + 订单查看。
 * 管理端页面: /admin.html
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /** 演示账号(课设范围使用固定口令, 生产环境应接入角色表 + 加密存储) */
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final SpotService spotService;
    private final OrderService orderService;
    private final OperationLogService operationLogService;

    public AdminController(SpotService spotService, OrderService orderService,
                           OperationLogService operationLogService) {
        this.spotService = spotService;
        this.orderService = orderService;
        this.operationLogService = operationLogService;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Requests.AdminLoginRequest request, HttpSession session) {
        if (!ADMIN_USERNAME.equals(request.username()) || !ADMIN_PASSWORD.equals(request.password())) {
            throw new BusinessException(401, "管理员账号或密码错误");
        }
        session.setAttribute("adminUser", ADMIN_USERNAME);
        return Result.ok(Map.of("username", ADMIN_USERNAME));
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpSession session) {
        session.removeAttribute("adminUser");
        return Result.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public Result<?> me(HttpSession session) {
        Object admin = session.getAttribute("adminUser");
        return Result.ok(admin == null ? Map.of("login", false) : Map.of("login", true, "username", admin));
    }

    // ==================== 景点管理 ====================

    @GetMapping("/spots")
    public Result<?> spots(@RequestParam(name = "keyword", required = false) String keyword,
                           @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                           @RequestParam(name = "size", required = false, defaultValue = "10") int size,
                           HttpSession session) {
        requireAdmin(session);
        return Result.ok(spotService.adminPage(keyword, page, size));
    }

    @PostMapping("/spots")
    public Result<?> create(@RequestBody ScenicSpot spot, HttpSession session) {
        requireAdmin(session);
        return Result.ok(spotService.adminCreate(spot));
    }

    @PutMapping("/spots/{id}")
    public Result<?> update(@PathVariable("id") Long id, @RequestBody ScenicSpot spot, HttpSession session) {
        requireAdmin(session);
        spot.setId(id);
        return Result.ok(spotService.adminUpdate(spot));
    }

    @DeleteMapping("/spots/{id}")
    public Result<?> delete(@PathVariable("id") Long id, HttpSession session) {
        requireAdmin(session);
        spotService.adminDelete(id);
        return Result.ok(Map.of("ok", true));
    }

    // ==================== 订单查看 ====================

    @GetMapping("/orders")
    public Result<?> orders(@RequestParam(name = "keyword", required = false) String keyword,
                            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
                            HttpSession session) {
        requireAdmin(session);
        return Result.ok(orderService.adminPage(keyword, page, size));
    }

    // ==================== 操作日志(AOP 审计) ====================

    @GetMapping("/logs")
    public Result<?> logs(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                          @RequestParam(name = "size", required = false, defaultValue = "10") int size,
                          HttpSession session) {
        requireAdmin(session);
        return Result.ok(operationLogService.page(page, size));
    }

    private void requireAdmin(HttpSession session) {
        if (session.getAttribute("adminUser") == null) {
            throw new BusinessException(401, "请先登录管理后台");
        }
    }
}
