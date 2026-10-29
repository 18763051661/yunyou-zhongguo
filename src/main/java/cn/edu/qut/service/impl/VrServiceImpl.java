package cn.edu.qut.service.impl;

import cn.edu.qut.entity.VrViewLog;
import cn.edu.qut.mapper.VrViewLogMapper;
import cn.edu.qut.service.VrService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * VR 环景舱服务实现: 每次进入 VR 全景落库一条打卡, 支撑热度榜与足迹。
 */
@Service
public class VrServiceImpl implements VrService {

    private final VrViewLogMapper vrMapper;

    public VrServiceImpl(VrViewLogMapper vrMapper) {
        this.vrMapper = vrMapper;
    }

    @Override
    public long record(Long spotId, String sessionId) {
        VrViewLog log = new VrViewLog();
        log.setSpotId(spotId);
        log.setSessionId(sessionId);
        vrMapper.insert(log);
        return vrMapper.countBySpot(spotId);
    }

    @Override
    public long count(Long spotId) {
        return vrMapper.countBySpot(spotId);
    }

    @Override
    public List<Map<String, Object>> hot(int n) {
        return vrMapper.hot(Math.max(1, Math.min(n, 20)));
    }

    @Override
    public List<Map<String, Object>> recentBySession(String sessionId, int n) {
        return vrMapper.recentBySession(sessionId, Math.max(1, Math.min(n, 50)));
    }
}
