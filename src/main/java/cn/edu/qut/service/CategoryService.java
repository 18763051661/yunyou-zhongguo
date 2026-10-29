package cn.edu.qut.service;

import cn.edu.qut.entity.Category;

import java.util.List;
import java.util.Map;

/**
 * 景点分类 Service
 */
public interface CategoryService {

    /**
     * 查询全部分类。
     *
     * @return 分类列表
     */
    List<Category> findAll();

    /**
     * 根据分类 ID 查询分类名称，找不到时返回空字符串。
     *
     * @param categoryId 分类 ID
     * @return 分类名称
     */
    String nameOf(Long categoryId);

    /**
     * 构建分类 ID 到分类名称的映射。
     *
     * @return 分类 ID -> 分类名称
     */
    Map<Long, String> nameMap();
}
