package cn.edu.qut.mapper;

import cn.edu.qut.entity.TicketOrder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TicketOrderMapper {

    @Insert("insert into ticket_order (order_no, user_id, spot_id, spot_name, quantity, unit_price, total_amount, "
            + "contact_name, contact_phone, pay_status, pay_method, visit_date, remark) "
            + "values (#{order.orderNo}, #{order.userId}, #{order.spotId}, #{order.spotName}, #{order.quantity}, "
            + "#{order.unitPrice}, #{order.totalAmount}, #{order.contactName}, #{order.contactPhone}, "
            + "#{order.payStatus}, #{order.payMethod}, #{order.visitDate}, #{order.remark})")
    @Options(useGeneratedKeys = true, keyProperty = "order.id")
    int insert(@Param("order") TicketOrder order);

    @Select("select o.*, s.image_prompt as spot_image_prompt, s.price as spot_price "
            + "from ticket_order o join scenic_spot s on s.id = o.spot_id "
            + "where o.user_id = #{userId} order by o.created_at desc")
    List<TicketOrder> findByUser(@Param("userId") Long userId);

    @Select("select o.*, s.image_prompt as spot_image_prompt "
            + "from ticket_order o join scenic_spot s on s.id = o.spot_id "
            + "where o.order_no = #{orderNo}")
    TicketOrder findByOrderNo(@Param("orderNo") String orderNo);

    @Update("update ticket_order set pay_status = #{status}, pay_method = #{method}, "
            + "pay_time = now() where order_no = #{orderNo}")
    int markPaid(@Param("orderNo") String orderNo,
                 @Param("status") String status,
                 @Param("method") String method);

    @Update("update ticket_order set pay_status = 'CANCELED' where order_no = #{orderNo}")
    int cancel(@Param("orderNo") String orderNo);

    /** 物理删除订单记录 (Service 层白名单: 仅 CANCELED 状态可删) */
    @Delete("delete from ticket_order where order_no = #{orderNo} and pay_status = 'CANCELED'")
    int deleteByOrderNo(@Param("orderNo") String orderNo);

    // ==================== 管理端 ====================

    @Select("<script>"
            + "select o.*, s.image_prompt as spot_image_prompt "
            + "from ticket_order o left join scenic_spot s on s.id = o.spot_id "
            + "<where>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    (o.spot_name like concat('%', #{keyword}, '%') "
            + "      or o.contact_name like concat('%', #{keyword}, '%') "
            + "      or o.order_no like concat('%', #{keyword}, '%'))"
            + "  </if>"
            + "</where>"
            + " order by o.id desc limit #{offset}, #{size}"
            + "</script>")
    List<TicketOrder> findAdminPage(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    @Select("<script>"
            + "select count(*) from ticket_order "
            + "<where>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    (spot_name like concat('%', #{keyword}, '%') "
            + "      or contact_name like concat('%', #{keyword}, '%') "
            + "      or order_no like concat('%', #{keyword}, '%'))"
            + "  </if>"
            + "</where>"
            + "</script>")
    long countAdminPage(@Param("keyword") String keyword);
}
