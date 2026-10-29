package cn.edu.qut.pay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付服务实现 (Mock 版)。
 * <p>
 * 微信支付沙箱需真实商户号 + API v3 密钥 + 证书, 课程项目无法完整接入。
 * 本实现返回模拟结果, 保持接口契约与真实微信支付一致,
 * 便于后续替换为真实 SDK 实现。
 * <p>
 * 双通道架构: AlipayPayService (支付宝) + WeChatPayService (微信)
 * 共享 OrderService.pay() 状态机流转 PENDING → PAID → CANCELED
 */
@Service
public class WeChatPayServiceImpl implements WeChatPayService {

    private static final Logger log = LoggerFactory.getLogger(WeChatPayServiceImpl.class);

    @Override
    public boolean isConfigured() {
        // 未配置真实商户号, 始终返回 false
        return false;
    }

    @Override
    public Map<String, Object> createOrder(String orderNo, BigDecimal totalAmount, String subject) {
        Map<String, Object> result = new HashMap<>();
        // 模拟微信 Native 下单返回的 code_url (实际应为 weixin://wxpay/bizpayurl?pr=xxx)
        result.put("code_url", "weixin://wxpay/bizpayurl?pr=MOCK_" + orderNo);
        result.put("prepay_id", "wx_mock_prepay_" + orderNo);
        result.put("out_trade_no", orderNo);
        result.put("mock", true);
        log.info("微信支付 Mock 下单: orderNo={}, amount={}, subject={}", orderNo, totalAmount, subject);
        return result;
    }

    @Override
    public String queryTradeStatus(String orderNo) {
        // Mock: 默认返回 WAIT_BUYER_PAY (等待买家付款)
        // 真实实现应调 https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/{orderNo}
        log.info("微信支付 Mock 查询: orderNo={}, status=WAIT_BUYER_PAY", orderNo);
        return "WAIT_BUYER_PAY";
    }

    @Override
    public boolean verifyNotifySign(Map<String, String> params) {
        // Mock: 签名验证默认通过
        return true;
    }
}
