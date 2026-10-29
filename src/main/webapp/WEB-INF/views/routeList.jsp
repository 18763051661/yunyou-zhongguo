<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>路线列表 - 云游中国</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
    .header { background: linear-gradient(135deg, #109e6e, #0ea5e9); color: #fff; padding: 20px; text-align: center; }
    .header h1 { font-size: 22px; }
    .nav { text-align: center; margin: 20px 0; }
    .nav a { display: inline-block; padding: 8px 16px; margin: 0 6px; border-radius: 8px; background: #fff; color: #109e6e; text-decoration: none; font-size: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
    .nav a:hover { background: #109e6e; color: #fff; }
    .container { max-width: 1000px; margin: 0 auto; padding: 20px; }
    .route-card { background: #fff; border-radius: 14px; padding: 20px; margin-bottom: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; gap: 20px; }
    .route-card img { width: 200px; height: 140px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
    .route-info { flex: 1; }
    .route-info h3 { font-size: 18px; color: #109e6e; }
    .route-info p { font-size: 14px; color: #64748b; margin-top: 6px; line-height: 1.6; }
    .route-meta { margin-top: 10px; font-size: 13px; color: #94a3b8; }
    .tag { display: inline-block; font-size: 12px; padding: 2px 8px; border-radius: 6px; background: #e0f2fe; color: #0369a1; margin-right: 4px; }
    .footer { text-align: center; padding: 20px; color: #94a3b8; font-size: 13px; }
  </style>
</head>
<body>
  <div class="header"><h1>主题路线列表 (JSP + JSTL)</h1></div>
  <div class="nav">
    <a href="/page/home">首页</a>
    <a href="/page/spots">景点列表</a>
    <a href="/page/routes">路线列表</a>
    <a href="/page/stats">数据统计</a>
    <a href="/page/tour/records">我的足迹</a>
    <a href="/">返回 SPA</a>
  </div>
  <div class="container">
    <c:forEach var="route" items="${routes}" varStatus="vs">
      <div class="route-card">
        <img src="${route.coverUrl}" alt="${route.name}" />
        <div class="route-info">
          <h3>${vs.index + 1}. ${route.name}</h3>
          <p>${route.subtitle}</p>
          <div class="route-meta">
            <span>📍 ${route.totalSpots} 站</span> |
            <span>⏱ ${route.durationMinutes} 分钟</span> |
            <span>💪 强度 ${route.difficulty}/5</span>
          </div>
          <div style="margin-top:8px">
            <c:forEach var="tag" items="${route.tags}">
              <span class="tag">${tag}</span>
            </c:forEach>
          </div>
          <div style="margin-top:12px">
            <a href="/page/routes/${route.id}" style="font-size:13px;color:#109e6e;text-decoration:none;font-weight:600">查看逐站详情 →</a>
          </div>
        </div>
      </div>
    </c:forEach>
  </div>
  <div class="footer">JSP 路线列表 | JSTL c:forEach + varStatus + EL 表达式</div>
</body>
</html>
