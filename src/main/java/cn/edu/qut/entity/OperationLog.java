package cn.edu.qut.entity;

import java.time.LocalDateTime;

/**
 * 管理端操作日志(由 AOP 切面 AuditLogAspect 自动写入)。
 */
public class OperationLog {

    private Long id;
    private String operator;   // 操作人
    private String action;     // 动作: 新增景点/修改景点/删除景点
    private String target;     // 操作对象: 景点名称 + ID
    private String detail;     // 触发方法
    private String ip;         // 来源 IP
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
