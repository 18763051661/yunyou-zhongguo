<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>云游中国 - JSP 首页</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
    .header { background: linear-gradient(135deg, #109e6e, #0ea5e9); color: #fff; padding: 30px 20px; text-align: center; }
    .header h1 { font-size: 28px; }
    .header p { margin-top: 8px; font-size: 14px; opacity: 0.9; }
    .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 30px; }
    .stat-card { background: #fff; border-radius: 12px; padding: 20px; text-align: center; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
    .stat-card b { font-size: 32px; color: #109e6e; }
    .stat-card span { display: block; font-size: 13px; color: #64748b; margin-top: 4px; }
    .section-title { font-size: 20px; margin-bottom: 16px; border-left: 4px solid #109e6e; padding-left: 12px; }
    .spot-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
    .spot-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.08); transition: transform 0.2s; }
    .spot-card:hover { transform: translateY(-4px); }
    .spot-card img { width: 100%; height: 180px; object-fit: cover; }
    .spot-card-body { padding: 14px; }
    .spot-card-body h3 { font-size: 16px; }
    .spot-card-body p { font-size: 13px; color: #64748b; margin-top: 6px; }
    .spot-card-body .price { color: #ea580c; font-weight: 700; font-size: 18px; margin-top: 8px; }
    .spot-card-body .price.free { color: #16a34a; }
    .tag { display: inline-block; font-size: 12px; padding: 2px 8px; border-radius: 6px; background: #e0f2fe; color: #0369a1; margin-right: 4px; }
    .nav { text-align: center; margin: 20px 0; }
    .nav a { display: inline-block; padding: 8px 16px; margin: 0 6px; border-radius: 8px; background: #fff; color: #109e6e; text-decoration: none; font-size: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
    .nav a:hover { background: #109e6e; color: #fff; }
    .footer { text-align: center; padding: 20px; color: #94a3b8; font-size: 13px; }
  </style>
</head>
<body>
  <div class="header">
    <h1>云游中国 - 全国景点智能导览</h1>
    <p>SSM 架构 + JSP + JSTL + EL 表达式 | 期末项目展示页</p>
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
    <!-- 统计概览: EL 表达式取值 -->
    <div class="stats-grid">
      <div class="stat-card"><b>${stats.spotCount}</b><span>真实景点</span></div>
      <div class="stat-card"><b>${stats.provinceCount}</b><span>覆盖省份</span></div>
      <div class="stat-card"><b>${stats.routeCount}</b><span>主题路线</span></div>
      <div class="stat-card"><b>${stats.userCount}</b><span>注册用户</span></div>
    </div>

    <!-- JSTL c:forEach 遍历热门景点 -->
    <div class="section-title">热门景点 (JSTL c:forEach)</div>
    <div class="spot-grid">
      <c:forEach var="spot" items="${hotSpots}" varStatus="vs">
        <div class="spot-card">
          <img src="${spot.imageUrl}" alt="${spot.name}" />
          <div class="spot-card-body">
            <h3>${vs.index + 1}. ${spot.name}</h3>
            <p>${spot.province} ${spot.city}</p>
            <p>
              <span class="tag">${spot.categoryName}</span>
              <span class="tag">评分 ${spot.rating}</span>
            </p>
            <c:choose>
              <c:when test="${spot.price == 0}">
                <div class="price free">免费</div>
              </c:when>
              <c:otherwise>
                <div class="price">￥<fmt:formatNumber value="${spot.price}" pattern="#" /> 元</div>
              </c:otherwise>
            </c:choose>
            <p style="margin-top:8px">
              <a href="/page/spots/${spot.id}" style="color:#109e6e;text-decoration:none">查看详情 →</a>
            </p>
          </div>
        </div>
      </c:forEach>
    </div>
  </div>

  <div class="footer">
    云游中国 SSM 期末项目 | Spring ${'7.0.9'} + MyBatis ${'3.5.19'} + JSP/JSTL + MySQL 8
  </div>
</body>
</html>
