package cn.edu.qut.dao;

import cn.edu.qut.entity.Route;
import cn.edu.qut.entity.RouteSpot;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 路线 DAO */
public interface RouteDao {

    List<Route> findAll();

    Route findById(Long id);

    List<RouteSpot> findSpotsByRoute(Long routeId);

    int countSpots(@Param("routeId") Long routeId);
}
