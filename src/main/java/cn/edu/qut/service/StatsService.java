package cn.edu.qut.service;

import java.util.Map;

/**
 * 统计 Service
 */
public interface StatsService {

    /**
     * 获取站点总览统计信息(景点、路线、评论、用户、收藏、平均评分及各类分布)。
     *
     * @return 统计总览
     */
    Map<String, Object> overview();
}
