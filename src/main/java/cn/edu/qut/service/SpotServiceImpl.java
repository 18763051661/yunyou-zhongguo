package cn.edu.qut.service;

import cn.edu.qut.common.BusinessException;
import cn.edu.qut.common.PageResult;
import cn.edu.qut.dao.FavoriteDao;
import cn.edu.qut.dao.SpotDao;
import cn.edu.qut.entity.ScenicSpot;
import cn.edu.qut.support.ImageService;
import cn.edu.qut.vo.SpotVO;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SpotServiceImpl implements SpotService {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("default", "rating", "priceAsc", "priceDesc", "durationAsc", "difficultyAsc", "province");

    private final SpotDao spotDao;
    private final CategoryService categoryService;
    private final FavoriteDao favoriteDao;
    private final ImageService imageService;

    public SpotServiceImpl(SpotDao spotDao, CategoryService categoryService,
                           FavoriteDao favoriteDao, ImageService imageService) {
        this.spotDao = spotDao;
        this.categoryService = categoryService;
        this.favoriteDao = favoriteDao;
        this.imageService = imageService;
    }

    @Override
    public PageResult<SpotVO> page(Long categoryId, String province, String region, Boolean freeOnly,
                                   String keyword, String tag, Integer maxDifficulty,
                                   String sort, Integer page, Integer size, Long userId) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 12 : Math.min(size, 60);
        String orderBy = (sort == null || !ALLOWED_SORTS.contains(sort)) ? "default" : sort;
        String kw = normalize(keyword);
        String tg = normalize(tag);
        String pv = normalize(province);
        String rg = normalize(region);

        long total = spotDao.countPage(categoryId, pv, rg, freeOnly, kw, tg, maxDifficulty);
        List<ScenicSpot> spots = spotDao.findPage(categoryId, pv, rg, freeOnly, kw, tg, maxDifficulty,
                orderBy, (pageNo - 1) * pageSize, pageSize);
        Map<Long, String> nameMap = categoryService.nameMap();
        List<SpotVO> list = spots.stream()
                .map(s -> decorate(s, nameMap.get(s.getCategoryId()), userId))
                .toList();
        return new PageResult<>(list, total, pageNo, pageSize);
    }

    @Override
    public SpotVO detail(Long id, Long userId) {
        ScenicSpot spot = spotDao.findById(id);
        if (spot == null || spot.getStatus() == null || spot.getStatus() != 1) {
            throw new BusinessException(404, "景点不存在或已下架");
        }
        return decorate(spot, categoryService.nameOf(spot.getCategoryId()), userId);
    }

    @Override
    public List<SpotVO> topPopular(int n) {
        Map<Long, String> nameMap = categoryService.nameMap();
        return spotDao.findTopPopular(n).stream()
                .map(s -> decorate(s, nameMap.get(s.getCategoryId()), null))
                .toList();
    }

    @Override
    public List<SpotVO> topRating(int n) {
        Map<Long, String> nameMap = categoryService.nameMap();
        return spotDao.findTopRating(n).stream()
                .map(s -> decorate(s, nameMap.get(s.getCategoryId()), null))
                .toList();
    }

    @Override
    public List<SpotVO> all() {
        Map<Long, String> nameMap = categoryService.nameMap();
        return spotDao.findAllOnShelf().stream()
                .map(s -> SpotVO.from(s, nameMap.get(s.getCategoryId()), imageService))
                .toList();
    }

    /** 云游盲盒: 随机抽取一个上架景点 */
    @Override
    public ScenicSpot randomOnShelf() {
        ScenicSpot spot = spotDao.findRandom();
        if (spot == null) {
            throw new BusinessException(404, "暂无可抽取的景点");
        }
        return spot;
    }

    /** 全国省份聚合导览 */
    @Override
    public List<Map<String, Object>> provinces() {
        return spotDao.provinceAggregate();
    }

    /** 基于经纬度的"附近推荐"(Haversine 球面距离, km) */
    @Override
    public List<Map<String, Object>> nearby(double lng, double lat, int n) {
        int limit = n <= 0 ? 8 : Math.min(n, 30);
        return all().stream()
                .map(s -> {
                    double dist = haversine(lat, lng, s.getLat(), s.getLng());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("spot", s);
                    m.put("distanceKm", (int) Math.round(dist));
                    return m;
                })
                .sorted(Comparator.comparingDouble(m -> ((Integer) m.get("distanceKm")).doubleValue()))
                .limit(limit)
                .toList();
    }

    /** Haversine 球面距离(km), 包可见以便单元测试 */
    double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ==================== 管理端 ====================

    @Override
    public PageResult<ScenicSpot> adminPage(String keyword, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        String kw = normalize(keyword);
        long total = spotDao.countAdminPage(kw);
        List<ScenicSpot> list = spotDao.findAdminPage(kw, (pageNo - 1) * pageSize, pageSize);
        return new PageResult<>(list, total, pageNo, pageSize);
    }

    @Override
    public ScenicSpot adminCreate(ScenicSpot spot) {
        validate(spot, true);
        if (spot.getStatus() == null) {
            spot.setStatus(1);
        }
        spotDao.insert(spot);
        return spot;
    }

    @Override
    public ScenicSpot adminUpdate(ScenicSpot spot) {
        if (spot.getId() == null || spotDao.findById(spot.getId()) == null) {
            throw new BusinessException(404, "景点不存在");
        }
        validate(spot, false);
        spotDao.update(spot);
        return spotDao.findById(spot.getId());
    }

    @Override
    public void adminDelete(Long id) {
        if (spotDao.deleteById(id) <= 0) {
            throw new BusinessException(404, "景点不存在");
        }
    }

    private void validate(ScenicSpot spot, boolean creating) {
        if (spot == null || spot.getName() == null || spot.getName().isBlank()) {
            throw new BusinessException(400, "景点名称不能为空");
        }
        if (creating && spot.getCategoryId() == null) {
            throw new BusinessException(400, "请选择景点分类");
        }
        if (spot.getPrice() != null && spot.getPrice().signum() < 0) {
            throw new BusinessException(400, "门票价格不能为负数");
        }
        if (spot.getStatus() != null && spot.getStatus() != 0 && spot.getStatus() != 1) {
            throw new BusinessException(400, "状态仅允许 0(下架) 或 1(上架)");
        }
    }

    private SpotVO decorate(ScenicSpot spot, String categoryName, Long userId) {
        SpotVO vo = SpotVO.from(spot, categoryName, imageService);
        if (userId != null) {
            vo.setFavorited(favoriteDao.exists(userId, spot.getId()) > 0);
        }
        return vo;
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
