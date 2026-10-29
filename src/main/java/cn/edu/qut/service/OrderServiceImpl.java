package cn.edu.qut.service;

import cn.edu.qut.common.PageResult;
import cn.edu.qut.dao.SpotDao;
import cn.edu.qut.dao.TicketOrderDao;
import cn.edu.qut.dto.Requests.CreateOrderRequest;
import cn.edu.qut.entity.ScenicSpot;
import cn.edu.qut.entity.TicketOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 门票订单服务 (沙箱支付, 模拟微信/支付宝)。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final TicketOrderDao orderDao;
    private final SpotDao spotDao;

    public OrderServiceImpl(TicketOrderDao orderDao, SpotDao spotDao) {
        this.orderDao = orderDao;
        this.spotDao = spotDao;
    }

    /** 创建门票订单 */
    @Override
    @Transactional
    public TicketOrder create(CreateOrderRequest req, Long userId) {
        if (req == null || req.spotId() == null || req.quantity() == null || req.quantity() < 1) {
            throw new IllegalArgumentException("景点ID和购票数量必填, 数量至少 1 张");
        }
        ScenicSpot spot = spotDao.findById(req.spotId());
        if (spot == null) {
            throw new IllegalArgumentException("景点不存在, ID=" + req.spotId());
        }
        if (spot.getPrice() == null || spot.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("该景点免费开放, 无需购票");
        }

        TicketOrder o = new TicketOrder();
        o.setOrderNo(generateOrderNo());
        o.setUserId(userId);
        o.setSpotId(spot.getId());
        o.setSpotName(spot.getName());
        o.setQuantity(req.quantity());
        o.setUnitPrice(spot.getPrice());
        o.setTotalAmount(spot.getPrice().multiply(BigDecimal.valueOf(req.quantity())));
        o.setContactName(req.contactName() == null ? "" : req.contactName());
        o.setContactPhone(req.contactPhone() == null ? "" : req.contactPhone());
        o.setVisitDate(req.visitDate() == null ? "" : req.visitDate());
        o.setRemark(req.remark() == null ? "" : req.remark());
        o.setPayStatus("PENDING");
        o.setPayMethod("SANDBOX");
        orderDao.insert(o);
        return orderDao.findByOrderNo(o.getOrderNo());
    }

    /** 沙箱模拟支付 (100% 成功, 除非 mockFail=true) */
    @Override
    public TicketOrder pay(String orderNo, String method, Boolean mockFail) {
        TicketOrder o = orderDao.findByOrderNo(orderNo);
        if (o == null) {
            throw new IllegalArgumentException("订单不存在: " + orderNo);
        }
        if ("PAID".equals(o.getPayStatus())) {
            return o; // 幂等
        }
        if ("CANCELED".equals(o.getPayStatus())) {
            throw new IllegalStateException("订单已取消, 无法支付");
        }
        // 沙箱 mock 失败: 模拟支付网关返回错误码
        if (Boolean.TRUE.equals(mockFail)) {
            String upper = method == null ? "" : method.toUpperCase();
            String reason;
            if ("ALIPAY".equals(upper)) {
                reason = "支付宝网关返回: 付款码已过期, 请重新出示付款码";
            } else if ("WECHAT".equals(upper)) {
                reason = "微信支付返回: 银行卡余额不足, 请更换支付方式";
            } else {
                reason = "支付网关超时, 未收到支付结果通知";
            }
            throw new RuntimeException("支付失败: " + reason);
        }
        String pm = (method == null || method.isBlank()) ? "SANDBOX" : method.toUpperCase();
        orderDao.markPaid(orderNo, "PAID", pm);
        return orderDao.findByOrderNo(orderNo);
    }

    @Override
    public List<TicketOrder> myOrders(Long userId) {
        return orderDao.findByUser(userId);
    }

    @Override
    public TicketOrder byOrderNo(String orderNo) {
        return orderDao.findByOrderNo(orderNo);
    }

    /** 取消订单: PENDING 直接取消; PAID 视为沙箱退款 (状态机 PAID → CANCELED, 原路退回) */
    @Override
    public TicketOrder cancel(String orderNo) {
        TicketOrder o = orderDao.findByOrderNo(orderNo);
        if (o == null) throw new IllegalArgumentException("订单不存在: " + orderNo);
        if ("CANCELED".equals(o.getPayStatus())) return o; // 幂等
        orderDao.cancel(orderNo);
        return orderDao.findByOrderNo(orderNo);
    }

    /** 删除订单记录: 仅 CANCELED 可物理删除, SQL 层再带状态条件白名单兜底 */
    @Override
    public int delete(String orderNo) {
        TicketOrder o = orderDao.findByOrderNo(orderNo);
        if (o == null) throw new IllegalArgumentException("订单不存在: " + orderNo);
        if ("PENDING".equals(o.getPayStatus())) throw new IllegalStateException("待支付订单请先取消后再删除");
        if ("PAID".equals(o.getPayStatus())) throw new IllegalStateException("已支付订单请先申请退款后再删除");
        int rows = orderDao.deleteByOrderNo(orderNo);
        if (rows == 0) throw new IllegalStateException("仅已取消的订单可删除");
        return rows;
    }

    private String generateOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int r = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "SN" + ts + r;
    }

    // ==================== 管理端 ====================

    @Override
    public PageResult<TicketOrder> adminPage(String keyword, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        long total = orderDao.countAdminPage(kw);
        List<TicketOrder> list = orderDao.findAdminPage(kw, (pageNo - 1) * pageSize, pageSize);
        return new PageResult<>(list, total, pageNo, pageSize);
    }

    /** 支付宝回调/主动查询确认支付: 状态机 PENDING → PAID, 幂等 */
    @Override
    @Transactional
    public TicketOrder markPaidFromAlipay(String orderNo, String tradeNo) {
        TicketOrder o = orderDao.findByOrderNo(orderNo);
        if (o == null) throw new IllegalArgumentException("订单不存在: " + orderNo);
        if ("PAID".equals(o.getPayStatus())) return o; // 幂等: 已支付直接返回
        if ("CANCELED".equals(o.getPayStatus())) throw new IllegalStateException("订单已取消, 无法支付");
        orderDao.markPaid(orderNo, "PAID", "ALIPAY");
        return orderDao.findByOrderNo(orderNo);
    }
}
