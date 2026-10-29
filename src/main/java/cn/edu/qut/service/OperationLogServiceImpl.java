package cn.edu.qut.service;

import cn.edu.qut.common.PageResult;
import cn.edu.qut.entity.OperationLog;
import cn.edu.qut.mapper.OperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void record(OperationLog entry) {
        try {
            operationLogMapper.insert(entry);
        } catch (Exception e) {
            // 审计日志绝不能影响业务主流程: 失败仅记录错误日志
            log.error("写入操作日志失败: {} {}", entry, e.getMessage(), e);
        }
    }

    @Override
    public PageResult<OperationLog> page(int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        long total = operationLogMapper.count();
        List<OperationLog> list = total == 0 ? List.of() : operationLogMapper.page((p - 1) * s, s);
        return new PageResult<>(list, total, p, s);
    }
}
