package cn.edu.qut.mapper;

import cn.edu.qut.entity.Route;
import cn.edu.qut.entity.RouteSpot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RouteMapper {

    @Select("select * from route order by sort_no asc, id asc")
    List<Route> findAll();

    @Select("select * from route where id = #{id}")
    Route findById(Long id);

    @Select("select rs.id, rs.route_id, rs.spot_id, rs.day_no, rs.step_no, rs.stay_minutes, rs.narration, "
            + "s.name as spot_name, s.summary as spot_summary, s.image_prompt as spot_image_prompt, "
            + "s.province as spot_province, s.city as spot_city, s.lng as spot_lng, s.lat as spot_lat, "
            + "s.open_time as spot_open_time, s.difficulty as spot_difficulty, "
            + "s.duration_minutes as spot_duration_minutes, s.price as spot_price, s.rating as spot_rating, "
            + "s.tags as spot_tags, s.category_id as spot_category_id "
            + "from route_spot rs join scenic_spot s on s.id = rs.spot_id "
            + "where rs.route_id = #{routeId} order by rs.day_no asc, rs.step_no asc")
    List<RouteSpot> findSpotsByRoute(Long routeId);

    @Select("select count(*) from route_spot where route_id = #{routeId}")
    int countSpots(@Param("routeId") Long routeId);
}
