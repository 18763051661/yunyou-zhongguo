package cn.edu.qut.service;

import cn.edu.qut.entity.User;

import java.util.Map;

/**
 * 用户 Service
 */
public interface UserService {

    /**
     * 注册新用户。
     *
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     * @return 用户视图信息
     */
    Map<String, Object> register(String username, String password, String nickname);

    /**
     * 用户登录，校验用户名和密码。
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录用户信息(不含密码)
     */
    User login(String username, String password);

    /**
     * 根据用户 ID 查询用户信息(不含密码)。
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    User getById(Long id);

    /**
     * 更新用户资料。
     *
     * @param userId          用户 ID
     * @param nickname        昵称
     * @param preferenceTags  偏好标签
     * @return 用户视图信息
     */
    Map<String, Object> updateProfile(Long userId, String nickname, String preferenceTags);

    /**
     * 将用户实体转换为对外视图(不包含密码等敏感字段)。
     *
     * @param user 用户实体
     * @return 用户视图信息
     */
    Map<String, Object> toView(User user);
}
