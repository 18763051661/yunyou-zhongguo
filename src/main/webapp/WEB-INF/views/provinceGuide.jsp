<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>全国省份导览 - 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #0ea5e9, #6366f1); color: #fff; padding: 30px 20px; text-align: center; }
  .hero h1 { font-size: 24px; }
  .hero p { font-size: 13px; opacity: .9; margin-top: 6px; }
  .nav { text-align: center; margin: 18px 0; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #0ea5e9; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #0ea5e9; color: #fff; }
  .container { max-width: 1100px; margin: 0 auto; padding: 0 16px 30px; }
  .summary-bar { display: flex; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
  .sum-card { background: #fff; border-radius: 10px; padding: 14px 20px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,.06); flex: 1; min-width: 120px; }
  .sum-card .num { font-size: 24px; font-weight: 700; color: #0ea5e9; }
  .sum-card .lbl { font-size: 12px; color: #94a3b8; margin-top: 4px; }
  .prov-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
  .prov-card { background: #fff; border-radius: 12px; padding: 18px; box-shadow: 0 2px 10px rgba(0,0,0,.06); transition: transform .15s, box-shadow .15s; }
  .prov-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,.1); }
  .prov-card.hot { border-top: 3px solid #f59e0b; }
  .prov-card.normal { border-top: 3px solid #e2e8f0; }
  .prov-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
  .prov-head h3 { font-size: 16px; }
  .prov-head .region-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
  .region-华北 { background: #dbeafe; color: #1e40af; }
  .region-东北 { background: #fef3c7; color: #92400e; }
  .region-华东 { background: #d1fae5; color: #065f46; }
  .region-华中 { background: #fce7f3; color: #9d174d; }
  .region-华南 { background: #e0e7ff; color: #3730a3; }
  .region-西南 { background: #f3e8ff; color: #6b21a8; }
  .region-西北 { background: #fed7aa; color: #9a3412; }
  .prov-stats { display: flex; gap: 14px; font-size: 13px; color: #64748b; }
  .prov-stats b { color: #0ea5e9; }
  .prov-spots { margin-top: 8px; font-size: 12px; color: #94a3b8; }
  .prov-spots a { color: #0ea5e9; text-decoration: none; }
  .footer { text-align: center; padding: 18px; color: #94a3b8; font-size: 13px; }
</style>
</head>
<body>
  <div class="hero">
    <h1>全国省份景点导览</h1>
    <p>JSTL 嵌套 c:forEach + EL 条件表达式 + CSS 动态类名</p>
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

    <!-- 汇总卡片: EL 算术计算总数 -->
    <c:set var="totalProvinces" value="${0}" />
    <c:set var="totalSpots" value="${0}" />
    <c:forEach var="p" items="${provinces}">
      <c:set var="totalProvinces" value="${totalProvinces + 1}" />
      <c:set var="cnt" value="${p.spotCount}" />
      <c:set var="totalSpots" value="${totalSpots + (cnt == null ? 0 : cnt)}" />
    </c:forEach>
    <div class="summary-bar">
      <div class="sum-card"><div class="num">${totalProvinces}</div><div class="lbl">覆盖省份</div></div>
      <div class="sum-card"><div class="num">${totalSpots}</div><div class="lbl">景点总数</div></div>
      <div class="sum-card">
        <div class="num"><fmt:formatNumber value="${totalSpots / totalProvinces}" pattern="0.0" /></div>
        <div class="lbl">平均每省</div>
      </div>
      <div class="sum-card"><div class="num">31</div><div class="lbl">全国省份</div></div>
    </div>

    <!-- 省份卡片网格: 嵌套 c:forEach + EL 条件类名 -->
    <div class="prov-grid">
      <c:forEach var="p" items="${provinces}" varStatus="vs">
        <c:set var="region" value="${p.region}" />
        <c:choose>
          <c:when test="${p.spotCount ge 10}">
            <c:set var="cardClass" value="prov-card hot" />
          </c:when>
          <c:otherwise>
            <c:set var="cardClass" value="prov-card normal" />
          </c:otherwise>
        </c:choose>
        <div class="${cardClass}">
          <div class="prov-head">
            <h3>${p.name}</h3>
            <c:if test="${not empty region}">
              <span class="region-tag region-${region}">${region}</span>
            </c:if>
          </div>
          <div class="prov-stats">
            <span>📍 <b>${p.spotCount}</b> 景点</span>
            <c:if test="${not empty p.avgRating}">
              <span>★ <b><fmt:formatNumber value="${p.avgRating}" pattern="0.0" /></b></span>
            </c:if>
          </div>
          <c:if test="${not empty p.topSpots}">
            <div class="prov-spots">
              <c:forEach var="s" items="${p.topSpots}" varStatus="ts">
                <c:if test="${ts.index gt 0}"> · </c:if>
                <a href="/page/spots/${s.id}">${s.name}</a>
              </c:forEach>
            </div>
          </c:if>
        </div>
      </c:forEach>
    </div>

  </div>
  <div class="footer">JSP 全国省份导览 | JSTL 嵌套 c:forEach + EL 条件表达式 + c:set 变量 + fmt:formatNumber</div>
</body>
</html>
