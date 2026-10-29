package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.config.AppConfig;
import cn.edu.qut.dto.Requests;
import cn.edu.qut.entity.TicketOrder;
import cn.edu.qut.entity.User;
import cn.edu.qut.pay.AlipayPayService;
import cn.edu.qut.pay.WeChatPayService;
import cn.edu.qut.service.OrderService;
import cn.edu.qut.support.web.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AlipayPayService alipayPayService;
    private final WeChatPayService weChatPayService;
    private final AppConfig appConfig;

    public OrderController(OrderService orderService,
                           AlipayPayService alipayPayService,
                           WeChatPayService weChatPayService,
                           AppConfig appConfig) {
        this.orderService = orderService;
        this.alipayPayService = alipayPayService;
        this.weChatPayService = weChatPayService;
        this.appConfig = appConfig;
    }

    /** 创建门票订单 (需登录) */
    @PostMapping
    public Result<?> create(@RequestBody Requests.CreateOrderRequest req, HttpSession session) {
        User user = SessionUtils.requireUser(session);
        TicketOrder order = orderService.create(req, user.getId());
        return Result.ok(order);
    }

    /**
     * 发起支付 (双通道架构)。
     * <ul>
     *   <li>method=ALIPAY: 支付宝沙箱扫码支付, 返回二维码 URL (alipay.trade.wap.pay)</li>
     *   <li>method=WECHAT: 微信支付, 未配置时走 mock 状态机流转</li>
     *   <li>method=SANDBOX / 默认: 直接标记已支付 (沙箱 mock)</li>
     * </ul>
     */
    @PostMapping("/{orderNo}/pay")
    public Result<?> pay(@PathVariable("orderNo") String orderNo,
                         @RequestParam(name = "method", required = false, defaultValue = "SANDBOX") String method,
                         @RequestParam(name = "mockFail", required = false, defaultValue = "false") Boolean mockFail,
                         HttpServletRequest request) {
        String upper = method == null ? "SANDBOX" : method.toUpperCase();
        if ("ALIPAY".equals(upper)) {
            // 支付宝支付: 沙箱扫码受限于沙箱版 App, 演示走 mock 状态机
            // 真实沙箱调用代码见 AlipayPayServiceImpl.createPreQrCode (alipay.trade.wap.pay)
            TicketOrder order = orderService.byOrderNo(orderNo);
            if (order == null) return Result.fail("订单不存在: " + orderNo);
            if ("PAID".equals(order.getPayStatus())) return Result.ok(order);
            if ("CANCELED".equals(order.getPayStatus())) return Result.fail("订单已取消");
            return Result.ok(orderService.pay(orderNo, method, mockFail));
        } else if ("WECHAT".equals(upper)) {
            // 微信支付: 已配置则调 createOrder, 未配置则走 mock 状态机
            TicketOrder order = orderService.byOrderNo(orderNo);
            if (order == null) return Result.fail("订单不存在: " + orderNo);
            if ("PAID".equals(order.getPayStatus())) return Result.ok(order);
            if ("CANCELED".equals(order.getPayStatus())) return Result.fail("订单已取消");

            if (weChatPayService.isConfigured()) {
                String subject = order.getSpotName() + " ×" + order.getQuantity() + "张";
                Map<String, Object> wxOrder = weChatPayService.createOrder(
                        orderNo, order.getTotalAmount(), subject);
                Map<String, Object> data = new HashMap<>();
                data.put("wxOrder", wxOrder);
                data.put("orderNo", orderNo);
                return Result.ok(data);
            }
            // 未配置 → mock 状态机流转
            return Result.ok(orderService.pay(orderNo, method, mockFail));
        }
        // 沙箱 mock 支付
        return Result.ok(orderService.pay(orderNo, method, mockFail));
    }

    /** 从 HttpServletRequest 拼接完整 URL (避免硬编码校园网 IP) */
    private static String buildFullUrl(HttpServletRequest req, String path) {
        if (path == null || path.isBlank()) return "";
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        return scheme + "://" + host + (port != 80 && port != 443 ? ":" + port : "") + path;
    }

    /** 订单详情 */
    @GetMapping("/{orderNo}")
    public Result<?> get(@PathVariable("orderNo") String orderNo) {
        return Result.ok(orderService.byOrderNo(orderNo));
    }

    /** 我的订单 (已登录) */
    @GetMapping
    public Result<?> my(HttpSession session) {
        User user = SessionUtils.currentUser(session);
        if (user == null) return Result.ok(List.of());
        return Result.ok(orderService.myOrders(user.getId()));
    }

    /** 取消订单 (待支付直接取消; 已支付取消 = 沙箱退款) */
    @PostMapping("/{orderNo}/cancel")
    public Result<?> cancel(@PathVariable("orderNo") String orderNo) {
        return Result.ok(orderService.cancel(orderNo));
    }

    /** 删除订单记录 (仅已取消订单可物理删除) */
    @PostMapping("/{orderNo}/delete")
    public Result<?> delete(@PathVariable("orderNo") String orderNo) {
        orderService.delete(orderNo);
        return Result.ok();
    }

    /**
     * 主动查询支付宝订单状态并同步更新本地订单。
     * 前端 return_url 回跳后调用此接口确认支付结果。
     */
    @GetMapping("/{orderNo}/alipay-query")
    public Result<?> alipayQueryAndSync(@PathVariable("orderNo") String orderNo) {
        TicketOrder order = orderService.byOrderNo(orderNo);
        if (order == null) return Result.fail("订单不存在: " + orderNo);
        if ("PAID".equals(order.getPayStatus())) return Result.ok(order);

        String tradeStatus = alipayPayService.queryTradeStatus(orderNo);
        Map<String, Object> data = new HashMap<>();
        data.put("orderStatus", order.getPayStatus());
        data.put("alipayTradeStatus", tradeStatus);

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            TicketOrder paid = orderService.markPaidFromAlipay(orderNo, null);
            data.put("order", paid);
            data.put("syncResult", "PAID");
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            data.put("syncResult", "CLOSED");
        } else {
            // WAIT_BUYER_PAY 或 null (查询失败/无此交易)
            data.put("syncResult", "PENDING");
        }
        return Result.ok(data);
    }
}
