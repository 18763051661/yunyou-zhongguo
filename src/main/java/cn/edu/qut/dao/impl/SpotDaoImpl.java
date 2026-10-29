package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.SpotDao;
import cn.edu.qut.entity.ScenicSpot;
import cn.edu.qut.mapper.SpotMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 景点 DAO 实现 */
@Repository
public class SpotDaoImpl implements SpotDao {

    @Autowired
    private SpotMapper spotMapper;

    @Override
    public ScenicSpot findById(Long id) {
        return spotMapper.findById(id);
    }

    @Override
    public List<ScenicSpot> findAllOnShelf() {
        return spotMapper.findAllOnShelf();
    }

    @Override
    public List<ScenicSpot> findPage(@Param("categoryId") Long categoryId,
                                      @Param("province") String province,
                                      @Param("region") String region,
                                      @Param("freeOnly") Boolean freeOnly,
                                      @Param("keyword") String keyword,
                                      @Param("tag") String tag,
                                      @Param("maxDifficulty") Integer maxDifficulty,
                                      @Param("sort") String sort,
                                      @Param("offset") int offset,
                                      @Param("size") int size) {
        return spotMapper.findPage(categoryId, province, region, freeOnly, keyword, tag, maxDifficulty, sort, offset, size);
    }

    @Override
    public long countPage(@Param("categoryId") Long categoryId,
                           @Param("province") String province,
                           @Param("region") String region,
                           @Param("freeOnly") Boolean freeOnly,
                           @Param("keyword") String keyword,
                           @Param("tag") String tag,
                           @Param("maxDifficulty") Integer maxDifficulty) {
        return spotMapper.countPage(categoryId, province, region, freeOnly, keyword, tag, maxDifficulty);
    }

    @Override
    public List<ScenicSpot> findTopPopular(int n) {
        return spotMapper.findTopPopular(n);
    }

    @Override
    public List<ScenicSpot> findTopRating(int n) {
        return spotMapper.findTopRating(n);
    }

    @Override
    public ScenicSpot findRandom() {
        return spotMapper.findRandom();
    }

    @Override
    public List<Map<String, Object>> provinceAggregate() {
        return spotMapper.provinceAggregate();
    }

    @Override
    public int applyNewRating(@Param("spotId") Long spotId, @Param("rating") int rating) {
        return spotMapper.applyNewRating(spotId, rating);
    }

    // ==================== 管理端 ====================

    @Override
    public List<ScenicSpot> findAdminPage(String keyword, int offset, int size) {
        return spotMapper.findAdminPage(keyword, offset, size);
    }

    @Override
    public long countAdminPage(String keyword) {
        return spotMapper.countAdminPage(keyword);
    }

    @Override
    public int insert(ScenicSpot spot) {
        return spotMapper.insert(spot);
    }

    @Override
    public int update(ScenicSpot spot) {
        return spotMapper.update(spot);
    }

    @Override
    public int deleteById(Long id) {
        return spotMapper.deleteById(id);
    }
}
