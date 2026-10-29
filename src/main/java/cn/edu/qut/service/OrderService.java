package cn.edu.qut.service;

import cn.edu.qut.common.PageResult;
import cn.edu.qut.dto.Requests.CreateOrderRequest;
import cn.edu.qut.entity.TicketOrder;

import java.util.List;

/**
 * 门票订单服务 (沙箱支付, 模拟微信/支付宝)。
 */
public interface OrderService {

    /**
     * 创建门票订单。
     *
     * @param req    下单请求
     * @param userId 当前用户 ID
     * @return 创建成功的订单
     */
    TicketOrder create(CreateOrderRequest req, Long userId);

    /**
     * 沙箱模拟支付 (100% 成功, 除非 mockFail=true)。
     *
     * @param orderNo  订单号
     * @param method   支付方式
     * @param mockFail 是否模拟支付失败
     * @return 支付后的订单
     */
    TicketOrder pay(String orderNo, String method, Boolean mockFail);

    /**
     * 查询当前用户的订单列表。
     *
     * @param userId 用户 ID
     * @return 订单列表
     */
    List<TicketOrder> myOrders(Long userId);

    /**
     * 根据订单号查询订单。
     *
     * @param orderNo 订单号
     * @return 订单
     */
    TicketOrder byOrderNo(String orderNo);

    /**
     * 取消订单 (已支付订单取消 = 沙箱退款, 状态机 PAID → CANCELED)。
     *
     * @param orderNo 订单号
     * @return 取消后的订单
     */
    TicketOrder cancel(String orderNo);

    /**
     * 删除订单记录 (仅 CANCELED 状态可物理删除)。
     *
     * @param orderNo 订单号
     * @return 删除行数
     */
    int delete(String orderNo);

    /**
     * 管理端订单分页(全部用户), 关键词匹配订单号/景点名/联系人。
     */
    PageResult<TicketOrder> adminPage(String keyword, Integer page, Integer size);

    /**
     * 支付宝回调/主动查询确认支付后, 标记订单为已支付。
     * 复用现有 @Transactional 状态机 PENDING → PAID。
     *
     * @param orderNo   订单号
     * @param tradeNo   支付宝交易号 (支付宝返回)
     * @return 已支付订单
     */
    TicketOrder markPaidFromAlipay(String orderNo, String tradeNo);
}
