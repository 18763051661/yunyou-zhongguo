package cn.edu.qut.service;

import java.util.List;
import java.util.Map;

/**
 * VR 环景舱服务: 游览打卡 + 热度统计。
 */
public interface VrService {

    /** 记录一次 VR 游览, 返回该景点累计 VR 游览次数 */
    long record(Long spotId, String sessionId);

    /** 某景点累计 VR 游览次数 */
    long count(Long spotId);

    /** VR 热度榜(被 VR 游览最多的景点) */
    List<Map<String, Object>> hot(int n);

    /** 某会话最近的 VR 游览足迹 */
    List<Map<String, Object>> recentBySession(String sessionId, int n);
}
