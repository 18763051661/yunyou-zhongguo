package cn.edu.qut.service;

import cn.edu.qut.common.BusinessException;
import cn.edu.qut.dao.RouteDao;
import cn.edu.qut.entity.Route;
import cn.edu.qut.entity.RouteSpot;
import cn.edu.qut.support.ImageService;
import cn.edu.qut.vo.RouteDetailVO;
import cn.edu.qut.vo.RouteStepVO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteServiceImpl implements RouteService {

    private final RouteDao routeDao;
    private final ImageService imageService;

    public RouteServiceImpl(RouteDao routeDao, ImageService imageService) {
        this.routeDao = routeDao;
        this.imageService = imageService;
    }

    @Override
    public List<Map<String, Object>> list() {
        return routeDao.findAll().stream().map(route -> {
            Map<String, Object> map = toMap(route);
            List<RouteSpot> routeSpots = routeDao.findSpotsByRoute(route.getId());
            map.put("coverUrl", coverUrl(route, routeSpots));
            map.put("totalSpots", routeSpots.size());
            return map;
        }).toList();
    }

    @Override
    public RouteDetailVO detail(Long routeId) {
        Route route = routeDao.findById(routeId);
        if (route == null) {
            throw new BusinessException(404, "路线不存在");
        }
        List<RouteSpot> routeSpots = routeDao.findSpotsByRoute(routeId);
        List<RouteStepVO> steps = routeSpots.stream()
                .map(rs -> RouteStepVO.from(rs, imageService))
                .toList();

        RouteDetailVO vo = new RouteDetailVO();
        vo.setId(route.getId());
        vo.setName(route.getName());
        vo.setSubtitle(route.getSubtitle());
        vo.setDescription(route.getDescription());
        vo.setDurationMinutes(route.getDurationMinutes() == null ? 0 : route.getDurationMinutes());
        vo.setDifficulty(route.getDifficulty() == null ? 1 : route.getDifficulty());
        vo.setTags(split(route.getTags()));
        vo.setSuitableFor(split(route.getSuitableFor()));
        vo.setCoverUrl(coverUrl(route, routeSpots));
        vo.setTotalSpots(steps.size());
        vo.setTotalStayMinutes(steps.stream().mapToInt(RouteStepVO::getStayMinutes).sum());
        vo.setSpots(steps);
        return vo;
    }

    private Map<String, Object> toMap(Route route) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", route.getId());
        map.put("name", route.getName());
        map.put("subtitle", route.getSubtitle());
        map.put("description", route.getDescription());
        map.put("durationMinutes", route.getDurationMinutes());
        map.put("difficulty", route.getDifficulty());
        map.put("tags", split(route.getTags()));
        map.put("suitableFor", split(route.getSuitableFor()));
        return map;
    }

    /**
     * 路线封面: 直接使用路线首个景点的真实照片(标题强匹配, 相关度有保证);
     * 路线下没有景点时才退回按路线名搜索。
     */
    private String coverUrl(Route route, List<RouteSpot> routeSpots) {
        if (routeSpots != null && !routeSpots.isEmpty() && routeSpots.get(0).getSpotId() != null) {
            return "/img/spot/" + routeSpots.get(0).getSpotId() + "?n=0&v=2";
        }
        String kw = (route.getName() == null || route.getName().isBlank())
                ? "中国风景区" : route.getName();
        try {
            return "/img/kw?kw=" + java.net.URLEncoder.encode(kw, java.nio.charset.StandardCharsets.UTF_8) + "&v=2";
        } catch (Exception e) {
            return "/img/kw?kw=" + route.getId() + "&v=2";
        }
    }

    private List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(text.split("[,，]")).map(String::trim).filter(t -> !t.isEmpty()).toList();
    }
}
