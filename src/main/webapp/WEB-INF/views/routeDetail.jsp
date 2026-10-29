<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>${route.name} - 云游中国</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
    .hero { position: relative; color: #fff; padding: 46px 20px 38px; text-align: center;
            background: linear-gradient(rgba(6,78,59,.72), rgba(14,116,144,.72)), url('${route.coverUrl}') center/cover; }
    .hero h1 { font-size: 26px; margin-bottom: 8px; text-shadow: 0 1px 4px rgba(0,0,0,.4); }
    .hero p { font-size: 14px; opacity: .92; max-width: 640px; margin: 0 auto; }
    .meta { margin-top: 14px; font-size: 13px; }
    .meta span { display: inline-block; background: rgba(255,255,255,.18); padding: 4px 12px; border-radius: 12px; margin: 0 4px; }
    .nav { text-align: center; margin: 20px 0; }
    .nav a { display: inline-block; padding: 8px 16px; margin: 0 6px; border-radius: 8px; background: #fff; color: #109e6e;
             text-decoration: none; font-size: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
    .nav a:hover { background: #109e6e; color: #fff; }
    .container { max-width: 900px; margin: 0 auto; padding: 10px 20px 30px; }
    .tag { display: inline-block; font-size: 12px; padding: 2px 8px; border-radius: 6px; background: #e0f2fe; color: #0369a1; margin-right: 4px; }
    .timeline { position: relative; margin-top: 8px; padding-left: 26px; }
    .timeline::before { content: ''; position: absolute; left: 8px; top: 8px; bottom: 8px; width: 3px; background: #99f6e4; border-radius: 2px; }
    .step { position: relative; background: #fff; border-radius: 14px; padding: 18px 20px; margin-bottom: 18px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
    .step::before { content: ''; position: absolute; left: -24px; top: 26px; width: 13px; height: 13px; border-radius: 50%;
                    background: #109e6e; border: 3px solid #ccfbf1; }
    .step-head { display: flex; gap: 16px; align-items: flex-start; }
    .step-head img { width: 190px; height: 126px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
    .step-no { display: inline-block; font-size: 12px; font-weight: 600; color: #fff; background: #109e6e; padding: 2px 10px; border-radius: 10px; margin-bottom: 6px; }
    .step h3 { font-size: 17px; color: #0f766e; display: inline; margin-left: 6px; }
    .step-loc { font-size: 12px; color: #94a3b8; margin: 4px 0 6px; }
    .step p.summary { font-size: 13px; color: #64748b; line-height: 1.65; }
    .facts { margin-top: 8px; font-size: 12px; color: #64748b; }
    .facts span { margin-right: 12px; }
    .narration { margin-top: 12px; background: #f0fdfa; border-left: 3px solid #14b8a6; padding: 10px 14px;
                 font-size: 13px; color: #155e75; line-height: 1.7; border-radius: 0 8px 8px 0; }
    .narration b { color: #0f766e; }
    .footer { text-align: center; padding: 20px; color: #94a3b8; font-size: 13px; }
  </style>
</head>
<body>
  <div class="hero">
    <h1>${route.name}</h1>
    <p>${route.subtitle}</p>
    <div class="meta">
      <span>🧭 共 ${route.totalSpots} 站</span>
      <span>⏱ 全程约 ${route.durationMinutes} 分钟</span>
      <span>💪 强度 ${route.difficulty}/5</span>
      <span>🏛 停留合计 ${route.totalStayMinutes} 分钟</span>
    </div>
    <div class="meta">
      <c:forEach var="tag" items="${route.tags}"><span class="tag" style="background:rgba(255,255,255,.25);color:#fff">${tag}</span></c:forEach>
    </div>
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
    <div class="timeline">
      <c:forEach var="s" items="${route.spots}" varStatus="vs">
        <div class="step">
          <div class="step-head">
            <img src="${s.imageUrl}" alt="${s.spotName}"/>
            <div>
              <span class="step-no">第 ${s.dayNo} 天 · 第 ${s.stepNo} 站</span>
              <h3>${s.spotName}</h3>
              <div class="step-loc">📍 ${s.spotProvince} · ${s.spotCity} ｜ 建议游览 ${s.spotDurationMinutes} 分钟，本站停留 ${s.stayMinutes} 分钟</div>
              <p class="summary">${s.spotSummary}</p>
              <div class="facts">
                <span>💵 门票：${s.spotPrice == 0 ? '免费' : s.spotPrice}</span>
                <span>🕘 开放：${s.spotOpenTime}</span>
                <span>⭐ 评分：${s.spotRating}</span>
              </div>
            </div>
          </div>
          <c:if test="${not empty s.narration}">
            <div class="narration"><b>🎧 AI 讲解：</b>${s.narration}</div>
          </c:if>
        </div>
      </c:forEach>
    </div>
  </div>
  <div class="footer">JSP 路线详情 | JSTL c:forEach 时间线 + c:if 条件渲染 + EL 表达式</div>
</body>
</html>
