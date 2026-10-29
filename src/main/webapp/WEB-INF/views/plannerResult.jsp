<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%
  // 把 spots_json 从 plan 里拿出(在 Java 里已 parse 成 List, 这里我们存到 request 用 JSTL 直接拿不到,
  // 但 Service 存的是 JSON 字符串, 我们再在页面里用 JS 解析渲染. JSTL 渲染 itinerary 纯文本即可)
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>${plan.title} - AI 出游推荐 · 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #0ea5e9, #7c3aed); color: #fff; padding: 40px 20px 34px; text-align: center; }
  .hero h1 { font-size: 22px; }
  .hero .badge { display: inline-block; background: rgba(255,255,255,.2); padding: 3px 10px; border-radius: 999px; font-size: 12px; margin-left: 8px; }
  .hero p { font-size: 13px; opacity: .92; margin-top: 8px; max-width: 440px; margin-left: auto; margin-right: auto; line-height: 1.6; }
  .nav { text-align: center; margin: 16px 0 10px; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #0ea5e9; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #0ea5e9; color: #fff; }
  .container { max-width: 720px; margin: 0 auto; padding: 12px 16px 36px; }
  .card { background: #fff; border-radius: 14px; padding: 22px 22px; margin-bottom: 14px; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
  h2 { font-size: 17px; color: #0ea5e9; margin-bottom: 14px; }
  h3 { font-size: 15px; color: #7c3aed; margin-bottom: 10px; }
  /* 景点卡片 */
  .spot-timeline { position: relative; padding-left: 26px; }
  .spot-timeline::before { content: ''; position: absolute; left: 9px; top: 6px; bottom: 6px; width: 2px; background: #c4b5fd; }
  .spot-item { position: relative; background: #faf5ff; border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; }
  .spot-item::before { content: attr(data-day); position: absolute; left: -23px; top: 14px; width: 22px; height: 22px; border-radius: 50%; background: #7c3aed; color: #fff; font-size: 11px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
  .spot-name b { font-size: 15px; color: #334155; }
  .spot-name small { color: #94a3b8; margin-left: 8px; }
  .spot-meta { margin-top: 4px; font-size: 12px; color: #64748b; }
  .spot-meta span { margin-right: 14px; }
  /* itinerary 纯文本渲染 */
  .itinerary { background: #f8fafc; border-left: 3px solid #0ea5e9; padding: 14px 18px; border-radius: 0 8px 8px 0; white-space: pre-wrap; font-size: 14px; line-height: 1.9; color: #334155; }
  .recent { display: flex; flex-wrap: wrap; gap: 8px; }
  .recent a { background: #f1f5f9; color: #475569; padding: 7px 12px; border-radius: 14px; text-decoration: none; font-size: 12px; }
  .recent a:hover { background: #e0f2fe; color: #0369a1; }
  .engine { font-size: 12px; color: #94a3b8; margin-top: 10px; }
  .engine .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #22c55e; margin-right: 4px; }
  .new-form { display: block; text-align: center; padding: 12px; border-radius: 10px; background: linear-gradient(135deg, #0ea5e9, #7c3aed); color: #fff; text-decoration: none; font-weight: 700; box-shadow: 0 6px 18px rgba(14,165,233,.3); }
  .new-form:hover { transform: translateY(-1px); }
</style>
</head>
<body>

<div class="hero">
  <h1>${plan.title} <span class="badge">${plan.days} 天</span></h1>
  <p>${plan.summary}</p>
</div>
<div class="nav">
  <a href="${pageContext.request.contextPath}/page/home">首页</a>
  <a href="${pageContext.request.contextPath}/page/spots">景点</a>
  <a href="${pageContext.request.contextPath}/page/routes">路线</a>
  <a href="${pageContext.request.contextPath}/page/planner">🧭 智能规划</a>
</div>

<div class="container">
  <!-- 景点时间线 -->
  <div class="card">
    <h2>📍 推荐景点</h2>
    <div class="spot-timeline" id="spotTimeline">
      <div style="padding:18px;color:#94a3b8">加载景点列表...</div>
    </div>
    <div class="engine">
      <span class="dot"></span>由 AI 引擎「
      <c:choose>
        <c:when test="${plan.provider=='deepseek'}">⚡ DeepSeek</c:when>
        <c:when test="${plan.provider=='qwen'}">⚡ 通义千问</c:when>
        <c:when test="${plan.provider=='doubao'}">⚡ 豆包AI</c:when>
        <c:otherwise>📚 本地规则引擎(AI 暂不可用时的兜底)</c:otherwise>
      </c:choose>
      」生成 · 从平台真实景点中挑选
    </div>
  </div>

  <!-- AI 逐日行程 -->
  <div class="card">
    <h2>📝 逐日行程(AI 生成)</h2>
    <div class="itinerary">${plan.itinerary}</div>
  </div>

  <!-- 最近生成 -->
  <c:if test="${not empty recent}">
  <div class="card">
    <h3>🕒 你可能想看(最近生成)</h3>
    <div class="recent">
      <c:forEach items="${recent}" var="r">
        <a href="${pageContext.request.contextPath}/page/planner/${r.id}">${r.title}(${r.days}天 · ${r.provider})</a>
      </c:forEach>
    </div>
  </div>
  </c:if>

  <a href="${pageContext.request.contextPath}/page/planner" class="new-form">✨ 换一批偏好 · 让 AI 再推荐一次</a>
</div>

<script>
// plan.spotsJson 已经在 Service 里存为 JSON 字符串, 我们从隐藏 div 拿了 parse
var spotsJson = ${plan.spotsJson == null ? '[]' : plan.spotsJson};
var box = document.getElementById('spotTimeline');
if (spotsJson && spotsJson.length) {
  var html = '';
  spotsJson.forEach(function(s) {
    var priceTxt = (s.price === null || s.price === 0) ? '免费' : ('¥' + s.price);
    var ratingTxt = s.rating ? ('⭐ ' + Number(s.rating).toFixed(1)) : '';
    html += '<div class="spot-item" data-day="' + (s.day || '?') + '">'
          +   '<div class="spot-name"><b>' + (s.name || '') + '</b><small>' + (s.province || '') + '</small></div>'
          +   '<div class="spot-meta"><span>🎟 ' + priceTxt + '</span><span>' + ratingTxt + '</span><span>⏱ ' + (s.stay_hours || 3) + ' 小时</span></div>'
          + '</div>';
  });
  box.innerHTML = html;
} else {
  box.innerHTML = '<div style="padding:18px;color:#94a3b8">暂无推荐景点数据</div>';
}
</script>
</body>
</html>
