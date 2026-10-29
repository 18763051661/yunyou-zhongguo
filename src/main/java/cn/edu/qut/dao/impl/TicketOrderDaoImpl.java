package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.TicketOrderDao;
import cn.edu.qut.entity.TicketOrder;
import cn.edu.qut.mapper.TicketOrderMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 门票订单 DAO 实现 */
@Repository
public class TicketOrderDaoImpl implements TicketOrderDao {

    @Autowired
    private TicketOrderMapper ticketOrderMapper;

    @Override
    public int insert(@Param("order") TicketOrder order) {
        return ticketOrderMapper.insert(order);
    }

    @Override
    public List<TicketOrder> findByUser(@Param("userId") Long userId) {
        return ticketOrderMapper.findByUser(userId);
    }

    @Override
    public TicketOrder findByOrderNo(@Param("orderNo") String orderNo) {
        return ticketOrderMapper.findByOrderNo(orderNo);
    }

    @Override
    public int markPaid(@Param("orderNo") String orderNo,
                        @Param("status") String status,
                        @Param("method") String method) {
        return ticketOrderMapper.markPaid(orderNo, status, method);
    }

    @Override
    public int cancel(@Param("orderNo") String orderNo) {
        return ticketOrderMapper.cancel(orderNo);
    }

    @Override
    public int deleteByOrderNo(@Param("orderNo") String orderNo) {
        return ticketOrderMapper.deleteByOrderNo(orderNo);
    }

    // ==================== 管理端 ====================

    @Override
    public List<TicketOrder> findAdminPage(String keyword, int offset, int size) {
        return ticketOrderMapper.findAdminPage(keyword, offset, size);
    }

    @Override
    public long countAdminPage(String keyword) {
        return ticketOrderMapper.countAdminPage(keyword);
    }
}
