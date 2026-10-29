package cn.edu.qut.mapper;

import cn.edu.qut.entity.TourRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TourRecordMapper {

    @Select("select count(*) from tour_record where session_id = #{sessionId} and spot_id = #{spotId}")
    int exists(@Param("sessionId") String sessionId, @Param("spotId") Long spotId);

    @Insert("insert into tour_record (session_id, user_id, route_id, spot_id) "
            + "values (#{sessionId}, #{userId}, #{routeId}, #{spotId})")
    int insert(TourRecord record);

    @Select("select count(distinct tr.spot_id) from tour_record tr "
            + "join route_spot rs on rs.spot_id = tr.spot_id and rs.route_id = #{routeId} "
            + "where tr.session_id = #{sessionId} and tr.route_id = #{routeId}")
    int countVisitedInRoute(@Param("sessionId") String sessionId, @Param("routeId") Long routeId);

    @Select("select tr.id, tr.session_id, tr.user_id, tr.route_id, tr.spot_id, tr.visited_at, "
            + "s.name as spot_name, s.image_prompt as spot_image_prompt, s.category_id as spot_category_id, "
            + "s.province as spot_province, s.city as spot_city, s.lng as spot_lng, s.lat as spot_lat "
            + "from tour_record tr join scenic_spot s on s.id = tr.spot_id "
            + "where tr.session_id = #{sessionId} order by tr.visited_at desc")
    List<TourRecord> findBySession(String sessionId);

    @Select("select count(distinct spot_id) from tour_record where session_id = #{sessionId}")
    int countSpotsBySession(String sessionId);

    @Select("select s.region as region, s.province as province, count(distinct tr.spot_id) as spotCount "
            + "from tour_record tr join scenic_spot s on s.id = tr.spot_id "
            + "where tr.session_id = #{sessionId} group by s.region, s.province order by spotCount desc")
    List<java.util.Map<String, Object>> provinceFootprint(String sessionId);
}
