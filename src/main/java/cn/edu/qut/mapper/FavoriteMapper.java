package cn.edu.qut.mapper;

import cn.edu.qut.entity.Favorite;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FavoriteMapper {

    @Insert("insert into favorite (user_id, spot_id) values (#{userId}, #{spotId})")
    int insert(@Param("userId") Long userId, @Param("spotId") Long spotId);

    @Delete("delete from favorite where user_id = #{userId} and spot_id = #{spotId}")
    int delete(@Param("userId") Long userId, @Param("spotId") Long spotId);

    @Select("select count(*) from favorite where user_id = #{userId} and spot_id = #{spotId}")
    int exists(@Param("userId") Long userId, @Param("spotId") Long spotId);

    @Select("select f.id, f.user_id, f.spot_id, f.created_at, "
            + "s.name as spot_name, s.summary as spot_summary, s.image_prompt as spot_image_prompt, "
            + "s.rating as spot_rating, s.category_id as spot_category_id, c.name as category_name "
            + "from favorite f "
            + "join scenic_spot s on s.id = f.spot_id "
            + "left join category c on c.id = s.category_id "
            + "where f.user_id = #{userId} order by f.created_at desc")
    List<Favorite> findByUser(Long userId);
}
