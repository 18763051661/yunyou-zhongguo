package cn.edu.qut.controller;

import cn.edu.qut.service.OrderService;
import cn.edu.qut.pay.AlipayPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝回调控制器 (独立于 OrderController, 路径不拼 /api/orders 前缀)。
 * <p>
 * /api/alipay/return - 前端同步跳转 (GET, 浏览器跳回)
 * /api/alipay/notify - 后端异步通知 (POST, 校园网无法接收, 保留接口)
 */
@RestController
@RequestMapping("/api/alipay")
public class AlipayController {

    private final AlipayPayService alipayPayService;
    private final OrderService orderService;

    public AlipayController(AlipayPayService alipayPayService, OrderService orderService) {
        this.alipayPayService = alipayPayService;
        this.orderService = orderService;
    }

    /**
     * 前端同步跳转地址 (GET, 浏览器自动跳回)。
     * 校园网无法接收异步 notify, 前端拿 orderNo 后调 /api/orders/{orderNo}/alipay-query 主动确认。
     */
    @GetMapping("/return")
    public void alipayReturn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String orderNo = req.getParameter("out_trade_no");
        resp.setContentType("text/html;charset=UTF-8");
        if (orderNo == null || orderNo.isBlank()) {
            resp.getWriter().write(htmlBox("支付结果未知", "未获取到订单号, 请到'我的订单'查看",
                    "location.href='/?tab=orders'"));
            return;
        }
        resp.getWriter().write(htmlBox("支付结果确认中", "正在查询支付宝支付状态…",
                "fetch('/api/orders/" + orderNo + "/alipay-query').then(r=>r.json()).then(data=>{"
                        + "var box=document.getElementById('box');"
                        + "var tip=document.getElementById('tip');"
                        + "var pa=data.data&&data.data.syncResult;"
                        + "if(pa==='PAID'){box.className='box success';"
                        + "box.innerHTML='<div class=\"ico ok\">✓</div><h3>🎉 支付成功</h3><p>订单号: " + orderNo + "</p><p>2 秒后跳转…</p>';"
                        + "setTimeout(()=>location.href='/?tab=orders',2000);}"
                        + "else if(pa==='CLOSED'){box.className='box fail';"
                        + "box.innerHTML='<div class=\"ico x\">×</div><h3>订单已关闭</h3><p>可能未完成支付</p><p>2 秒后跳转…</p>';"
                        + "setTimeout(()=>location.href='/?tab=orders',2500);}"
                        + "else{box.className='box';"
                        + "box.innerHTML='<h3>待确认</h3><p>支付状态: '+(data.data&&data.data.alipayTradeStatus||'未知')+'</p><p>2 秒后跳转我的订单</p>';"
                        + "setTimeout(()=>location.href='/?tab=orders',2500);}"
                        + "}).catch(()=>{"
                        + "document.getElementById('tip').textContent='网络异常, 请手动查看我的订单';"
                        + "setTimeout(()=>location.href='/?tab=orders',2500);"
                        + "});"));
    }

    /**
     * 支付宝异步通知 (POST, form-urlencoded)。
     * 校园网无公网 HTTPS, 支付宝无法回调到本地, 此接口仅保留以便未来配置公网。
     */
    @PostMapping("/notify")
    public String alipayNotify(HttpServletRequest req) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> raw = req.getParameterMap();
        for (Map.Entry<String, String[]> e : raw.entrySet()) {
            String[] v = e.getValue();
            params.put(e.getKey(), v != null && v.length > 0 ? v[0] : "");
        }
        if (!alipayPayService.verifyNotifySign(params)) {
            return "fail";
        }
        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            try {
                orderService.markPaidFromAlipay(outTradeNo, tradeNo);
            } catch (Exception ignored) { /* 幂等 */ }
        }
        return "success";
    }

    // ========== 工具方法 ==========

    private static String htmlBox(String title, String tip, String onLoadScript) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<title>支付结果</title>"
                + "<style>body{font-family:'PingFang SC','Microsoft YaHei';display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;background:#f4f7f6}"
                + ".box{background:#fff;border-radius:14px;padding:32px 48px;box-shadow:0 6px 24px rgba(15,60,45,.08);text-align:center}"
                + ".ico{width:48px;height:48px;border-radius:50%;display:grid;place-items:center;font-size:28px;color:#fff;margin:0 auto 12px}"
                + ".ico.ok{background:linear-gradient(135deg,#16a34a,#15803d)}.ico.x{background:linear-gradient(135deg,#dc2626,#991b1b)}"
                + ".spinner{border:3px solid #e5e7eb;border-top-color:#0e9f6e;border-radius:50%;width:40px;height:40px;animation:spin 1s linear infinite;margin:0 auto 16px}"
                + "@keyframes spin{to{transform:rotate(360deg)}}"
                + "h3{margin:0 0 8px;color:#1f2937}p{color:#4b5563;margin:0}</style></head><body>"
                + "<div class='box' id='box'><div class='spinner'></div><h3>" + title + "</h3><p id='tip'>" + tip + "</p></div>"
                + "<script>" + onLoadScript + "</script></body></html>";
    }
}
