<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>点评 - ${spot.name} - 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #0f766e, #0ea5e9); color: #fff; padding: 34px 20px; text-align: center; }
  .hero h1 { font-size: 22px; }
  .hero p { font-size: 13px; opacity: .9; margin-top: 6px; }
  .nav { text-align: center; margin: 18px 0 10px; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #0f766e; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #0f766e; color: #fff; }
  .container { max-width: 800px; margin: 0 auto; padding: 0 16px 30px; }
  .back { display: inline-block; font-size: 13px; color: #0ea5e9; text-decoration: none; margin-bottom: 10px; }
  .card { background: #fff; border-radius: 12px; padding: 18px 20px; margin-bottom: 16px; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
  .summary { display: flex; gap: 24px; align-items: center; }
  .summary .big { font-size: 42px; font-weight: 700; color: #0f766e; }
  .summary .big sub { font-size: 16px; color: #94a3b8; font-weight: 400; }
  .summary .total { color: #94a3b8; font-size: 13px; margin-top: 4px; }
  .bars { flex: 1; }
  .bar-row { display: flex; gap: 8px; align-items: center; font-size: 13px; margin-bottom: 4px; }
  .bar-row .lab { width: 36px; color: #64748b; }
  .bar-row .track { flex: 1; background: #e2e8f0; border-radius: 6px; height: 10px; overflow: hidden; }
  .bar-row .fill { height: 100%; background: linear-gradient(90deg, #14b8a6, #0ea5e9); }
  .bar-row .val { width: 32px; text-align: right; color: #94a3b8; font-size: 12px; }
  h3 { font-size: 15px; color: #0f766e; margin-bottom: 10px; }
  .cmt { border-bottom: 1px solid #f1f5f9; padding: 12px 0; }
  .cmt:last-child { border-bottom: none; }
  .cmt-head { display: flex; gap: 10px; align-items: center; margin-bottom: 4px; }
  .cmt-avatar { width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(135deg, #14b8a6, #0ea5e9); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; flex-shrink: 0; }
  .cmt-name { font-size: 14px; color: #334155; font-weight: 600; }
  .cmt-time { font-size: 12px; color: #94a3b8; margin-left: auto; }
  .stars { color: #f59e0b; font-size: 13px; }
  .cmt-body { font-size: 13px; color: #475569; line-height: 1.65; margin-top: 4px; padding-left: 44px; }
  .empty { text-align: center; color: #94a3b8; padding: 20px 0; font-size: 13px; }
  .pager { display: flex; justify-content: center; gap: 10px; padding: 14px 0 4px; }
  .pager a, .pager span { padding: 6px 14px; border-radius: 6px; font-size: 13px; text-decoration: none; }
  .pager a { background: #fff; color: #0f766e; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .pager a:hover { background: #0f766e; color: #fff; }
  .pager .cur { background: #0f766e; color: #fff; }
  .pager .dot { color: #cbd5e1; }
  .footer { text-align: center; padding: 18px; color: #94a3b8; font-size: 13px; }
</style>
</head>
<body>
  <div class="hero">
    <h1>${spot.name} · 点评</h1>
    <p>${spot.province} · ${spot.city} · ${spot.summary}</p>
  </div>
  <div class="nav">
    <a href="/page/home">首页</a>
    <a href="/page/spots">景点列表</a>
    <a href="/page/routes">路线列表</a>
    <a href="/page/stats">数据统计</a>
    <a href="/page/tour/records">我的足迹</a>
    <a href="/">返回 SPA</a>
  </div>
  <div class="container">
    <a class="back" href="/page/spots/${spot.id}">← 返回景点详情</a>

    <!-- 评分汇总卡片 -->
    <div class="card">
      <h3>游客评分汇总</h3>
      <div class="summary">
        <div>
          <div class="big">
            <fmt:formatNumber value="${spot.rating}" minFractionDigits="1" maxFractionDigits="1"/>
            <sub>/ 5.0</sub>
          </div>
          <div class="total">共 ${spot.ratingCount} 条游客点评</div>
        </div>
        <div class="bars">
          <c:forEach var="r" items="${summary.distribution}">
            <div class="bar-row">
              <div class="lab">${r.rating} 星</div>
              <div class="track"><div class="fill" style="width:${r.percent}%"></div></div>
              <div class="val">${r.count}</div>
            </div>
          </c:forEach>
        </div>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="card">
      <h3>游客点评 (${comments.total})</h3>
      <c:choose>
        <c:when test="${empty comments.list}">
          <div class="empty">暂无点评，来抢沙发的是你</div>
        </c:when>
        <c:otherwise>
          <c:forEach var="c" items="${comments.list}">
            <div class="cmt">
              <div class="cmt-head">
                <div class="cmt-avatar">
                  <c:out value="${c.nickname != null ? c.nickname.substring(0,1) : 'U'}"/>
                </div>
                <span class="cmt-name">${c.nickname == null ? '游客' : c.nickname}</span>
                <span class="stars">
                  <c:forEach begin="1" end="5" varStatus="s">
                    <c:out value="${s.index <= c.rating ? '★' : '☆'}"/>
                  </c:forEach>
                </span>
                <span class="cmt-time">
                  <fmt:formatDate value="${c.createdAt}" pattern="yyyy-MM-dd HH:mm"/>
                </span>
              </div>
              <div class="cmt-body">${c.content}</div>
            </div>
          </c:forEach>
        </c:otherwise>
      </c:choose>

      <!-- 分页 -->
      <c:if test="${comments.totalPages > 1}">
        <div class="pager">
          <c:if test="${page > 1}">
            <a href="/page/spots/${spot.id}/comments?page=${page-1}">上一页</a>
          </c:if>
          <c:forEach begin="1" end="${comments.totalPages}" varStatus="s">
            <c:choose>
              <c:when test="${s.index == page}">
                <span class="cur">${s.index}</span>
              </c:when>
              <c:when test="${s.index == 1 or s.index == comments.totalPages or (s.index >= page-2 and s.index <= page+2)}">
                <a href="/page/spots/${spot.id}/comments?page=${s.index}">${s.index}</a>
              </c:when>
              <c:otherwise>
                <span class="dot">…</span>
              </c:otherwise>
            </c:choose>
          </c:forEach>
          <c:if test="${page < comments.totalPages}">
            <a href="/page/spots/${spot.id}/comments?page=${page+1}">下一页</a>
          </c:if>
        </div>
      </c:if>
    </div>
  </div>
  <div class="footer">JSP 景点点评 | JSTL forEach 渲染评论列表 + fmt:formatNumber/formatDate 格式化 + c:choose/c:if 条件渲染</div>
</body>
</html>
