package cn.edu.qut.dao;

import cn.edu.qut.entity.ScenicSpot;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 景点 DAO */
public interface SpotDao {

    ScenicSpot findById(Long id);

    List<ScenicSpot> findAllOnShelf();

    /**
     * 动态条件分页查询(全国)。sort 由 Service 白名单映射为 ORDER BY, 杜绝注入。
     */
    List<ScenicSpot> findPage(@Param("categoryId") Long categoryId,
                              @Param("province") String province,
                              @Param("region") String region,
                              @Param("freeOnly") Boolean freeOnly,
                              @Param("keyword") String keyword,
                              @Param("tag") String tag,
                              @Param("maxDifficulty") Integer maxDifficulty,
                              @Param("sort") String sort,
                              @Param("offset") int offset,
                              @Param("size") int size);

    long countPage(@Param("categoryId") Long categoryId,
                   @Param("province") String province,
                   @Param("region") String region,
                   @Param("freeOnly") Boolean freeOnly,
                   @Param("keyword") String keyword,
                   @Param("tag") String tag,
                   @Param("maxDifficulty") Integer maxDifficulty);

    List<ScenicSpot> findTopPopular(int n);

    List<ScenicSpot> findTopRating(int n);

    /** 云游盲盒: 随机抽取一个上架景点 */
    ScenicSpot findRandom();

    /** 省份聚合: 省份、景点数、平均评分、区域归属(用于全国导览与足迹地图) */
    List<Map<String, Object>> provinceAggregate();

    int applyNewRating(@Param("spotId") Long spotId, @Param("rating") int rating);

    // ==================== 管理端 ====================

    /** 管理端分页(含未上架), 关键词匹配名称/省份/城市 */
    List<ScenicSpot> findAdminPage(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    long countAdminPage(@Param("keyword") String keyword);

    int insert(ScenicSpot spot);

    int update(ScenicSpot spot);

    int deleteById(Long id);
}
