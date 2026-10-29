package cn.edu.qut.mapper;

import cn.edu.qut.entity.VrViewLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * VR 环景舱游览记录 Mapper(注解 SQL)。
 */
@Mapper
public interface VrViewLogMapper {

    @Insert("insert into vr_view_log(spot_id, session_id) values(#{spotId}, #{sessionId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VrViewLog log);

    @Select("select count(*) from vr_view_log where spot_id = #{spotId}")
    long countBySpot(@Param("spotId") Long spotId);

    /** VR 热度榜: 联表景点表聚合, 取被 VR 游览最多的 N 个 */
    @Select("select s.id, s.name, s.province, s.image_url as imageUrl, "
            + "count(*) as vrCount "
            + "from vr_view_log v join scenic_spot s on s.id = v.spot_id "
            + "group by v.spot_id, s.id, s.name, s.province, s.image_url "
            + "order by vrCount desc limit #{n}")
    List<Map<String, Object>> hot(@Param("n") int n);

    /** 某会话最近 VR 游览的景点(用于足迹展示) */
    @Select("select v.id, v.spot_id as spotId, s.name as spotName, s.province, v.viewed_at as viewedAt "
            + "from vr_view_log v join scenic_spot s on s.id = v.spot_id "
            + "where v.session_id = #{sessionId} order by v.viewed_at desc limit #{n}")
    List<Map<String, Object>> recentBySession(@Param("sessionId") String sessionId, @Param("n") int n);
}
