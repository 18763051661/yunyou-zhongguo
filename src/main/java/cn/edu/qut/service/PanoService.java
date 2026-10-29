package cn.edu.qut.service;

import java.util.List;
import java.util.Map;

/**
 * 真 360° 立方体全景素材服务。
 * 为拥有真实全景素材的著名景点提供六面立方体贴图(f/r/b/l/u/d)的定位与元信息,
 * 供 VR 环景舱以立方体相机方式呈现真·360° 沉浸视角。
 */
public interface PanoService {

    /**
     * 查询某景点是否有真实全景素材。
     *
     * @param spotId 景点 ID
     * @return 有素材: {available:true, title, source, faces:[六面图URL]}; 无素材: {available:false}
     */
    Map<String, Object> findBySpot(Long spotId);

    /**
     * 全部支持真 360° 全景的景点清单(用于前端打标/展示)。
     *
     * @return [{spotId, name, title, source}]
     */
    List<Map<String, Object>> list();
}
