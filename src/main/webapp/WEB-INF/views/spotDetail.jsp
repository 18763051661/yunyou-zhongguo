<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>${spot.name} - 云游中国</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
    .header { background: linear-gradient(135deg, #109e6e, #0ea5e9); color: #fff; padding: 20px; text-align: center; }
    .header h1 { font-size: 22px; }
    .nav { text-align: center; margin: 20px 0; }
    .nav a { display: inline-block; padding: 8px 16px; margin: 0 6px; border-radius: 8px; background: #fff; color: #109e6e; text-decoration: none; font-size: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
    .nav a:hover { background: #109e6e; color: #fff; }
    .container { max-width: 900px; margin: 0 auto; padding: 20px; }
    .detail-card { background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
    .detail-card img { width: 100%; height: 400px; object-fit: cover; }
    .detail-body { padding: 24px; }
    .detail-body h2 { font-size: 24px; margin-bottom: 8px; }
    .meta-row { display: flex; flex-wrap: wrap; gap: 12px; font-size: 14px; color: #64748b; margin-bottom: 16px; }
    .meta-row span { display: inline-flex; align-items: center; gap: 4px; }
    .tag-row { margin: 12px 0; }
    .tag { display: inline-block; font-size: 12px; padding: 3px 10px; border-radius: 6px; background: #e0f2fe; color: #0369a1; margin-right: 6px; }
    .highlight-box { background: #f0fdf4; border-left: 4px solid #16a34a; padding: 14px 18px; border-radius: 8px; margin: 16px 0; }
    .highlight-box h4 { font-size: 14px; color: #16a34a; margin-bottom: 8px; }
    .highlight-box ul { padding-left: 20px; font-size: 14px; line-height: 1.8; }
    .desc { font-size: 14px; line-height: 1.9; color: #475569; margin: 16px 0; }
    .price-box { display: flex; align-items: center; gap: 16px; margin: 16px 0; padding: 14px 20px; background: #fff7ed; border-radius: 10px; }
    .price-box .price { font-size: 28px; font-weight: 700; color: #ea580c; }
    .price-box .price.free { color: #16a34a; }
    .price-box .label { font-size: 13px; color: #94a3b8; }
    .footer { text-align: center; padding: 20px; color: #94a3b8; font-size: 13px; }
  </style>
</head>
<body>
  <div class="header"><h1>景点详情 (JSP + EL 表达式)</h1></div>
  <div class="nav">
    <a href="/page/home">首页</a>
    <a href="/page/spots">景点列表</a>
    <a href="/page/routes">路线列表</a>
    <a href="/page/stats">数据统计</a>
    <a href="/page/tour/records">我的足迹</a>
    <a href="/">返回 SPA</a>
  </div>
  <div class="container">
    <div class="detail-card">
      <img src="${spot.imageUrl}" alt="${spot.name}" />
      <div class="detail-body">
        <h2>${spot.name}</h2>
        <div class="meta-row">
          <span>📍 ${spot.province} ${spot.city}</span>
          <span>🏷️ ${spot.categoryName}</span>
          <span>⭐ 评分 ${spot.rating} (${spot.ratingCount} 人)</span>
          <span>🕘 ${spot.openTime}</span>
          <span>💪 强度 ${spot.difficulty}/5</span>
        </div>

        <div class="tag-row">
          <c:forEach var="tag" items="${spot.tags}">
            <span class="tag">${tag}</span>
          </c:forEach>
          <c:forEach var="item" items="${spot.suitableFor}">
            <span class="tag">${item}</span>
          </c:forEach>
        </div>

        <c:choose>
          <c:when test="${spot.price == 0}">
            <div class="price-box"><span class="price free">免费</span><span class="label">免费开放</span></div>
          </c:when>
          <c:otherwise>
            <div class="price-box"><span class="price">￥<fmt:formatNumber value="${spot.price}" pattern="#" /></span><span class="label">门票价格</span></div>
          </c:otherwise>
        </c:choose>

        <div class="highlight-box">
          <h4>🎯 核心看点</h4>
          <ul>
            <c:forEach var="h" items="${spot.highlights}">
              <li>${h}</li>
            </c:forEach>
          </ul>
        </div>

        <p class="desc">${spot.description}</p>

        <c:if test="${not empty spot.tips}">
          <div class="highlight-box" style="border-color:#f59e0b;background:#fffbeb">
            <h4 style="color:#f59e0b">💡 游玩贴士</h4>
            <p style="font-size:14px;line-height:1.8">${spot.tips}</p>
          </div>
        </c:if>
      </div>
    </div>
  </div>
  <div class="footer">JSP 景点详情页 | EL 表达式 + JSTL c:forEach + c:choose/c:when/c:otherwise</div>
</body>
</html>
