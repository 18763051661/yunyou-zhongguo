package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.entity.User;
import cn.edu.qut.service.CategoryService;
import cn.edu.qut.service.SpotService;
import cn.edu.qut.support.web.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SpotController {

    private final SpotService spotService;
    private final CategoryService categoryService;

    public SpotController(SpotService spotService, CategoryService categoryService) {
        this.spotService = spotService;
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public Result<?> categories() {
        return Result.ok(categoryService.findAll());
    }

    @GetMapping("/spots")
    public Result<?> spots(@RequestParam(name = "categoryId", required = false) Long categoryId,
                           @RequestParam(name = "province", required = false) String province,
                           @RequestParam(name = "region", required = false) String region,
                           @RequestParam(name = "freeOnly", required = false) Boolean freeOnly,
                           @RequestParam(name = "keyword", required = false) String keyword,
                           @RequestParam(name = "tag", required = false) String tag,
                           @RequestParam(name = "maxDifficulty", required = false) Integer maxDifficulty,
                           @RequestParam(name = "sort", required = false, defaultValue = "default") String sort,
                           @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                           @RequestParam(name = "size", required = false, defaultValue = "12") int size,
                           HttpSession session) {
        User user = SessionUtils.currentUser(session);
        Long userId = user == null ? null : user.getId();
        return Result.ok(spotService.page(categoryId, province, region, freeOnly, keyword, tag,
                maxDifficulty, sort, page, size, userId));
    }

    @GetMapping("/spots/{id}")
    public Result<?> detail(@PathVariable("id") Long id, HttpSession session) {
        User user = SessionUtils.currentUser(session);
        Long userId = user == null ? null : user.getId();
        return Result.ok(spotService.detail(id, userId));
    }

    @GetMapping("/spots/hot")
    public Result<?> hot(@RequestParam(name = "n", required = false, defaultValue = "8") int n) {
        return Result.ok(spotService.topPopular(Math.min(n, 40)));
    }

    @GetMapping("/spots/top-rated")
    public Result<?> topRated(@RequestParam(name = "n", required = false, defaultValue = "8") int n) {
        return Result.ok(spotService.topRating(Math.min(n, 40)));
    }

    /** 云游盲盒: 随机抽取一个上架景点 */
    @GetMapping("/spots/random")
    public Result<?> random() {
        return Result.ok(spotService.randomOnShelf());
    }

    /** 省份聚合(全国导览地图/省份卡片) */
    @GetMapping("/spots/provinces")
    public Result<?> provinces() {
        return Result.ok(spotService.provinces());
    }

    /** 附近推荐: 按经纬度 Haversine 距离排序 */
    @GetMapping("/spots/nearby")
    public Result<?> nearby(@RequestParam(name = "lng", required = false, defaultValue = "116.40") double lng,
                            @RequestParam(name = "lat", required = false, defaultValue = "39.92") double lat,
                            @RequestParam(name = "n", required = false, defaultValue = "8") int n) {
        return Result.ok(spotService.nearby(lng, lat, n));
    }

    /** 全国景点地图: 返回所有上架景点的 id/name/lng/lat/province/rating/category, 供 Leaflet 地图渲染 */
    @GetMapping("/spots/map")
    public Result<?> map() {
        return Result.ok(spotService.all());
    }
}
