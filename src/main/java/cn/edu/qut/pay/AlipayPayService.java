package cn.edu.qut.pay;

import cn.edu.qut.entity.TicketOrder;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝沙箱支付服务。
 * <p>
 * 校园网无公网 HTTPS, 无法接收支付宝异步通知(notify_url),
 * 因此支付结果依赖前端 return_url 回跳后的主动查询 trade.query 确认。
 */
public interface AlipayPayService {

    /**
     * 检查支付宝配置是否已填写 (appId 和私钥非空)。
     *
     * @return true=已配置, false=未配置
     */
    boolean isConfigured();

    /**
     * 创建电脑网站支付订单, 返回支付表单 HTML。
     * 前端拿到 HTML 后自动提交 form 即可跳转支付宝沙箱收银台。
     *
     * @param orderNo      订单号
     * @param totalAmount  金额 (元)
     * @param subject      订单标题 (景点名 + 数量)
     * @param returnUrl    同步跳转地址
     * @return 支付宝支付表单 HTML
     */
    String createPagePay(String orderNo, BigDecimal totalAmount, String subject, String returnUrl);

    /**
     * 创建当面付预下单订单, 返回二维码内容 URL。
     * 前端用 qrcode.js 将此 URL 渲染成二维码图片, 手机支付宝 App 扫码付款。
     * <p>
     * 文档: alipay.trade.precreate (当面付 precreate API)
     *
     * @param orderNo      订单号
     * @param totalAmount  金额 (元)
     * @param subject      订单标题
     * @return 二维码内容 URL (qr_code 字段), null=失败
     */
    String createPreQrCode(String orderNo, BigDecimal totalAmount, String subject);

    /**
     * 主动查询支付宝订单状态。
     * <p>
     * 校园网无法接收异步通知, 前端 return_url 回跳后靠此方法确认支付结果。
     *
     * @param orderNo 订单号 (我方订单号)
     * @return 支付宝交易状态: TRADE_SUCCESS / TRADE_FINISHED / WAIT_BUYER_PAY / TRADE_CLOSED / null(查询失败)
     */
    String queryTradeStatus(String orderNo);

    /**
     * 验证支付宝异步通知签名。
     *
     * @param params 回调参数 (支付宝回调时传递的所有表单参数)
     * @return true=签名正确
     */
    boolean verifyNotifySign(Map<String, String> params);
}
