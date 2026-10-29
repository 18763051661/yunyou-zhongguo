package cn.edu.qut.dao;

import cn.edu.qut.entity.Category;

import java.util.List;

/** 景点分类 DAO */
public interface CategoryDao {

    List<Category> findAll();

    Category findById(Long id);
}
