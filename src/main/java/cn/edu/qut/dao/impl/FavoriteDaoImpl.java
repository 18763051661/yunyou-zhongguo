package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.FavoriteDao;
import cn.edu.qut.entity.Favorite;
import cn.edu.qut.mapper.FavoriteMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 收藏 DAO 实现 */
@Repository
public class FavoriteDaoImpl implements FavoriteDao {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Override
    public int insert(@Param("userId") Long userId, @Param("spotId") Long spotId) {
        return favoriteMapper.insert(userId, spotId);
    }

    @Override
    public int delete(@Param("userId") Long userId, @Param("spotId") Long spotId) {
        return favoriteMapper.delete(userId, spotId);
    }

    @Override
    public int exists(@Param("userId") Long userId, @Param("spotId") Long spotId) {
        return favoriteMapper.exists(userId, spotId);
    }

    @Override
    public List<Favorite> findByUser(Long userId) {
        return favoriteMapper.findByUser(userId);
    }
}
