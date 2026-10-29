package cn.edu.qut.pay;

import cn.edu.qut.config.AppConfig;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.internal.util.AlipaySignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝沙箱支付实现。
 * <p>
 * 文档: https://opendocs.alipay.com/open/270/105898 (电脑网站支付 API)
 * <p>
 * 校园网无公网 HTTPS, notifyUrl 留空, 支付结果依赖前端 return_url 回跳后
 * 调用 alipay.trade.query 主动查询确认。
 */
@Service
public class AlipayPayServiceImpl implements AlipayPayService {

    private static final Logger log = LoggerFactory.getLogger(AlipayPayServiceImpl.class);

    private final AppConfig appConfig;

    public AlipayPayServiceImpl(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 懒加载 AlipayClient, 首次调用时创建。
     * 如果配置未填写, 直接返回 null。
     */
    private AlipayClient client() {
        if (!isConfigured()) return null;
        try {
            AlipayConfig cfg = new AlipayConfig();
            cfg.setAppId(appConfig.alipayAppId.trim());
            cfg.setPrivateKey(appConfig.alipayAppPrivateKey.trim());
            cfg.setAlipayPublicKey(appConfig.alipayPublicKey.trim());
            cfg.setServerUrl(appConfig.alipayGatewayUrl.trim());
            cfg.setSignType(appConfig.alipaySignType);
            cfg.setCharset(appConfig.alipayCharset);
            cfg.setFormat(appConfig.alipayFormat);
            return new DefaultAlipayClient(cfg);
        } catch (AlipayApiException e) {
            log.error("初始化支付宝 Client 失败", e);
            return null;
        }
    }

    @Override
    public boolean isConfigured() {
        return appConfig.alipayAppId != null
                && !appConfig.alipayAppId.isBlank()
                && !appConfig.alipayAppId.startsWith("请替换")
                && appConfig.alipayAppPrivateKey != null
                && !appConfig.alipayAppPrivateKey.isBlank()
                && !appConfig.alipayAppPrivateKey.startsWith("请替换")
                && appConfig.alipayPublicKey != null
                && !appConfig.alipayPublicKey.isBlank()
                && !appConfig.alipayPublicKey.startsWith("请替换");
    }

    @Override
    public String createPagePay(String orderNo, BigDecimal totalAmount, String subject, String returnUrl) {
        AlipayClient client = client();
        if (client == null) {
            throw new IllegalStateException("支付宝沙箱未配置, 请先填写 alipay.properties");
        }
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setReturnUrl(returnUrl);
            // notifyUrl 留空: 校园网无公网 HTTPS, 无法接收异步通知
            request.setNotifyUrl(appConfig.alipayNotifyUrl);

            // 业务参数
            request.setBizContent("{"
                    + "\"out_trade_no\":\"" + orderNo + "\","
                    + "\"total_amount\":\"" + totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP) + "\","
                    + "\"subject\":\"" + subject.replace("\"", "'") + "\","
                    + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\","
                    + "\"timeout_express\":\"30m\""
                    + "}");

            // 必须用 pageExecute() 获取 HTML 表单, exec() 无法获取页面跳转表单
            AlipayTradePagePayResponse response = client.pageExecute(request);
            if (response.isSuccess()) {
                return response.getBody(); // HTML 表单, 前端自动提交即可跳转
            } else {
                log.error("支付宝下单失败: code={}, msg={}, subCode={}, subMsg={}",
                        response.getCode(), response.getMsg(),
                        response.getSubCode(), response.getSubMsg());
                throw new RuntimeException("支付宝下单失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝下单异常", e);
            throw new RuntimeException("支付宝下单异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String createPreQrCode(String orderNo, BigDecimal totalAmount, String subject) {
        AlipayClient client = client();
        if (client == null) {
            throw new IllegalStateException("支付宝沙箱未配置, 请先填写 alipay.properties");
        }
        try {
            // 用 wap.pay (手机网站支付) 代替 precreate (当面付)
            // 原因: 沙箱 precreate 生成的二维码需要沙箱版支付宝 App 才能扫, 普通支付宝 App 扫会显示"失效"
            // wap.pay 生成 H5 收银台 URL, 手机浏览器扫码即可打开, 无需沙箱版支付宝 App
            com.alipay.api.request.AlipayTradeWapPayRequest request =
                    new com.alipay.api.request.AlipayTradeWapPayRequest();
            request.setBizContent("{"
                    + "\"out_trade_no\":\"" + orderNo + "\","
                    + "\"total_amount\":\"" + totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP) + "\","
                    + "\"subject\":\"" + subject.replace("\"", "'") + "\","
                    + "\"product_code\":\"QUICK_WAP_WAY\","
                    + "\"timeout_express\":\"30m\""
                    + "}");
            // pageExecute 返回自动提交的表单 HTML, 从中提取 action URL 作为二维码内容
            com.alipay.api.response.AlipayTradeWapPayResponse response = client.pageExecute(request);
            if (response.isSuccess()) {
                String body = response.getBody(); // HTML 表单
                // 从 form 的 action 属性提取完整 URL (含签名参数), 手机浏览器扫码可直接打开
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("action=\"([^\"]+)\"").matcher(body);
                if (m.find()) {
                    return m.group(1).replace("&amp;", "&");
                }
                // 兜底: 直接返回 body (前端会 try 失败)
                throw new RuntimeException("未能从支付表单提取 URL");
            } else {
                log.error("支付宝 wap.pay 下单失败: code={}, msg={}, subCode={}, subMsg={}",
                        response.getCode(), response.getMsg(),
                        response.getSubCode(), response.getSubMsg());
                throw new RuntimeException("支付宝下单失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝 wap.pay 下单异常", e);
            throw new RuntimeException("支付宝下单异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String queryTradeStatus(String orderNo) {
        AlipayClient client = client();
        if (client == null) return null;
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            AlipayTradeQueryResponse response = client.execute(request);
            if (response.isSuccess()) {
                return response.getTradeStatus(); // TRADE_SUCCESS / TRADE_FINISHED / WAIT_BUYER_PAY / TRADE_CLOSED
            } else {
                log.warn("支付宝查询订单失败: out_trade_no={}, code={}, subMsg={}",
                        orderNo, response.getCode(), response.getSubMsg());
                return null;
            }
        } catch (AlipayApiException e) {
            log.error("支付宝查询订单异常: out_trade_no={}", orderNo, e);
            return null;
        }
    }

    @Override
    public boolean verifyNotifySign(Map<String, String> params) {
        if (!isConfigured() || params == null) return false;
        try {
            // rsaCheckV2: 验签支付宝 V2 协议通知 (RSA2 自动适配)
            return AlipaySignature.rsaCheckV2(
                    params,
                    appConfig.alipayCharset,
                    appConfig.alipayPublicKey);
        } catch (AlipayApiException e) {
            log.error("支付宝通知验签失败", e);
            return false;
        }
    }
}
