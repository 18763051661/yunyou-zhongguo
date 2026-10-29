package cn.edu.qut.pay;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付服务接口。
 * <p>
 * 与 AlipayPayService 平行的双通道支付架构。
 * 微信支付沙箱需真实商户号, 本接口定义完整契约, 实现层返回 mock 结果。
 * <p>
 * 文档: https://pay.weixin.qq.com/wiki/doc/apiv3/open/pay/chapter2_5_1.shtml (JSAPI 下单)
 */
public interface WeChatPayService {

    /**
     * 检查微信支付配置是否已填写 (mchId 和 API key 非空)。
     *
     * @return true=已配置, false=未配置
     */
    boolean isConfigured();

    /**
     * 统一下单 (Native 下单生成二维码 / JSAPI 下单生成 prepay_id)。
     *
     * @param orderNo     订单号
     * @param totalAmount 金额 (元)
     * @param subject     订单标题
     * @return 预支付信息 Map, 含 prepay_id / code_url (二维码) 等
     */
    Map<String, Object> createOrder(String orderNo, BigDecimal totalAmount, String subject);

    /**
     * 主动查询微信订单状态。
     *
     * @param orderNo 订单号 (我方订单号)
     * @return 微信交易状态: SUCCESS / REFUND / NOTPAY / CLOSED / REVOKED / null
     */
    String queryTradeStatus(String orderNo);

    /**
     * 验证微信支付回调签名。
     *
     * @param params 回调参数 (resource 解密后的 JSON)
     * @return true=签名正确
     */
    boolean verifyNotifySign(Map<String, String> params);
}
