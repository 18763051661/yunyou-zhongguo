package cn.edu.qut.service;

import cn.edu.qut.common.BusinessException;
import cn.edu.qut.dao.UserDao;
import cn.edu.qut.entity.User;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public Map<String, Object> register(String username, String password, String nickname) {
        if (username == null || !username.matches("^[A-Za-z0-9_\\u4e00-\\u9fa5]{2,20}$")) {
            throw new BusinessException("用户名需为 2-20 位中英文、数字或下划线");
        }
        if (password == null || password.length() < 6 || password.length() > 30) {
            throw new BusinessException("密码长度需为 6-30 位");
        }
        if (userDao.findByUsername(username) != null) {
            throw new BusinessException("用户名已被注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname((nickname == null || nickname.isBlank()) ? username : nickname.trim());
        userDao.insert(user);
        return toView(user);
    }

    @Override
    public User login(String username, String password) {
        if (username == null || password == null) {
            throw new BusinessException("请输入用户名和密码");
        }
        User user = userDao.findByUsername(username.trim());
        if (user == null || !password.equals(user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public User getById(Long id) {
        User user = userDao.findById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public Map<String, Object> updateProfile(Long userId, String nickname, String preferenceTags) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname.trim());
        }
        if (preferenceTags != null) {
            user.setPreferenceTags(preferenceTags.trim());
        }
        userDao.updateProfile(user);
        return toView(user);
    }

    @Override
    public Map<String, Object> toView(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("preferenceTags", user.getPreferenceTags());
        return map;
    }
}
