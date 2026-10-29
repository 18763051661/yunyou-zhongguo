package cn.edu.qut.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Haversine 球面距离单元测试: 保障"附近推荐"的距离计算正确。
 */
class SpotDistanceTest {

    private final SpotServiceImpl service = new SpotServiceImpl(null, null, null, null);

    @Test
    void beijingToShanghaiShouldBeAbout1067Km() {
        // 北京天安门 (39.9042, 116.4074) → 上海人民广场 (31.2304, 121.4737), 理论直线约 1067 km
        double d = service.haversine(39.9042, 116.4074, 31.2304, 121.4737);
        assertTrue(d > 1010 && d < 1120, "北京→上海距离应约 1067km, 实际=" + d);
    }

    @Test
    void distanceShouldBeZeroForSamePointAndSymmetric() {
        assertEquals(0.0, service.haversine(36.06, 120.38, 36.06, 120.38), 1e-6);
        double ab = service.haversine(36.06, 120.38, 31.23, 121.47);
        double ba = service.haversine(31.23, 121.47, 36.06, 120.38);
        assertEquals(ab, ba, 1e-9, "距离计算应满足对称性");
    }
}
