package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.service.PanoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 真 360° 全景素材接口。
 */
@RestController
@RequestMapping("/api/pano")
public class PanoController {

    private final PanoService panoService;

    public PanoController(PanoService panoService) {
        this.panoService = panoService;
    }

    /** 全部支持真 360° 全景的景点清单。 */
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(panoService.list());
    }

    /** 某景点的全景素材定位(六面立方体贴图地址)。 */
    @GetMapping("/{spotId}")
    public Result<?> bySpot(@PathVariable("spotId") Long spotId) {
        return Result.ok(panoService.findBySpot(spotId));
    }
}
