package cn.edu.qut.service;

import cn.edu.qut.vo.TourProgressVO;

import java.util.List;
import java.util.Map;

/**
 * 在线游览 Service
 */
public interface TourService {

    /**
     * 开始一条路线的在线游览: 返回路线详情 + 当前进度。
     *
     * @param routeId   路线 ID
     * @param sessionId 浏览器会话 ID(可为 null, 自动生成)
     * @param userId    当前用户 ID
     * @return 会话 ID、路线详情与当前进度
     */
    Map<String, Object> start(Long routeId, String sessionId, Long userId);

    /**
     * 到达并打卡某个景点。
     *
     * @param sessionId 浏览器会话 ID
     * @param routeId   路线 ID
     * @param spotId    景点 ID
     * @param userId    当前用户 ID
     * @return 当前进度
     */
    TourProgressVO visit(String sessionId, Long routeId, Long spotId, Long userId);

    /**
     * 查询当前游览进度。
     *
     * @param sessionId 浏览器会话 ID
     * @param routeId   路线 ID
     * @return 当前进度
     */
    TourProgressVO progress(String sessionId, Long routeId);

    /**
     * 我的游览足迹(按浏览器会话)。
     *
     * @param sessionId 浏览器会话 ID
     * @return 游览记录列表
     */
    List<Map<String, Object>> records(String sessionId);

    /**
     * 省份足迹聚合 + 徽章成就(供"我的足迹"中国地图点亮)。
     *
     * @param sessionId 浏览器会话 ID
     * @return 会话 ID、省份聚合、计数与徽章列表
     */
    Map<String, Object> footprint(String sessionId);
}
