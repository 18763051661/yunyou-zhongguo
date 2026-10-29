package cn.edu.qut.service;

import cn.edu.qut.entity.TravelPlan;
import cn.edu.qut.entity.TravelPreference;

import java.util.List;

/** 智能出游规划: 用户偏好问卷 → AI 生成个性化路线 */
public interface TravelPlannerService {

    /** 提交偏好问卷, 触发 AI 生成, 存库并返回路线 */
    TravelPlan generate(TravelPreference pref);

    /** 查某条 AI 路线 */
    TravelPlan plan(Long id);

    /** 查最近 8 条 AI 路线(首页展示) */
    List<TravelPlan> recentPlans();

    /** 查某问卷对应的最新路线 */
    TravelPlan planByPref(Long prefId);
}
