package cn.edu.qut.support.web;

import cn.edu.qut.common.BusinessException;
import cn.edu.qut.entity.User;
import jakarta.servlet.http.HttpSession;

/**
 * 会话工具。
 */
public final class SessionUtils {

    private SessionUtils() {
    }

    /** 未登录抛 401 业务异常。 */
    public static User requireUser(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            throw new BusinessException(401, "请先登录后再操作");
        }
        return user;
    }

    /** 允许匿名, 返回 null。 */
    public static User currentUser(HttpSession session) {
        return (User) session.getAttribute("loginUser");
    }
}
