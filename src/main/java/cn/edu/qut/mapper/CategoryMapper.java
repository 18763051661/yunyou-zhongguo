package cn.edu.qut.mapper;

import cn.edu.qut.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("select * from category order by sort_no asc, id asc")
    List<Category> findAll();

    @Select("select * from category where id = #{id}")
    Category findById(Long id);
}
