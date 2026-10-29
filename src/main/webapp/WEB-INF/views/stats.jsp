<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>数据统计 - 云游中国</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
    .header { background: linear-gradient(135deg, #109e6e, #0ea5e9); color: #fff; padding: 20px; text-align: center; }
    .header h1 { font-size: 22px; }
    .nav { text-align: center; margin: 20px 0; }
    .nav a { display: inline-block; padding: 8px 16px; margin: 0 6px; border-radius: 8px; background: #fff; color: #109e6e;
             text-decoration: none; font-size: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
    .nav a:hover { background: #109e6e; color: #fff; }
    .container { max-width: 1000px; margin: 0 auto; padding: 0 20px 30px; }
    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 14px; margin-bottom: 22px; }
    .card { background: #fff; border-radius: 12px; padding: 18px 14px; text-align: center; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
    .card .num { font-size: 26px; font-weight: 700; color: #0f766e; }
    .card .lbl { font-size: 13px; color: #64748b; margin-top: 4px; }
    .panel { background: #fff; border-radius: 12px; padding: 20px 22px; margin-bottom: 18px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
    .panel h3 { font-size: 16px; color: #0f766e; margin-bottom: 14px; }
    .bar-row { display: flex; align-items: center; margin-bottom: 8px; font-size: 13px; }
    .bar-row .name { width: 90px; color: #475569; text-align: right; padding-right: 10px; flex-shrink: 0; }
    .bar-row .track { flex: 1; background: #f1f5f9; border-radius: 6px; height: 20px; overflow: hidden; }
    .bar-row .fill { height: 100%; border-radius: 6px; background: linear-gradient(90deg, #14b8a6, #0ea5e9); min-width: 2px; }
    .bar-row .val { width: 46px; text-align: right; color: #64748b; padding-left: 8px; flex-shrink: 0; }
    .spot-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 14px; }
    .spot { background: #f8fafc; border-radius: 10px; overflow: hidden; }
    .spot img { width: 100%; height: 110px; object-fit: cover; display: block; }
    .spot .info { padding: 8px 10px; font-size: 13px; }
    .spot .info b { display: block; color: #0f766e; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .spot .info span { font-size: 12px; color: #94a3b8; }
    .footer { text-align: center; padding: 20px; color: #94a3b8; font-size: 13px; }
  </style>
</head>
<body>
  <div class="header"><h1>平台数据统计 (JSP + JSTL)</h1></div>
  <div class="nav">
    <a href="/page/home">首页</a>
    <a href="/page/spots">景点列表</a>
    <a href="/page/routes">路线列表</a>
    <a href="/page/stats">数据统计</a>
    <a href="/page/tour/records">我的足迹</a>
    <a href="/">返回 SPA</a>
  </div>
  <div class="container">

    <div class="cards">
      <div class="card"><div class="num">${stats.spotCount}</div><div class="lbl">在架景点</div></div>
      <div class="card"><div class="num">${stats.provinceCount}</div><div class="lbl">覆盖省份</div></div>
      <div class="card"><div class="num">${stats.routeCount}</div><div class="lbl">主题路线</div></div>
      <div class="card"><div class="num">${stats.userCount}</div><div class="lbl">注册用户</div></div>
      <div class="card"><div class="num">${stats.favoriteCount}</div><div class="lbl">收藏次数</div></div>
      <div class="card"><div class="num">${stats.commentCount}</div><div class="lbl">点评数量</div></div>
      <div class="card"><div class="num">${stats.avgRating}</div><div class="lbl">平均评分</div></div>
    </div>

    <div class="panel">
      <h3>七大区域景点分布</h3>
      <c:forEach var="r" items="${stats.regionDistribution}">
        <div class="bar-row">
          <div class="name">${r.region}</div>
          <div class="track"><div class="fill" style="width:${r.count * 100 / stats.spotCount}%"></div></div>
          <div class="val">${r.count}</div>
        </div>
      </c:forEach>
    </div>

    <div class="panel">
      <h3>景点分类分布</h3>
      <c:forEach var="c" items="${stats.categoryDistribution}">
        <div class="bar-row">
          <div class="name">${c.name}</div>
          <div class="track"><div class="fill" style="width:${c.count * 100 / stats.spotCount}%"></div></div>
          <div class="val">${c.count}</div>
        </div>
      </c:forEach>
    </div>

    <div class="panel">
      <h3>游客评分分布(条数)</h3>
      <c:forEach var="rt" items="${stats.ratingDistribution}">
        <div class="bar-row">
          <div class="name">${rt.rating} 星</div>
          <div class="track"><div class="fill" style="width:${rt.count * 100 / stats.commentCount}%"></div></div>
          <div class="val">${rt.count}</div>
        </div>
      </c:forEach>
    </div>

    <div class="panel">
      <h3>热门景点 Top 8</h3>
      <div class="spot-grid">
        <c:forEach var="s" items="${stats.hotSpots}">
          <a href="/page/spots/${s.id}" style="text-decoration:none">
            <div class="spot">
              <img src="${s.imageUrl}" alt="${s.name}"/>
              <div class="info"><b>${s.name}</b><span>${s.province} · 热度 ${s.popularity} · ¥${s.price}</span></div>
            </div>
          </a>
        </c:forEach>
      </div>
    </div>

  </div>
  <div class="footer">JSP 数据统计 | 服务端聚合 SQL + JSTL c:forEach 纯 CSS 图表, 无前端框架依赖</div>
</body>
</html>
