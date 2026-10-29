<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>景点列表 - 云游中国</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
    .header { background: linear-gradient(135deg, #109e6e, #0ea5e9); color: #fff; padding: 20px; text-align: center; }
    .header h1 { font-size: 24px; }
    .nav { text-align: center; margin: 20px 0; }
    .nav a { display: inline-block; padding: 8px 16px; margin: 0 6px; border-radius: 8px; background: #fff; color: #109e6e; text-decoration: none; font-size: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
    .nav a:hover { background: #109e6e; color: #fff; }
    .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
    .info-bar { background: #fff; border-radius: 8px; padding: 12px 20px; margin-bottom: 20px; font-size: 14px; color: #64748b; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
    .info-bar b { color: #109e6e; }
    .spot-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
    .spot-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
    .spot-card img { width: 100%; height: 180px; object-fit: cover; }
    .spot-card-body { padding: 14px; }
    .spot-card-body h3 { font-size: 16px; }
    .spot-card-body p { font-size: 13px; color: #64748b; margin-top: 6px; }
    .price { color: #ea580c; font-weight: 700; font-size: 18px; margin-top: 8px; }
    .price.free { color: #16a34a; }
    .tag { display: inline-block; font-size: 12px; padding: 2px 8px; border-radius: 6px; background: #e0f2fe; color: #0369a1; margin-right: 4px; }
    .pagination { text-align: center; margin: 30px 0; }
    .pagination a { display: inline-block; padding: 8px 14px; margin: 0 4px; border-radius: 8px; background: #fff; color: #109e6e; text-decoration: none; font-size: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
    .pagination a:hover { background: #109e6e; color: #fff; }
    .pagination a.current { background: #109e6e; color: #fff; }
    .pagination a.disabled { color: #cbd5e1; pointer-events: none; }
  </style>
</head>
<body>
  <div class="header"><h1>景点列表 (JSP + JSTL 分页)</h1></div>
  <div class="nav">
    <a href="/page/home">首页</a>
    <a href="/page/spots">景点列表</a>
    <a href="/page/routes">路线列表</a>
    <a href="/page/stats">数据统计</a>
    <a href="/page/tour/records">我的足迹</a>
    <a href="/">返回 SPA</a>
  </div>
  <div class="container">
    <div class="info-bar">
      共 <b>${total}</b> 个景点 | 当前第 <b>${page}</b> 页 | 每页 <b>${size}</b> 条 | 共 <b>${totalPages}</b> 页
    </div>

    <div class="spot-grid">
      <c:forEach var="spot" items="${spots}" varStatus="vs">
        <div class="spot-card">
          <img src="${spot.imageUrl}" alt="${spot.name}" />
          <div class="spot-card-body">
            <h3>${spot.name}</h3>
            <p>${spot.province} ${spot.city}</p>
            <p>
              <span class="tag">${spot.categoryName}</span>
              <c:if test="${not empty spot.tags}">
                <c:forEach var="tag" items="${spot.tags}" end="2">
                  <span class="tag">${tag}</span>
                </c:forEach>
              </c:if>
            </p>
            <p>评分: <b style="color:#f59e0b">★ ${spot.rating}</b></p>
            <c:choose>
              <c:when test="${spot.price == 0}">
                <div class="price free">免费开放</div>
              </c:when>
              <c:otherwise>
                <div class="price">￥<fmt:formatNumber value="${spot.price}" pattern="#" /> 元</div>
              </c:otherwise>
            </c:choose>
            <p style="margin-top:8px"><a href="/page/spots/${spot.id}" style="color:#109e6e;text-decoration:none">查看详情 →</a></p>
          </div>
        </div>
      </c:forEach>
    </div>

    <!-- JSTL 分页导航 -->
    <div class="pagination">
      <c:if test="${page > 1}">
        <a href="/page/spots?page=${page - 1}&size=${size}">← 上一页</a>
      </c:if>
      <c:forEach begin="1" end="${totalPages}" var="i">
        <c:choose>
          <c:when test="${i == page}">
            <a class="current">${i}</a>
          </c:when>
          <c:otherwise>
            <a href="/page/spots?page=${i}&size=${size}">${i}</a>
          </c:otherwise>
        </c:choose>
      </c:forEach>
      <c:if test="${page < totalPages}">
        <a href="/page/spots?page=${page + 1}&size=${size}">下一页 →</a>
      </c:if>
    </div>
  </div>
</body>
</html>
