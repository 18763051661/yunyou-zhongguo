package cn.edu.qut.dao;

import cn.edu.qut.entity.TicketOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 门票订单 DAO */
public interface TicketOrderDao {

    int insert(@Param("order") TicketOrder order);

    List<TicketOrder> findByUser(@Param("userId") Long userId);

    TicketOrder findByOrderNo(@Param("orderNo") String orderNo);

    int markPaid(@Param("orderNo") String orderNo,
                 @Param("status") String status,
                 @Param("method") String method);

    int cancel(@Param("orderNo") String orderNo);

    /** 删除订单记录 (仅 CANCELED 可删, SQL 带状态条件兜底) */
    int deleteByOrderNo(@Param("orderNo") String orderNo);

    // ==================== 管理端 ====================

    /** 管理端订单分页, 关键词匹配订单号/景点名/联系人 */
    List<TicketOrder> findAdminPage(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    long countAdminPage(@Param("keyword") String keyword);
}
