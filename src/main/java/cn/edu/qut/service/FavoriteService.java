package cn.edu.qut.service;

import java.util.List;
import java.util.Map;

/**
 * 景点收藏 Service
 */
public interface FavoriteService {

    /**
     * 切换用户对某景点的收藏状态，已收藏则取消，未收藏则新增。
     *
     * @param userId 用户 ID
     * @param spotId 景点 ID
     * @return 当前是否收藏及景点 ID
     */
    Map<String, Object> toggle(Long userId, Long spotId);

    /**
     * 查询某用户的收藏列表。
     *
     * @param userId 用户 ID
     * @return 收藏列表
     */
    List<Map<String, Object>> list(Long userId);
}
