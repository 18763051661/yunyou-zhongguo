package cn.edu.qut.dao;

import cn.edu.qut.entity.User;

/** 用户 DAO */
public interface UserDao {

    int insert(User user);

    User findByUsername(String username);

    User findById(Long id);

    int updateProfile(User user);
}
