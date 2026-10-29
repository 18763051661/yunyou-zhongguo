package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.TourRecordDao;
import cn.edu.qut.entity.TourRecord;
import cn.edu.qut.mapper.TourRecordMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 游览记录 DAO 实现 */
@Repository
public class TourRecordDaoImpl implements TourRecordDao {

    @Autowired
    private TourRecordMapper tourRecordMapper;

    @Override
    public int exists(@Param("sessionId") String sessionId, @Param("spotId") Long spotId) {
        return tourRecordMapper.exists(sessionId, spotId);
    }

    @Override
    public int insert(TourRecord record) {
        return tourRecordMapper.insert(record);
    }

    @Override
    public int countVisitedInRoute(@Param("sessionId") String sessionId, @Param("routeId") Long routeId) {
        return tourRecordMapper.countVisitedInRoute(sessionId, routeId);
    }

    @Override
    public List<TourRecord> findBySession(String sessionId) {
        return tourRecordMapper.findBySession(sessionId);
    }

    @Override
    public int countSpotsBySession(String sessionId) {
        return tourRecordMapper.countSpotsBySession(sessionId);
    }

    @Override
    public List<Map<String, Object>> provinceFootprint(String sessionId) {
        return tourRecordMapper.provinceFootprint(sessionId);
    }
}
