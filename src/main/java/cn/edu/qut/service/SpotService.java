package cn.edu.qut.service;

import cn.edu.qut.common.PageResult;
import cn.edu.qut.entity.ScenicSpot;
import cn.edu.qut.vo.SpotVO;

import java.util.List;
import java.util.Map;

/**
 * 景点 Service
 */
public interface SpotService {

    /**
     * 分页查询景点列表(支持分类、省份、关键词、标签、难度等多条件筛选与排序)。
     *
     * @param categoryId    分类 ID
     * @param province      省份
     * @param region        区域
     * @param freeOnly      仅免费
     * @param keyword       关键词
     * @param tag           标签
     * @param maxDifficulty 最大难度
     * @param sort           排序方式
     * @param page           页码
     * @param size           每页数量
     * @param userId         当前用户 ID(用于标记收藏状态, 可为 null)
     * @return 分页结果
     */
    PageResult<SpotVO> page(Long categoryId, String province, String region, Boolean freeOnly,
                            String keyword, String tag, Integer maxDifficulty,
                            String sort, Integer page, Integer size, Long userId);

    /**
     * 查询景点详情, 若景点不存在或已下架则抛出 404。
     *
     * @param id     景点 ID
     * @param userId 当前用户 ID(用于标记收藏状态, 可为 null)
     * @return 景点视图
     */
    SpotVO detail(Long id, Long userId);

    /**
     * 热门景点 Top N。
     *
     * @param n 数量
     * @return 景点列表
     */
    List<SpotVO> topPopular(int n);

    /**
     * 高评分景点 Top N。
     *
     * @param n 数量
     * @return 景点列表
     */
    List<SpotVO> topRating(int n);

    /** 云游盲盒: 随机抽取一个上架景点 */
    ScenicSpot randomOnShelf();

    /**
     * 全部在架景点列表。
     *
     * @return 景点列表
     */
    List<SpotVO> all();

    /**
     * 全国省份聚合导览。
     *
     * @return 省份聚合数据
     */
    List<Map<String, Object>> provinces();

    /**
     * 基于经纬度的"附近推荐"(Haversine 球面距离, km)。
     *
     * @param lng 经度
     * @param lat 纬度
     * @param n   返回数量
     * @return 附近景点及距离列表
     */
    List<Map<String, Object>> nearby(double lng, double lat, int n);

    // ==================== 管理端 ====================

    /** 管理端分页(含未上架景点) */
    PageResult<ScenicSpot> adminPage(String keyword, Integer page, Integer size);

    /** 新增景点(名称必填) */
    ScenicSpot adminCreate(ScenicSpot spot);

    /** 更新景点(必须已存在) */
    ScenicSpot adminUpdate(ScenicSpot spot);

    /** 删除景点 */
    void adminDelete(Long id);
}
