package cn.edu.qut.service;

import cn.edu.qut.dao.CategoryDao;
import cn.edu.qut.entity.Category;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDao categoryDao;

    public CategoryServiceImpl(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    @Override
    public List<Category> findAll() {
        return categoryDao.findAll();
    }

    @Override
    public String nameOf(Long categoryId) {
        if (categoryId == null) {
            return "";
        }
        Category category = categoryDao.findById(categoryId);
        return category == null ? "" : category.getName();
    }

    @Override
    public Map<Long, String> nameMap() {
        Map<Long, String> map = new LinkedHashMap<>();
        for (Category category : categoryDao.findAll()) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }
}
