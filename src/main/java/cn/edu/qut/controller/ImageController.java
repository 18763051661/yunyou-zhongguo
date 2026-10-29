package cn.edu.qut.controller;

import cn.edu.qut.support.SpotImageService;
import cn.edu.qut.support.SpotImageService.ImageData;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 真实景点图片代理。
 * 前端统一通过同源地址取图:
 *   /img/spot/{id}?n=0..3   按景点取真实照片(主图/画廊)
 *   /img/kw?kw=路线名        按关键词取图(路线封面)
 * 后端负责搜索、下载、磁盘缓存与兜底, 浏览器侧无防盗链/跨域/鉴权问题。
 */
@RestController
public class ImageController {

    private final SpotImageService spotImageService;

    public ImageController(SpotImageService spotImageService) {
        this.spotImageService = spotImageService;
    }

    /** 景点真实照片 */
    @GetMapping("/img/spot/{id}")
    public ResponseEntity<byte[]> spot(@PathVariable("id") Long id,
                                       @RequestParam(name = "n", defaultValue = "0") int n) {
        return respond(spotImageService.forSpot(id, n));
    }

    /** 按关键词取图(路线封面等) */
    @GetMapping("/img/kw")
    public ResponseEntity<byte[]> byKeyword(@RequestParam("kw") String keyword,
                                            @RequestParam(name = "n", defaultValue = "0") int n) {
        return respond(spotImageService.forKeyword(keyword, n));
    }

    private ResponseEntity<byte[]> respond(ImageData img) {
        MediaType type = MediaType.parseMediaType(
                img.contentType() == null ? "image/jpeg" : img.contentType());
        return ResponseEntity.ok()
                .contentType(type)
                // 浏览器缓存 7 天, 图片内容按 id/序号稳定
                .header(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(7, TimeUnit.DAYS).getHeaderValue())
                .body(img.bytes());
    }
}
