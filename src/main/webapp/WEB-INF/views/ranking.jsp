<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>景点排行榜 - 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #f59e0b, #ea580c); color: #fff; padding: 30px 20px; text-align: center; }
  .hero h1 { font-size: 24px; }
  .hero p { font-size: 13px; opacity: .9; margin-top: 6px; }
  .nav { text-align: center; margin: 18px 0; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #ea580c; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #ea580c; color: #fff; }
  .container { max-width: 900px; margin: 0 auto; padding: 0 16px 30px; }
  .card { background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
  .card h2 { font-size: 17px; color: #ea580c; margin-bottom: 14px; border-bottom: 2px solid #fef3c7; padding-bottom: 8px; }
  .rank-row { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 8px; transition: background .15s; }
  .rank-row:hover { background: #fffbeb; }
  .rank-no { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; flex-shrink: 0; }
  .rank-no.gold { background: linear-gradient(135deg, #fbbf24, #f59e0b); color: #fff; }
  .rank-no.silver { background: linear-gradient(135deg, #cbd5e1, #94a3b8); color: #fff; }
  .rank-no.bronze { background: linear-gradient(135deg, #d97706, #b45309); color: #fff; }
  .rank-no.normal { background: #f1f5f9; color: #64748b; }
  .rank-info { flex: 1; }
  .rank-info b { font-size: 15px; }
  .rank-info span { display: block; font-size: 12px; color: #94a3b8; margin-top: 2px; }
  .rank-score { font-size: 18px; font-weight: 700; }
  .rank-score.rating { color: #f59e0b; }
  .rank-score.popular { color: #ea580c; }
  .rank-tags { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 4px; }
  .rank-tags .tag { font-size: 11px; padding: 2px 6px; border-radius: 4px; background: #fef3c7; color: #92400e; }
  .rank-tags .free { background: #d1fae5; color: #065f46; }
  .footer { text-align: center; padding: 18px; color: #94a3b8; font-size: 13px; }
</style>
</head>
<body>
  <div class="hero">
    <h1>景点排行榜</h1>
    <p>JSTL c:forEach varStatus 索引 + c:choose 奖牌等级 + fmt:formatNumber 评分格式化</p>
  </div>
  <div class="nav">
    <a href="/page/home">首页</a>
    <a href="/page/spots">景点列表</a>
    <a href="/page/ranking">排行榜</a>
    <a href="/page/provinces">省份导览</a>
    <a href="/page/favorites">收藏夹</a>
    <a href="/page/about">关于</a>
    <a href="/">返回 SPA</a>
  </div>
  <div class="container">

    <!-- 高评分 Top 10: c:forEach varStatus + c:choose 奖牌 -->
    <div class="card">
      <h2>高评分景点 Top 10</h2>
      <c:forEach var="spot" items="${topRating}" varStatus="vs">
        <c:set var="rank" value="${vs.index + 1}" />
        <div class="rank-row">
          <c:choose>
            <c:when test="${rank == 1}">
              <div class="rank-no gold">1</div>
            </c:when>
            <c:when test="${rank == 2}">
              <div class="rank-no silver">2</div>
            </c:when>
            <c:when test="${rank == 3}">
              <div class="rank-no bronze">3</div>
            </c:when>
            <c:otherwise>
              <div class="rank-no normal">${rank}</div>
            </c:otherwise>
          </c:choose>
          <div class="rank-info">
            <b>${spot.name}</b>
            <span>${spot.province} ${spot.city} · ${spot.categoryName}</span>
            <div class="rank-tags">
              <c:choose>
                <c:when test="${spot.price == 0}">
                  <span class="tag free">免费</span>
                </c:when>
                <c:otherwise>
                  <span class="tag">¥<fmt:formatNumber value="${spot.price}" pattern="#" /></span>
                </c:otherwise>
              </c:choose>
              <c:if test="${not empty spot.tags}">
                <c:forEach var="t" items="${spot.tags}" end="1">
                  <span class="tag">${t}</span>
                </c:forEach>
              </c:if>
            </div>
          </div>
          <div class="rank-score rating">★ <fmt:formatNumber value="${spot.rating}" pattern="0.0" /></div>
        </div>
      </c:forEach>
    </div>

    <!-- 热门 Top 10: 按人气排序 -->
    <div class="card">
      <h2>热门景点 Top 10 (按人气值)</h2>
      <c:forEach var="spot" items="${topPopular}" varStatus="vs">
        <c:set var="rank" value="${vs.index + 1}" />
        <div class="rank-row">
          <c:choose>
            <c:when test="${rank le 3}">
              <div class="rank-no ${rank == 1 ? 'gold' : (rank == 2 ? 'silver' : 'bronze')}">${rank}</div>
            </c:when>
            <c:otherwise>
              <div class="rank-no normal">${rank}</div>
            </c:otherwise>
          </c:choose>
          <div class="rank-info">
            <b>${spot.name}</b>
            <span>${spot.province} ${spot.city}</span>
          </div>
          <div class="rank-score popular">🔥 ${spot.popularity}</div>
        </div>
      </c:forEach>
    </div>

  </div>
  <div class="footer">JSP 景点排行榜 | JSTL c:forEach varStatus 索引 + c:choose 条件渲染奖牌 + fmt:formatNumber 格式化</div>
</body>
</html>
