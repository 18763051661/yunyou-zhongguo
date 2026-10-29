package cn.edu.qut.service;

import cn.edu.qut.dto.Requests;
import cn.edu.qut.vo.RecommendItemVO;

import java.util.List;

/**
 * 智能推荐 Service
 */
public interface RecommendService {

    /**
     * 多因子加权评分推荐: 综合兴趣标签匹配、评分口碑、热度、人群适配、时长适配、预算适配等因子,
     * 返回 Top 6 推荐景点及推荐理由。
     *
     * @param request 推荐请求(兴趣、时长、预算、人群、目的地等)
     * @return 推荐结果列表
     */
    List<RecommendItemVO> recommend(Requests.RecommendRequest request);
}
