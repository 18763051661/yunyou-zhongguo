package cn.edu.qut.mapper;

import cn.edu.qut.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper {

    @Insert("insert into spot_comment (user_id, spot_id, rating, content) "
            + "values (#{userId}, #{spotId}, #{rating}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    @Select("select cm.id, cm.user_id, cm.spot_id, cm.rating, cm.content, cm.created_at, "
            + "u.nickname as nickname, u.avatar as avatar "
            + "from spot_comment cm join sys_user u on u.id = cm.user_id "
            + "where cm.spot_id = #{spotId} order by cm.created_at desc limit #{offset}, #{size}")
    List<Comment> findBySpot(@Param("spotId") Long spotId,
                             @Param("offset") int offset,
                             @Param("size") int size);

    @Select("select count(*) from spot_comment where spot_id = #{spotId}")
    long countBySpot(Long spotId);

    @Select("select count(*) as total, coalesce(avg(rating), 0) as avgRating from spot_comment")
    Map<String, Object> overallStats();

    @Select("select count(*) from spot_comment")
    long countAll();

    /** 按评分分组统计某景点评论分布 (for 评分条形图) */
    @Select("select rating as rating, count(*) as cnt from spot_comment where spot_id = #{spotId} group by rating order by rating desc")
    List<Map<String, Object>> ratingDistribution(@Param("spotId") Long spotId);
}
