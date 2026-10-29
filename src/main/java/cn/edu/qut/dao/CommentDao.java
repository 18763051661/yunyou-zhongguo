package cn.edu.qut.dao;

import cn.edu.qut.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 评论 DAO */
public interface CommentDao {

    int insert(Comment comment);

    List<Comment> findBySpot(@Param("spotId") Long spotId,
                             @Param("offset") int offset,
                             @Param("size") int size);

    long countBySpot(Long spotId);

    List<Map<String, Object>> ratingDistribution(Long spotId);

    Map<String, Object> overallStats();

    long countAll();
}
