<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>我的收藏夹 - 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #ec4899, #f43f5e); color: #fff; padding: 30px 20px; text-align: center; }
  .hero h1 { font-size: 24px; }
  .hero p { font-size: 13px; opacity: .9; margin-top: 6px; }
  .nav { text-align: center; margin: 18px 0; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #ec4899; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #ec4899; color: #fff; }
  .container { max-width: 900px; margin: 0 auto; padding: 0 16px 30px; }
  .fav-list { display: flex; flex-direction: column; gap: 14px; }
  .fav-card { background: #fff; border-radius: 12px; padding: 16px 20px; box-shadow: 0 2px 10px rgba(0,0,0,.06); display: flex; gap: 14px; align-items: center; transition: transform .15s; }
  .fav-card:hover { transform: translateX(4px); }
  .fav-card img { width: 80px; height: 80px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
  .fav-info { flex: 1; }
  .fav-info h3 { font-size: 16px; margin-bottom: 4px; }
  .fav-info p { font-size: 13px; color: #64748b; }
  .fav-meta { display: flex; gap: 10px; margin-top: 6px; }
  .fav-meta span { font-size: 12px; padding: 2px 8px; border-radius: 4px; background: #fce7f3; color: #9d174d; }
  .fav-meta .free { background: #d1fae5; color: #065f46; }
  .fav-meta .rating { background: #fef3c7; color: #92400e; }
  .fav-actions { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
  .fav-actions a { font-size: 13px; padding: 6px 14px; border-radius: 8px; text-decoration: none; }
  .fav-actions .btn-detail { background: #ec4899; color: #fff; }
  .fav-actions .btn-remove { background: #fff; color: #ef4444; border: 1px solid #fecaca; }
  .fav-time { font-size: 11px; color: #94a3b8; }
  .empty { text-align: center; padding: 50px 20px; color: #94a3b8; }
  .empty .big { font-size: 48px; margin-bottom: 10px; }
  .empty a { color: #ec4899; text-decoration: none; }
  .footer { text-align: center; padding: 18px; color: #94a3b8; font-size: 13px; }
  .count-bar { background: #fff; border-radius: 8px; padding: 12px 20px; margin-bottom: 16px; font-size: 14px; color: #64748b; box-shadow: 0 1px 6px rgba(0,0,0,.06); }
  .count-bar b { color: #ec4899; }
</style>
</head>
<body>
  <div class="hero">
    <h1>我的收藏夹</h1>
    <p>JSTL c:choose + c:when empty 判断 + EL 条件样式</p>
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

    <!-- c:choose empty 判断 -->
    <c:choose>
      <c:when test="${empty favorites}">
        <div class="empty">
          <div class="big">💔</div>
          <p>还没有收藏任何景点</p>
          <p style="margin-top:10px"><a href="/page/spots">去景点列表逛逛 →</a></p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="count-bar">
          共收藏 <b>${favorites.size()}</b> 个景点
        </div>
        <div class="fav-list">
          <c:forEach var="fav" items="${favorites}" varStatus="vs">
            <div class="fav-card">
              <img src="${fav.imageUrl}" alt="${fav.spotName}"
                   onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2280%22 height=%2280%22><rect width=%2280%22 height=%2280%22 fill=%22%23fce7f3%22/><text x=%2250%25%22 y=%2255%25%22 text-anchor=%22middle%22 fill=%22%23ec4899%22 font-size=%2214%22>暂无图</text></svg>'"/>
              <div class="fav-info">
                <h3>${fav.spotName}</h3>
                <p>${fav.province} ${fav.city} · ${fav.categoryName}</p>
                <div class="fav-meta">
                  <c:choose>
                    <c:when test="${fav.price == 0}">
                      <span class="free">免费</span>
                    </c:when>
                    <c:otherwise>
                      <span>¥<fmt:formatNumber value="${fav.price}" pattern="#" /></span>
                    </c:otherwise>
                  </c:choose>
                  <c:if test="${not empty fav.rating}">
                    <span class="rating">★ <fmt:formatNumber value="${fav.rating}" pattern="0.0" /></span>
                  </c:if>
                </div>
              </div>
              <div class="fav-actions">
                <a class="btn-detail" href="/page/spots/${fav.spotId}">查看详情</a>
                <span class="fav-time">收藏于 ${fav.createdAt}</span>
              </div>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>

  </div>
  <div class="footer">JSP 收藏夹 | JSTL c:choose + c:when empty 判断 + c:forEach + EL 条件样式</div>
</body>
</html>
