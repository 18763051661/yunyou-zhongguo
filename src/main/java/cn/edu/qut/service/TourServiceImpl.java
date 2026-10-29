package cn.edu.qut.service;

import cn.edu.qut.common.BusinessException;
import cn.edu.qut.dao.RouteDao;
import cn.edu.qut.dao.TourRecordDao;
import cn.edu.qut.entity.Route;
import cn.edu.qut.entity.RouteSpot;
import cn.edu.qut.entity.TourRecord;
import cn.edu.qut.support.ImageService;
import cn.edu.qut.vo.RouteDetailVO;
import cn.edu.qut.vo.TourProgressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.format.DateTimeFormatter;

@Service
public class TourServiceImpl implements TourService {

    /** 足迹时间统一输出 ISO 本地时间, 前端 new Date(...) 可直接解析(Map 值上的 @JsonFormat 不生效) */
    private static final DateTimeFormatter VISITED_AT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RouteDao routeDao;
    private final TourRecordDao tourRecordDao;
    private final RouteService routeService;
    private final ImageService imageService;

    public TourServiceImpl(RouteDao routeDao, TourRecordDao tourRecordDao,
                           RouteService routeService, ImageService imageService) {
        this.routeDao = routeDao;
        this.tourRecordDao = tourRecordDao;
        this.routeService = routeService;
        this.imageService = imageService;
    }

    /**
     * 开始一条路线的在线游览: 返回路线详情 + 当前进度。
     */
    @Override
    public Map<String, Object> start(Long routeId, String sessionId, Long userId) {
        if (routeId == null || routeDao.findById(routeId) == null) {
            throw new BusinessException(404, "路线不存在");
        }
        String sid = ensureSession(sessionId);
        RouteDetailVO detail = routeService.detail(routeId);
        TourProgressVO progress = progress(sid, routeId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sid);
        result.put("route", detail);
        result.put("progress", progress);
        return result;
    }

    /**
     * 到达并打卡某个景点。
     */
    @Override
    @Transactional
    public TourProgressVO visit(String sessionId, Long routeId, Long spotId, Long userId) {
        String sid = ensureSession(sessionId);
        Route route = routeDao.findById(routeId);
        if (route == null) {
            throw new BusinessException(404, "路线不存在");
        }
        List<RouteSpot> steps = routeDao.findSpotsByRoute(routeId);
        boolean belongs = steps.stream().anyMatch(rs -> rs.getSpotId().equals(spotId));
        if (!belongs) {
            throw new BusinessException("该景点不属于当前游览路线");
        }
        if (tourRecordDao.exists(sid, spotId) == 0) {
            TourRecord record = new TourRecord();
            record.setSessionId(sid);
            record.setRouteId(routeId);
            record.setSpotId(spotId);
            record.setUserId(userId);
            tourRecordDao.insert(record);
        }
        return progress(sid, routeId);
    }

    @Override
    public TourProgressVO progress(String sessionId, Long routeId) {
        if (routeId == null || routeDao.findById(routeId) == null) {
            throw new BusinessException(404, "路线不存在");
        }
        String sid = ensureSession(sessionId);
        List<Long> routeSpotIds = routeDao.findSpotsByRoute(routeId).stream()
                .map(RouteSpot::getSpotId).toList();
        List<Long> visitedIds = tourRecordDao.findBySession(sid).stream()
                .map(TourRecord::getSpotId)
                .filter(routeSpotIds::contains)
                .distinct()
                .toList();
        return new TourProgressVO(sid, routeId, routeSpotIds.size(), visitedIds.size(), visitedIds);
    }

    /**
     * 我的游览足迹(按浏览器会话)。
     */
    @Override
    public List<Map<String, Object>> records(String sessionId) {
        String sid = ensureSession(sessionId);
        return tourRecordDao.findBySession(sid).stream().map(record -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("spotId", record.getSpotId());
            map.put("spotName", record.getSpotName());
            map.put("imageUrl", "/img/spot/" + record.getSpotId() + "?n=0&v=2");
            map.put("categoryId", record.getSpotCategoryId());
            map.put("province", record.getSpotProvince());
            map.put("city", record.getSpotCity());
            map.put("lng", record.getSpotLng());
            map.put("lat", record.getSpotLat());
            map.put("visitedAt", record.getVisitedAt() == null
                    ? null : VISITED_AT_FMT.format(record.getVisitedAt()));
            return map;
        }).toList();
    }

    /**
     * 省份足迹聚合 + 徽章成就(供"我的足迹"中国地图点亮)。
     */
    @Override
    public Map<String, Object> footprint(String sessionId) {
        String sid = ensureSession(sessionId);
        List<Map<String, Object>> rows = tourRecordDao.provinceFootprint(sid);
        int spotCount = tourRecordDao.countSpotsBySession(sid);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sid);
        result.put("provinces", rows);
        result.put("provinceCount", rows.size());
        result.put("spotCount", spotCount);
        result.put("badges", badges(rows.size(), spotCount));
        return result;
    }

    private List<String> badges(int provinceCount, int spotCount) {
        List<String> badges = new ArrayList<>();
        if (spotCount >= 1) badges.add("初出茅庐");
        if (spotCount >= 5) badges.add("打卡达人");
        if (spotCount >= 20) badges.add("环游先锋");
        if (provinceCount >= 3) badges.add("走遍三省");
        if (provinceCount >= 7) badges.add("纵横七大区");
        if (provinceCount >= 15) badges.add("神州行者");
        if (provinceCount >= 31) badges.add("点亮中国");
        return badges;
    }

    private String ensureSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "tour-" + UUID.randomUUID().toString().replace("-", "");
        }
        return sessionId.trim();
    }
}
