package cn.edu.qut.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 分页计算单元测试: 保障管理端/前台分页页码正确。
 */
class PageResultTest {

    @Test
    void shouldComputeTotalPagesCorrectly() {
        // 95 条数据, 每页 20 条 → 5 页
        PageResult<String> r = new PageResult<>(List.of("a"), 95, 3, 20);
        assertEquals(95, r.getTotal());
        assertEquals(3, r.getPage());
        assertEquals(5, r.getTotalPages());
        assertEquals(1, r.getList().size());
        // 整除边界: 100 条每页 20 → 恰好 5 页
        assertEquals(5, new PageResult<>(List.of(), 100, 1, 20).getTotalPages());
    }

    @Test
    void shouldGuardAgainstInvalidPageSize() {
        // size=0 时不应抛除零异常, totalPages 兜底为 0
        assertEquals(0, new PageResult<>(List.of(), 95, 1, 0).getTotalPages());
    }
}
