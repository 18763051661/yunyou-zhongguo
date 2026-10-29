package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.RouteDao;
import cn.edu.qut.entity.Route;
import cn.edu.qut.entity.RouteSpot;
import cn.edu.qut.mapper.RouteMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 路线 DAO 实现 */
@Repository
public class RouteDaoImpl implements RouteDao {

    @Autowired
    private RouteMapper routeMapper;

    @Override
    public List<Route> findAll() {
        return routeMapper.findAll();
    }

    @Override
    public Route findById(Long id) {
        return routeMapper.findById(id);
    }

    @Override
    public List<RouteSpot> findSpotsByRoute(Long routeId) {
        return routeMapper.findSpotsByRoute(routeId);
    }

    @Override
    public int countSpots(@Param("routeId") Long routeId) {
        return routeMapper.countSpots(routeId);
    }
}
