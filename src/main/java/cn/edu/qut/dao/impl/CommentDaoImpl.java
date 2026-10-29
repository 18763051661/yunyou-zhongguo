package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.CommentDao;
import cn.edu.qut.entity.Comment;
import cn.edu.qut.mapper.CommentMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 评论 DAO 实现 */
@Repository
public class CommentDaoImpl implements CommentDao {

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public int insert(Comment comment) {
        return commentMapper.insert(comment);
    }

    @Override
    public List<Comment> findBySpot(@Param("spotId") Long spotId,
                                    @Param("offset") int offset,
                                    @Param("size") int size) {
        return commentMapper.findBySpot(spotId, offset, size);
    }

    @Override
    public long countBySpot(Long spotId) {
        return commentMapper.countBySpot(spotId);
    }

    @Override
    public List<Map<String, Object>> ratingDistribution(Long spotId) {
        return commentMapper.ratingDistribution(spotId);
    }

    @Override
    public Map<String, Object> overallStats() {
        return commentMapper.overallStats();
    }

    @Override
    public long countAll() {
        return commentMapper.countAll();
    }
}
