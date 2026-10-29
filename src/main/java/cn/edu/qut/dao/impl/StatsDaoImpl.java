package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.StatsDao;
import cn.edu.qut.mapper.StatsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 统计 DAO 实现 */
@Repository
public class StatsDaoImpl implements StatsDao {

    @Autowired
    private StatsMapper statsMapper;

    @Override
    public long countSpots() {
        return statsMapper.countSpots();
    }

    @Override
    public long countProvinces() {
        return statsMapper.countProvinces();
    }

    @Override
    public List<Map<String, Object>> regionDistribution() {
        return statsMapper.regionDistribution();
    }

    @Override
    public long countRoutes() {
        return statsMapper.countRoutes();
    }

    @Override
    public long countComments() {
        return statsMapper.countComments();
    }

    @Override
    public long countUsers() {
        return statsMapper.countUsers();
    }

    @Override
    public long countFavorites() {
        return statsMapper.countFavorites();
    }

    @Override
    public double avgRating() {
        return statsMapper.avgRating();
    }

    @Override
    public List<Map<String, Object>> categoryDistribution() {
        return statsMapper.categoryDistribution();
    }

    @Override
    public List<Map<String, Object>> ratingDistribution() {
        return statsMapper.ratingDistribution();
    }
}
