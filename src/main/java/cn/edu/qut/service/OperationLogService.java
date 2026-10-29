package cn.edu.qut.service;

import cn.edu.qut.common.PageResult;
import cn.edu.qut.entity.OperationLog;

/**
 * 操作日志服务: AOP 切面写入 + 管理端分页查询。
 */
public interface OperationLogService {

    /** 记录一条操作日志(内部吞异常, 绝不影响业务主流程) */
    void record(OperationLog log);

    /** 分页查询(按时间倒序) */
    PageResult<OperationLog> page(int page, int size);
}
