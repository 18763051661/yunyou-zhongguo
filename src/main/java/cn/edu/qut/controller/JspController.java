package cn.edu.qut.controller;

import cn.edu.qut.entity.TravelPlan;
import cn.edu.qut.entity.TravelPreference;
import cn.edu.qut.service.CommentService;
import cn.edu.qut.service.FavoriteService;
import cn.edu.qut.service.OrderService;
import cn.edu.qut.service.RouteService;
import cn.edu.qut.service.SpotService;
import cn.edu.qut.service.StatsService;
import cn.edu.qut.service.TourService;
import cn.edu.qut.service.TravelPlannerService;
import cn.edu.qut.vo.SpotVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * JSP 页面控制器 (传统 SSM 视图层, 演示 JSTL + EL 表达式)
 * 与 REST API 前后端分离方案并存, 路径前缀 /page 避免冲突
 */
@Controller
@RequestMapping("/page")
public class JspController {

    private final SpotService spotService;
    private final RouteService routeService;
    private final StatsService statsService;
    private final CommentService commentService;
    private final TourService tourService;
    private final TravelPlannerService plannerService;
    private final FavoriteService favoriteService;
    private final OrderService orderService;

    public JspController(SpotService spotService, RouteService routeService,
                         StatsService statsService, CommentService commentService,
                         TourService tourService, TravelPlannerService plannerService,
                         FavoriteService favoriteService, OrderService orderService) {
        this.spotService = spotService;
        this.routeService = routeService;
        this.statsService = statsService;
        this.commentService = commentService;
        this.tourService = tourService;
        this.plannerService = plannerService;
        this.favoriteService = favoriteService;
        this.orderService = orderService;
    }

    /** JSP 首页: 统计概览 + 热门景点 */
    @GetMapping("/home")
    public String home(Model model) {
        Map<String, Object> stats = statsService.overview();
        List<SpotVO> hotSpots = spotService.topPopular(12);
        model.addAttribute("stats", stats);
        model.addAttribute("hotSpots", hotSpots);
        return "home";
    }

