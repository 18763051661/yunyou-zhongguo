package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.UserDao;
import cn.edu.qut.entity.User;
import cn.edu.qut.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/** 用户 DAO 实现 */
@Repository
public class UserDaoImpl implements UserDao {

    @Autowired
    private UserMapper userMapper;

    @Override
    public int insert(User user) {
        return userMapper.insert(user);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public int updateProfile(User user) {
        return userMapper.updateProfile(user);
    }
}
