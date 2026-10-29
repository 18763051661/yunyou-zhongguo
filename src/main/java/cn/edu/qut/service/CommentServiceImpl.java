package cn.edu.qut.service;

import cn.edu.qut.common.BusinessException;
import cn.edu.qut.common.PageResult;
import cn.edu.qut.dao.CommentDao;
import cn.edu.qut.dao.SpotDao;
import cn.edu.qut.entity.Comment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentDao commentDao;
    private final SpotDao spotDao;

    public CommentServiceImpl(CommentDao commentDao, SpotDao spotDao) {
        this.commentDao = commentDao;
        this.spotDao = spotDao;
    }

    @Override
    @Transactional
    public Map<String, Object> add(Long userId, Long spotId, Integer rating, String content) {
        if (spotId == null || spotDao.findById(spotId) == null) {
            throw new BusinessException(404, "景点不存在");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分必须在 1-5 星之间");
        }
        String text = content == null ? "" : content.trim();
        if (text.length() < 2 || text.length() > 300) {
            throw new BusinessException("评论内容需为 2-300 字");
        }
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setSpotId(spotId);
        comment.setRating(rating);
        comment.setContent(text);
        commentDao.insert(comment);
        spotDao.applyNewRating(spotId, rating);
        return Map.of("ok", true);
    }

    @Override
    public PageResult<Comment> list(Long spotId, Integer page, Integer size) {
        if (spotId == null || spotDao.findById(spotId) == null) {
            throw new BusinessException(404, "景点不存在");
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);
        List<Comment> list = commentDao.findBySpot(spotId, (pageNo - 1) * pageSize, pageSize);
        long total = commentDao.countBySpot(spotId);
        return new PageResult<>(list, total, pageNo, pageSize);
    }

    @Override
    public Map<String, Object> spotRatingSummary(Long spotId) {
        Map<String, Object> map = new LinkedHashMap<>();
        long total = commentDao.countBySpot(spotId);
        map.put("total", total);
        // 确保 distribution 里始终有 1-5 星, 便于 JSP c:forEach 渲染条形图
        Map<Integer, Long> distMap = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) distMap.put(i, 0L);
        for (Map<String, Object> row : commentDao.ratingDistribution(spotId)) {
            int rating = ((Number) row.get("rating")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            distMap.put(rating, cnt);
        }
        List<Map<String, Object>> distribution = new java.util.ArrayList<>();
        for (Map.Entry<Integer, Long> e : distMap.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("rating", e.getKey());
            r.put("count", e.getValue());
            r.put("percent", total == 0 ? 0 : (e.getValue() * 100.0 / total));
            distribution.add(r);
        }
        map.put("distribution", distribution);
        return map;
    }
}
