package cn.edu.qut.mapper;

import cn.edu.qut.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Insert("insert into sys_user (username, password, nickname, avatar, preference_tags) "
            + "values (#{username}, #{password}, #{nickname}, #{avatar}, #{preferenceTags})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("select * from sys_user where username = #{username}")
    User findByUsername(String username);

    @Select("select * from sys_user where id = #{id}")
    User findById(Long id);

    @Update("update sys_user set preference_tags = #{preferenceTags}, nickname = #{nickname} where id = #{id}")
    int updateProfile(User user);
}
