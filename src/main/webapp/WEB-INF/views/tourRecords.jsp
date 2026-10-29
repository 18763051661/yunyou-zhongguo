<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>我的足迹 - 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #7c3aed, #0ea5e9); color: #fff; padding: 34px 20px; text-align: center; }
  .hero h1 { font-size: 22px; }
  .hero p { font-size: 13px; opacity: .9; margin-top: 6px; }
  .nav { text-align: center; margin: 18px 0 10px; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #7c3aed; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #7c3aed; color: #fff; }
  .container { max-width: 800px; margin: 0 auto; padding: 0 16px 30px; }
  .summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
  .sum-card { background: #fff; border-radius: 12px; padding: 16px 10px; text-align: center; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
  .sum-card .num { font-size: 26px; font-weight: 700; color: #7c3aed; }
  .sum-card .lbl { font-size: 12px; color: #94a3b8; margin-top: 4px; }
  .card { background: #fff; border-radius: 12px; padding: 18px 20px; margin-bottom: 16px; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
  h3 { font-size: 15px; color: #7c3aed; margin-bottom: 12px; }
  /* 省份中国地图: 简化为 4x8 网格点亮 */
  .map-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 6px; }
  .map-cell { aspect-ratio: 1; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 11px; color: #94a3b8;
              background: #f1f5f9; border: 1px solid #e2e8f0; transition: all .15s; }
  .map-cell.on { background: linear-gradient(135deg, #7c3aed, #0ea5e9); color: #fff; border-color: transparent; box-shadow: 0 2px 8px rgba(124,58,237,.3); }
  .badge-row { display: flex; gap: 10px; flex-wrap: wrap; }
  .badge { background: #faf5ff; color: #7c3aed; border: 1px dashed #c4b5fd; padding: 6px 12px; border-radius: 14px; font-size: 12px; }
  .badge.locked { background: #f1f5f9; color: #cbd5e1; border-color: #e2e8f0; }
  /* 打卡时间线 */
  .timeline { position: relative; padding-left: 24px; margin-top: 4px; }
  .timeline::before { content: ''; position: absolute; left: 7px; top: 4px; bottom: 4px; width: 2px; background: #c4b5fd; }
  .tl-item { position: relative; background: #faf5ff; border-radius: 10px; padding: 10px 14px; margin-bottom: 10px; }
  .tl-item::before { content: ''; position: absolute; left: -21px; top: 14px; width: 10px; height: 10px; border-radius: 50%; background: #7c3aed; border: 2px solid #fff; box-shadow: 0 0 0 2px #c4b5fd; }
  .tl-top { display: flex; gap: 10px; align-items: center; }
  .tl-top img { width: 46px; height: 46px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
  .tl-info b { font-size: 14px; color: #334155; }
  .tl-info span { display: block; font-size: 12px; color: #94a3b8; margin-top: 2px; }
  .tl-time { margin-left: auto; font-size: 12px; color: #7c3aed; font-weight: 600; }
  .empty { text-align: center; color: #94a3b8; padding: 30px 0; font-size: 13px; }
  .footer { text-align: center; padding: 18px; color: #94a3b8; font-size: 13px; }
  /* 简化地图省份占位符 (4x8 格填充 31 省份) */
  .p-1 { grid-column: 2; grid-row: 1; } .p-2 { grid-column: 3; grid-row: 1; } .p-3 { grid-column: 4; grid-row: 1; }
  .p-4 { grid-column: 5; grid-row: 1; } .p-5 { grid-column: 6; grid-row: 1; } .p-6 { grid-column: 7; grid-row: 1; }
  .p-7 { grid-column: 2; grid-row: 2; } .p-8 { grid-column: 3; grid-row: 2; } .p-9 { grid-column: 4; grid-row: 2; }
  .p-10 { grid-column: 5; grid-row: 2; } .p-11 { grid-column: 6; grid-row: 2; }
  .p-12 { grid-column: 3; grid-row: 3; } .p-13 { grid-column: 4; grid-row: 3; } .p-14 { grid-column: 5; grid-row: 3; }
  .p-15 { grid-column: 6; grid-row: 3; }
  .p-16 { grid-column: 2; grid-row: 4; } .p-17 { grid-column: 3; grid-row: 4; } .p-18 { grid-column: 4; grid-row: 4; }
  .p-19 { grid-column: 5; grid-row: 4; } .p-20 { grid-column: 6; grid-row: 4; }
  .p-21 { grid-column: 2; grid-row: 5; } .p-22 { grid-column: 3; grid-row: 5; } .p-23 { grid-column: 4; grid-row: 5; }
  .p-24 { grid-column: 5; grid-row: 5; } .p-25 { grid-column: 6; grid-row: 5; }
  .p-26 { grid-column: 2; grid-row: 6; } .p-27 { grid-column: 3; grid-row: 6; } .p-28 { grid-column: 4; grid-row: 6; }
  .p-29 { grid-column: 5; grid-row: 6; } .p-30 { grid-column: 6; grid-row: 6; }
  .p-31 { grid-column: 5; grid-row: 7; }
</style>
</head>
<body>
  <div class="hero">
    <h1>我的虚拟游览足迹</h1>
    <p>Session: ${sessionId} · 点亮你走过的每一个省份</p>
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

    <!-- 数字统计 -->
    <div class="summary">
      <div class="sum-card">
        <div class="num">${footprint.spotCount == null ? 0 : footprint.spotCount}</div>
        <div class="lbl">已打卡景点</div>
      </div>
      <div class="sum-card">
        <div class="num">${footprint.provinceCount == null ? 0 : footprint.provinceCount}</div>
        <div class="lbl">点亮省份</div>
      </div>
      <div class="sum-card">
        <div class="num">${footprint.badgeCount == null ? 0 : footprint.badgeCount}</div>
        <div class="lbl">解锁徽章</div>
      </div>
    </div>

    <!-- 简化中国地图: 省份点亮 -->
    <div class="card">
      <h3>🎯 中国地图点亮</h3>
      <div class="map-grid">
        <c:forEach items="${footprint.provinces}" var="p">
          <c:set var="cls" value="p-${p.id} ${p.visited ? 'on' : ''}"/>
          <div class="${cls}" title="${p.name}">${p.name}</div>
        </c:forEach>
      </div>
      <div style="font-size:12px;color:#94a3b8;margin-top:10px">
        已点亮 ${footprint.provinceCount} / 31 个省份 · 覆盖率
        <c:choose>
          <c:when test="${footprint.provinceCount == null or footprint.provinceCount == 0}">0</c:when>
          <c:otherwise>${(footprint.provinceCount * 100 / 31).intValue()}</c:otherwise>
        </c:choose>%
      </div>
    </div>

    <!-- 徽章成就 -->
    <div class="card">
      <h3>🏅 徽章成就 (已解锁 ${footprint.badges == null ? 0 : footprint.badges.size()})</h3>
      <c:choose>
        <c:when test="${empty footprint.badges}">
          <div class="empty">还没有解锁任何徽章, 打卡更多景点试试吧</div>
        </c:when>
        <c:otherwise>
          <div class="badge-row">
            <c:forEach items="${footprint.badges}" var="b">
              <div class="badge">🏆 ${b}</div>
            </c:forEach>
          </div>
        </c:otherwise>
      </c:choose>
    </div>

    <!-- 打卡时间线 -->
    <div class="card">
      <h3>📍 打卡时间线 (${records.size()})</h3>
      <c:choose>
        <c:when test="${empty records}">
          <div class="empty">还没有任何打卡记录 · 在前台虚拟游览路线后就会出现</div>
        </c:when>
        <c:otherwise>
          <div class="timeline">
            <c:forEach items="${records}" var="r">
              <div class="tl-item">
                <div class="tl-top">
                  <img src="${r.imageUrl}" alt="${r.spotName}" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2246%22 height=%2246%22><rect width=%2246%22 height=%2246%22 fill=%22%23e2e8f0%22/><text x=%2250%25%22 y=%2255%25%22 text-anchor=%22middle%22 fill=%22%2394a3b8%22 font-size=%2210%22>no img</text></svg>'"/>
                  <div class="tl-info">
                    <b>${r.spotName}</b>
                    <span>${r.province} · ${r.city}</span>
                  </div>
                  <span class="tl-time">${r.visitedAt}</span>
                </div>
              </div>
            </c:forEach>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
  <div class="footer">JSP 虚拟游览足迹 | JSTL forEach 时间线 + 简化中国地图点亮 + c:choose 条件渲染</div>
</body>
</html>
