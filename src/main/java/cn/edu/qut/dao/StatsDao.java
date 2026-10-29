package cn.edu.qut.dao;

import java.util.List;
import java.util.Map;

/** 统计 DAO */
public interface StatsDao {

    long countSpots();

    long countProvinces();

    List<Map<String, Object>> regionDistribution();

    long countRoutes();

    long countComments();

    long countUsers();

    long countFavorites();

    double avgRating();

    List<Map<String, Object>> categoryDistribution();

    List<Map<String, Object>> ratingDistribution();
}
