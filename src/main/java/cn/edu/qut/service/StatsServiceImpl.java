package cn.edu.qut.service;

import cn.edu.qut.dao.StatsDao;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatsServiceImpl implements StatsService {

    private final StatsDao statsDao;
    private final SpotService spotService;

    public StatsServiceImpl(StatsDao statsDao, SpotService spotService) {
        this.statsDao = statsDao;
        this.spotService = spotService;
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("spotCount", statsDao.countSpots());
        map.put("provinceCount", statsDao.countProvinces());
        map.put("routeCount", statsDao.countRoutes());
        map.put("commentCount", statsDao.countComments());
        map.put("userCount", statsDao.countUsers());
        map.put("favoriteCount", statsDao.countFavorites());
        map.put("avgRating", Math.round(statsDao.avgRating() * 10.0) / 10.0);
        map.put("categoryDistribution", statsDao.categoryDistribution());
        map.put("regionDistribution", statsDao.regionDistribution());
        map.put("provinceDistribution", spotService.provinces());
        map.put("ratingDistribution", statsDao.ratingDistribution());
        map.put("hotSpots", spotService.topPopular(8));
        map.put("topRatedSpots", spotService.topRating(8));
        return map;
    }
}