    /** JSP 景点列表: 分页 + JSTL forEach */
    @GetMapping("/spots")
    public String spots(@RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "size", defaultValue = "12") int size,
                        Model model) {
        var result = spotService.page(null, null, null, null, null, null, null, "default", page, size, null);
        model.addAttribute("spots", result.getList());
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("total", result.getTotal());
        return "spotList";
    }

    /** JSP 景点详情: 单景点信息 + EL 表达式 */
    @GetMapping("/spots/{id}")
    public String spotDetail(@PathVariable("id") Long id, Model model) {
        SpotVO spot = spotService.detail(id, null);
        model.addAttribute("spot", spot);
        return "spotDetail";
    }

    /** JSP 路线列表 */
    @GetMapping("/routes")
    public String routes(Model model) {
        List<Map<String, Object>> routes = routeService.list();
        model.addAttribute("routes", routes);
        return "routeList";
    }

    /** JSP 路线详情: 逐站游览时间线(含 AI 讲解词) */
    @GetMapping("/routes/{id}")
    public String routeDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("route", routeService.detail(id));
        return "routeDetail";
    }

    /** JSP 数据统计: 平台概览 + 分布图表(JSTL 渲染) */
    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("stats", statsService.overview());
        return "stats";
    }

    /** JSP 景点点评: 评分分布 + 用户评论列表 + 分页(JSTL + EL + 条件渲染) */
    @GetMapping("/spots/{id}/comments")
    public String spotComments(@PathVariable("id") Long id,
                               @RequestParam(name = "page", defaultValue = "1") int page,
                               Model model) {
        SpotVO spot = spotService.detail(id, null);
        var comments = commentService.list(id, page, 8);
        var summary = commentService.spotRatingSummary(id);
        model.addAttribute("spot", spot);
        model.addAttribute("comments", comments);
        model.addAttribute("summary", summary);
        model.addAttribute("page", page);
        return "spotComments";
    }

    /** JSP 虚拟游览足迹: 打卡时间线 + 省份点亮 + 徽章成就 */
    @GetMapping("/tour/records")
    public String tourRecords(@RequestParam(name = "sessionId", required = false, defaultValue = "demo") String sessionId,
                              Model model) {
        List<Map<String, Object>> records = tourService.records(sessionId);
        Map<String, Object> footprint = tourService.footprint(sessionId);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("records", records);
        model.addAttribute("footprint", footprint);
        return "tourRecords";
    }

    /* ---------- 智能出游规划表单 ---------- */

    /** GET: 渲染智能规划问卷表单 */
    @GetMapping("/planner")
    public String plannerForm(Model model) {
        if (!model.containsAttribute("pref")) {
            TravelPreference pref = new TravelPreference();
            pref.setDays(3);
            model.addAttribute("pref", pref);
        }
        return "plannerForm";
    }

    /** POST: 提交问卷 → AI 生成 → 重定向到结果页 */
    @PostMapping("/planner/generate")
    public String plannerSubmit(@ModelAttribute TravelPreference pref, Model model) {
        TravelPlan plan = plannerService.generate(pref);
        return "redirect:/page/planner/" + plan.getId();
    }

    /** GET: 展示 AI 生成的路线推荐 */
    @GetMapping("/planner/{planId}")
    public String plannerResult(@PathVariable Long planId, Model model) {
        TravelPlan plan = plannerService.plan(planId);
        if (plan == null) return "redirect:/page/planner";
        model.addAttribute("plan", plan);
        model.addAttribute("recent", plannerService.recentPlans().stream().limit(5).toList());
        return "plannerResult";
    }

    /* ---------- 景点排行榜 (JSTL varStatus + c:choose 奖牌) ---------- */

    /** JSP 景点排行榜: 高评分 Top 10 + 热门 Top 10 */
    @GetMapping("/ranking")
    public String ranking(Model model) {
        model.addAttribute("topRating", spotService.topRating(10));
        model.addAttribute("topPopular", spotService.topPopular(10));
        return "ranking";
    }

    /* ---------- 全国省份维度导览 (嵌套 c:forEach) ---------- */

    /** JSP 省份维度导览: 31 省份景点聚合卡片 */
    @GetMapping("/provinces")
    public String provinces(Model model) {
        model.addAttribute("provinces", spotService.provinces());
        return "provinceGuide";
    }

    /* ---------- 收藏夹 (c:choose + empty 判断) ---------- */

    /** JSP 收藏夹: 演示用户收藏景点列表(userId=1 demo) */
    @GetMapping("/favorites")
    public String favorites(Model model) {
        model.addAttribute("favorites", favoriteService.list(1L));
        return "favorites";
    }

    /* ---------- 关于平台 (c:set + fmt:formatNumber) ---------- */

    /** JSP 关于平台: 项目介绍 + 技术栈 + 统计数据 */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("stats", statsService.overview());
        return "about";
    }

    /* ---------- 我的订单 (JSTL forEach + c:choose 状态徽章 + fmt:formatNumber 金额) ---------- */

    /** JSP 我的订单: 票根式卡片 + 消费统计 + AOP 审计日志展示 */
    @GetMapping("/orders")
    public String orders(@RequestParam(name = "userId", required = false, defaultValue = "1") Long userId,
                         Model model) {
        List<cn.edu.qut.entity.TicketOrder> orders = orderService.myOrders(userId);
        model.addAttribute("orders", orders);
        model.addAttribute("userId", userId);
        /* 聚合统计 */
        java.math.BigDecimal paidTotal = orders.stream()
            .filter(o -> "PAID".equals(o.getPayStatus()))
            .map(cn.edu.qut.entity.TicketOrder::getTotalAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long paidCount = orders.stream().filter(o -> "PAID".equals(o.getPayStatus())).count();
        long pendingCount = orders.stream().filter(o -> "PENDING".equals(o.getPayStatus())).count();
        long canceledCount = orders.stream().filter(o -> "CANCELED".equals(o.getPayStatus())).count();
        long ticketCount = orders.stream()
            .filter(o -> !"CANCELED".equals(o.getPayStatus()))
            .mapToLong(o -> o.getQuantity() == null ? 0 : o.getQuantity()).sum();
        model.addAttribute("paidTotal", paidTotal);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("canceledCount", canceledCount);
        model.addAttribute("ticketCount", ticketCount);
        return "orders";
    }
}
