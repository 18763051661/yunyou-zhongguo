package cn.edu.qut.mapper;

import cn.edu.qut.entity.TravelPlan;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TravelPlanMapper {

    @Insert("insert into travel_plan(pref_id,title,summary,days,spots_json,itinerary,provider) "
            + "values(#{prefId},#{title},#{summary},#{days},#{spotsJson},#{itinerary},#{provider})")
    int insert(TravelPlan plan);

    @Select("select * from travel_plan where id=#{id}")
    TravelPlan findById(Long id);

    @Select("select * from travel_plan where pref_id=#{prefId} order by id desc limit 1")
    TravelPlan findByPref(Long prefId);

    @Select("select * from travel_plan order by id desc limit 8")
    List<TravelPlan> recent();
}
