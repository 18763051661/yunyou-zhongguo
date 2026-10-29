<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>我的订单 - 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #0e9f6e, #34d399); color: #fff; padding: 34px 20px; text-align: center; }
  .hero h1 { font-size: 22px; }
  .hero p { font-size: 13px; opacity: .9; margin-top: 6px; }
  .nav { text-align: center; margin: 18px 0 10px; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #0e9f6e; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #0e9f6e; color: #fff; }
  .container { max-width: 800px; margin: 0 auto; padding: 0 16px 30px; }

  /* 统计条 */
  .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 16px; }
  .sum-card { background: #fff; border-radius: 12px; padding: 14px 8px; text-align: center; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
  .sum-card .num { font-size: 22px; font-weight: 700; color: #0e9f6e; }
  .sum-card .lbl { font-size: 12px; color: #94a3b8; margin-top: 4px; }

  /* 票根式卡片 */
  .ticket {
    display: flex; background: #fff; border-radius: 12px; margin-bottom: 14px;
    box-shadow: 0 2px 10px rgba(0,0,0,.06); overflow: hidden;
  }
  .ticket.stub-cancel .tk-stub { background: linear-gradient(160deg, #cbd5d1, #e2e8e6); }
  .tk-stub {
    width: 100px; flex-shrink: 0; background: linear-gradient(160deg, #0e9f6e, #34d399);
    color: #fff; display: flex; flex-direction: column; justify-content: center; align-items: center;
    gap: 4px; padding: 14px 6px; position: relative;
  }
  .tk-stub::after {
    content: ""; position: absolute; right: -7px; top: 50%; transform: translateY(-50%);
    width: 14px; height: 14px; border-radius: 50%; background: #f0f4f8;
  }
  .tk-stub-name { font-size: 12px; font-weight: 700; text-align: center; line-height: 1.3; }
  .tk-stub-price { font-size: 18px; font-weight: 800; margin-top: 4px; }
  .tk-stub-no { font-size: 8.5px; opacity: .75; font-family: monospace; word-break: break-all; text-align: center; }
  .tk-main { flex: 1; padding: 14px 18px; border-left: 2px dashed #e2e8f0; }
  .tk-top { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
  .tk-top b { font-size: 15px; }
  .tk-no { margin-left: auto; font-size: 11px; color: #94a3b8; font-family: monospace; }
  .tk-status { font-size: 11px; padding: 3px 10px; border-radius: 999px; font-weight: 600; }
  .tk-status.ok { background: #def7ec; color: #057a55; }
  .tk-status.wait { background: #fff6e5; color: #b2730a; }
  .tk-status.off { background: #f1f3f2; color: #9aa5a0; }
  .tk-info { font-size: 12.5px; color: #4b5563; margin-top: 8px; line-height: 1.7; }
  .tk-amount { color: #d84315; font-weight: 700; }
  .tk-foot { margin-top: 8px; font-size: 11.5px; color: #94a3b8; }

  h3 { font-size: 15px; color: #0e9f6e; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
  h3::before { content: ""; width: 4px; height: 16px; border-radius: 2px; background: #0e9f6e; }
  .empty { text-align: center; color: #94a3b8; padding: 30px 0; font-size: 13px; }
  .footer { text-align: center; padding: 18px; color: #94a3b8; font-size: 13px; }
</style>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
</head>
<body>
  <div class="hero">
    <h1>🧾 我的门票订单</h1>
    <p>Spring AOP 自动审计 · 票根式 UI · 状态机 PENDING / PAID / CANCELED</p>
  </div>
  <div class="nav">
    <a href="/page/home">首页</a>
    <a href="/page/spots">景点列表</a>
    <a href="/page/routes">路线列表</a>
    <a href="/page/stats">数据统计</a>
    <a href="/page/tour/records">我的足迹</a>
    <a href="/page/orders">我的订单</a>
    <a href="/">返回 SPA</a>
  </div>
  <div class="container">

    <!-- 消费统计 (JSTL c:set + fmt:formatNumber) -->
    <c:set var="count" value="${orders.size()}" />
    <div class="summary">
      <div class="sum-card"><div class="num">¥<fmt:formatNumber value="${paidTotal}" pattern="0.00"/></div><div class="lbl">累计消费</div></div>
      <div class="sum-card"><div class="num">${count}</div><div class="lbl">全部订单</div></div>
      <div class="sum-card"><div class="num">${ticketCount}</div><div class="lbl">持票张数</div></div>
      <div class="sum-card"><div class="num" style="${pendingCount > 0 ? 'color:#b2730a' : ''}">${pendingCount}</div><div class="lbl">待支付</div></div>
    </div>

    <!-- 订单列表 (JSTL forEach varStatus + c:choose 状态徽章) -->
    <h3>订单列表 <small style="font-weight:400;color:#94a3b8;font-size:12px">共 ${count} 笔</small></h3>
    <c:choose>
      <c:when test="${empty orders}">
        <div class="empty">还没有订单 · 去 SPA 首页挑个景点买票吧 🎫</div>
      </c:when>
      <c:otherwise>
        <c:forEach items="${orders}" var="o" varStatus="i">
          <div class="ticket ${o.payStatus == 'CANCELED' ? 'stub-cancel' : ''}">
            <!-- 存根区 -->
            <div class="tk-stub">
              <div class="tk-stub-name"><c:out value="${o.spotName}" /></div>
              <div class="tk-stub-price">¥<fmt:formatNumber value="${o.totalAmount}" pattern="0.00"/></div>
              <div class="tk-stub-no"><c:out value="${o.orderNo}" /></div>
            </div>
            <!-- 详情区 -->
            <div class="tk-main">
              <div class="tk-top">
                <b>🎫 <c:out value="${o.spotName}" /></b>
                <c:choose>
                  <c:when test="${o.payStatus == 'PAID'}">
                    <span class="tk-status ok">✅ 已支付</span>
                  </c:when>
                  <c:when test="${o.payStatus == 'CANCELED'}">
                    <span class="tk-status off">已取消</span>
                  </c:when>
                  <c:otherwise>
                    <span class="tk-status wait">⏳ 待支付</span>
                  </c:otherwise>
                </c:choose>
                <span class="tk-no">#${i.index + 1}</span>
              </div>
              <div class="tk-info">
                ${o.quantity} 张 × ¥<fmt:formatNumber value="${o.unitPrice}" pattern="0.00"/>
                · 合计 <span class="tk-amount">¥<fmt:formatNumber value="${o.totalAmount}" pattern="0.00"/></span>
                <c:if test="${not empty o.visitDate}"> · 游玩 <c:out value="${o.visitDate}" /></c:if>
                <c:if test="${not empty o.contactName}"> · 联系人 <c:out value="${o.contactName}" /></c:if>
              </div>
              <div class="tk-foot">下单于 <c:out value="${o.createdAt}" /> · Spring AOP 自动记录审计日志</div>
            </div>
          </div>
        </c:forEach>
      </c:otherwise>
    </c:choose>

  </div>
  <div class="footer">JSP 门票订单 · JSTL forEach varStatus + c:choose + fmt:formatNumber + c:set</div>
</body>
</html>
