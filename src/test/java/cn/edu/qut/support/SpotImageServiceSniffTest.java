package cn.edu.qut.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 图片魔数识别单元测试: 保证图片服务不会被伪装的内容类型欺骗。
 */
class SpotImageServiceSniffTest {

    @Test
    void shouldIdentifyCommonImageMagicNumbers() {
        // JPEG: FF D8
        assertEquals("image/jpeg", SpotImageService.sniffType(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}));
        // PNG: 89 50 4E 47
        assertEquals("image/png", SpotImageService.sniffType(new byte[]{(byte) 0x89, 'P', 'N', 'G'}));
        // GIF: "GIF"
        assertEquals("image/gif", SpotImageService.sniffType("GIF89a".getBytes()));
    }

    @Test
    void shouldFallbackToJpegForUnknownOrNullContent() {
        // 网页文本(如防盗链拦截页)不能被当成合法图片类型
        assertEquals("image/jpeg", SpotImageService.sniffType("<html>forbidden</html>".getBytes()));
        assertEquals("image/jpeg", SpotImageService.sniffType(new byte[]{1, 2, 3}));
        // 空指针/空数组不抛异常
        assertEquals("image/jpeg", SpotImageService.sniffType(null));
        assertEquals("image/jpeg", SpotImageService.sniffType(new byte[0]));
    }
}
