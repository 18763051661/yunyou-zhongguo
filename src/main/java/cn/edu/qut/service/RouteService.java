package cn.edu.qut.service;

import cn.edu.qut.vo.RouteDetailVO;

import java.util.List;
import java.util.Map;

/**
 * 路线 Service
 */
public interface RouteService {

    /**
     * 查询全部路线列表(含封面及景点数量)。
     *
     * @return 路线列表
     */
    List<Map<String, Object>> list();

    /**
     * 查询某路线的详情，包含路线下的所有景点步骤。
     *
     * @param routeId 路线 ID
     * @return 路线详情
     */
    RouteDetailVO detail(Long routeId);
}
