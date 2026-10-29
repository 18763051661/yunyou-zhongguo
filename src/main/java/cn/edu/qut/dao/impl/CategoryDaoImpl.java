package cn.edu.qut.dao.impl;

import cn.edu.qut.dao.CategoryDao;
import cn.edu.qut.entity.Category;
import cn.edu.qut.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 分类 DAO 实现 */
@Repository
public class CategoryDaoImpl implements CategoryDao {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> findAll() {
        return categoryMapper.findAll();
    }

    @Override
    public Category findById(Long id) {
        return categoryMapper.findById(id);
    }
}
