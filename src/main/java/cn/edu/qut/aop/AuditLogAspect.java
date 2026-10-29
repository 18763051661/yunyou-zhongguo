package cn.edu.qut.aop;

import cn.edu.qut.entity.OperationLog;
import cn.edu.qut.entity.ScenicSpot;
import cn.edu.qut.entity.TicketOrder;
import cn.edu.qut.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 管理端操作审计切面(Spring AOP)。
 * 无侵入拦截景点管理写操作(Service 实现层, 根容器代理), 自动写入 operation_log,
 * 演示 AOP 在真实业务中的应用 —— 审计日志与业务代码完全解耦。
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final OperationLogService operationLogService;

    public AuditLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /** 新增/修改景点切点(有返回值) */
    @Pointcut("execution(* cn.edu.qut.service.SpotServiceImpl.adminCreate(..)) "
            + "|| execution(* cn.edu.qut.service.SpotServiceImpl.adminUpdate(..))")
    public void adminSpotSave() {
    }

    /** 删除景点切点(void, 单独通知避免 returning 绑定歧义) */
    @Pointcut("execution(* cn.edu.qut.service.SpotServiceImpl.adminDelete(..))")
    public void adminSpotDelete() {
    }

    /** 订单域切点: 创建 / 支付 / 取消(退款) / 删除 (OrderServiceImpl 在 service 包, 无 impl 子包) */
    @Pointcut("execution(* cn.edu.qut.service.OrderServiceImpl.create(..)) "
            + "|| execution(* cn.edu.qut.service.OrderServiceImpl.pay(..)) "
            + "|| execution(* cn.edu.qut.service.OrderServiceImpl.cancel(..))")
    public void orderWrite() {
    }

    /** 订单删除切点 (返回 int, 单独通知避免 returning 绑定歧义) */
    @Pointcut("execution(* cn.edu.qut.service.OrderServiceImpl.delete(..))")
    public void orderDelete() {
    }

    @AfterReturning(pointcut = "adminSpotSave()", returning = "result")
    public void afterSave(JoinPoint jp, Object result) {
        boolean isCreate = jp.getSignature().getName().endsWith("Create");
        ScenicSpot spot = result instanceof ScenicSpot s ? s
                : (jp.getArgs().length > 0 && jp.getArgs()[0] instanceof ScenicSpot a ? a : null);
        String action = isCreate ? "新增景点" : "修改景点";
        String target = spot == null ? "(未知)" : spot.getName() + " #" + spot.getId();
        writeLog(action, target, jp);
    }

    @AfterReturning("adminSpotDelete() && args(id)")
    public void afterDelete(JoinPoint jp, Long id) {
        writeLog("删除景点", "#" + id, jp);
    }

    /** 订单域写操作审计: create/pay/cancel(退款) 成功后自动落库 */
    @AfterReturning(pointcut = "orderWrite()", returning = "order")
    public void afterOrderWrite(JoinPoint jp, TicketOrder order) {
        String name = jp.getSignature().getName();
        String action = switch (name) {
            case "create" -> "创建门票订单";
            case "pay"    -> "支付门票订单";
            case "cancel" -> order != null && "CANCELED".equals(order.getPayStatus()) ? "取消/退款门票订单" : "取消门票订单";
            default       -> "订单操作";
        };
        String target = order == null ? "(未知)" : order.getSpotName() + " · " + order.getOrderNo();
        writeLog(action, target, jp);
    }

    /** 订单删除审计 */
    @AfterReturning("orderDelete() && args(orderNo)")
    public void afterOrderDelete(JoinPoint jp, String orderNo) {
        writeLog("删除订单记录", orderNo, jp);
    }

    private void writeLog(String action, String target, JoinPoint jp) {
        try {
            OperationLog entry = new OperationLog();
            entry.setOperator(currentAdmin());
            entry.setAction(action);
            entry.setTarget(target);
            entry.setDetail(jp.getSignature().toShortString());
            entry.setIp(currentIp());
            entry.setCreatedAt(LocalDateTime.now());
            operationLogService.record(entry);
        } catch (Exception e) {
            // 双保险: 切面层任何异常都不允许影响业务主流程
            log.warn("操作日志切面异常(已忽略): {}", e.getMessage());
        }
    }

    private String currentAdmin() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "system";
        }
        HttpSession session = attrs.getRequest().getSession(false);
        Object admin = session == null ? null : session.getAttribute("adminUser");
        return admin == null ? "anonymous" : String.valueOf(admin);
    }

    private String currentIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "-";
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        return ip == null || ip.isBlank() ? request.getRemoteAddr() : ip.split(",")[0].trim();
    }
}
