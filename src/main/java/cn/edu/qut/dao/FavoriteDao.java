package cn.edu.qut.dao;

import cn.edu.qut.entity.Favorite;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 收藏 DAO */
public interface FavoriteDao {

    int insert(@Param("userId") Long userId, @Param("spotId") Long spotId);

    int delete(@Param("userId") Long userId, @Param("spotId") Long spotId);

    int exists(@Param("userId") Long userId, @Param("spotId") Long spotId);

    List<Favorite> findByUser(Long userId);
}
