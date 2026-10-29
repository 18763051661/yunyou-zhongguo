package cn.edu.qut.dao;

import cn.edu.qut.entity.TourRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 游览记录 DAO */
public interface TourRecordDao {

    int exists(@Param("sessionId") String sessionId, @Param("spotId") Long spotId);

    int insert(TourRecord record);

    int countVisitedInRoute(@Param("sessionId") String sessionId, @Param("routeId") Long routeId);

    List<TourRecord> findBySession(String sessionId);

    int countSpotsBySession(String sessionId);

    List<Map<String, Object>> provinceFootprint(String sessionId);
}
