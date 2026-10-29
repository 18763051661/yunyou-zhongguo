package cn.edu.qut.service;

import cn.edu.qut.common.PageResult;
import cn.edu.qut.entity.Comment;

import java.util.Map;

/**
 * 景点评论 Service
 */
public interface CommentService {

    /**
     * 新增评论并更新景点评分。
     *
     * @param userId  用户 ID
     * @param spotId  景点 ID
     * @param rating  评分 1-5
     * @param content 评论内容
     * @return 操作结果
     */
    Map<String, Object> add(Long userId, Long spotId, Integer rating, String content);

    /**
     * 分页查询某景点的评论。
     *
     * @param spotId 景点 ID
     * @param page   页码
     * @param size   每页数量
     * @return 评论分页结果
     */
    PageResult<Comment> list(Long spotId, Integer page, Integer size);

    /**
     * 查询某景点的评分汇总信息。
     *
     * @param spotId 景点 ID
     * @return 评分汇总
     */
    Map<String, Object> spotRatingSummary(Long spotId);
}
