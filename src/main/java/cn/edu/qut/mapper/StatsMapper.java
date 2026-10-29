package cn.edu.qut.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    @Select("select count(*) from scenic_spot where status = 1")
    long countSpots();

    @Select("select count(distinct province) from scenic_spot where status = 1")
    long countProvinces();

    @Select("select region as region, count(*) as count from scenic_spot where status = 1 group by region order by field(region,'华北','东北','华东','华中','华南','西南','西北')")
    List<Map<String, Object>> regionDistribution();

    @Select("select count(*) from route")
    long countRoutes();

    @Select("select count(*) from spot_comment")
    long countComments();

    @Select("select count(*) from sys_user")
    long countUsers();

    @Select("select count(*) from favorite")
    long countFavorites();

    @Select("select coalesce(avg(rating), 0) from spot_comment")
    double avgRating();

    @Select("select c.name as name, count(s.id) as count from category c "
            + "left join scenic_spot s on s.category_id = c.id and s.status = 1 "
            + "group by c.id, c.name order by c.sort_no")
    List<Map<String, Object>> categoryDistribution();

    @Select("select coalesce(rating, 5) as rating, count(*) as count from spot_comment group by rating order by rating")
    List<Map<String, Object>> ratingDistribution();
}
