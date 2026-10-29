<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>关于平台 - 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; padding: 40px 20px; text-align: center; }
  .hero h1 { font-size: 26px; }
  .hero p { font-size: 14px; opacity: .9; margin-top: 8px; max-width: 600px; margin-left: auto; margin-right: auto; }
  .nav { text-align: center; margin: 18px 0; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #6366f1; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #6366f1; color: #fff; }
  .container { max-width: 800px; margin: 0 auto; padding: 0 16px 30px; }
  .card { background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 20px; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
  .card h2 { font-size: 17px; color: #6366f1; margin-bottom: 14px; }
  .stat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 14px; }
  .stat-item { text-align: center; padding: 16px 8px; background: #f8fafc; border-radius: 8px; }
  .stat-item .num { font-size: 28px; font-weight: 700; color: #6366f1; }
  .stat-item .lbl { font-size: 12px; color: #94a3b8; margin-top: 4px; }
  .tech-list { display: flex; flex-wrap: wrap; gap: 8px; }
  .tech-badge { padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 500; }
  .tech-spring { background: #dbeafe; color: #1e40af; }
  .tech-mvc { background: #d1fae5; color: #065f46; }
  .tech-mybatis { background: #fef3c7; color: #92400e; }
  .tech-mysql { background: #fce7f3; color: #9d174d; }
  .tech-ai { background: #e0e7ff; color: #3730a3; }
  .tech-frontend { background: #f3e8ff; color: #6b21a8; }
  .tech-jsp { background: #fed7aa; color: #9a3412; }
  .tech-aop { background: #cffafe; color: #0e7490; }
  .feature-list { list-style: none; }
  .feature-list li { padding: 8px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #475569; display: flex; align-items: center; gap: 8px; }
  .feature-list li:last-child { border-bottom: none; }
  .feature-list .icon { font-size: 18px; }
  .arch-flow { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 10px; }
  .arch-node { padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; }
  .arch-arrow { color: #94a3b8; font-size: 16px; }
  .footer { text-align: center; padding: 18px; color: #94a3b8; font-size: 13px; }
  .footer b { color: #6366f1; }
</style>
</head>
<body>
  <div class="hero">
    <h1>🇨🇳 云游中国</h1>
    <p>全国景点智能导览推荐 · 在线虚拟游览 · AI 助教讲解系统</p>
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

    <!-- 平台统计: c:set + fmt:formatNumber -->
    <c:set var="spotCount" value="${stats.spotCount == null ? 0 : stats.spotCount}" />
    <c:set var="routeCount" value="${stats.routeCount == null ? 0 : stats.routeCount}" />
    <c:set var="commentCount" value="${stats.commentCount == null ? 0 : stats.commentCount}" />
    <c:set var="userCount" value="${stats.userCount == null ? 0 : stats.userCount}" />
    <c:set var="favCount" value="${stats.favoriteCount == null ? 0 : stats.favoriteCount}" />

    <div class="card">
      <h2>📊 平台数据总览</h2>
      <div class="stat-grid">
        <div class="stat-item">
          <div class="num"><fmt:formatNumber value="${spotCount}" /></div>
          <div class="lbl">收录景点</div>
        </div>
        <div class="stat-item">
          <div class="num"><fmt:formatNumber value="${routeCount}" /></div>
          <div class="lbl">精选路线</div>
        </div>
        <div class="stat-item">
          <div class="num"><fmt:formatNumber value="${commentCount}" /></div>
          <div class="lbl">用户评论</div>
        </div>
        <div class="stat-item">
          <div class="num"><fmt:formatNumber value="${userCount}" /></div>
          <div class="lbl">注册用户</div>
        </div>
        <div class="stat-item">
          <div class="num"><fmt:formatNumber value="${favCount}" /></div>
          <div class="lbl">景点收藏</div>
        </div>
        <c:if test="${not empty stats.avgRating}">
          <div class="stat-item">
            <div class="num"><fmt:formatNumber value="${stats.avgRating}" pattern="0.0" /></div>
            <div class="lbl">平均评分</div>
          </div>
        </c:if>
      </div>
    </div>

    <!-- 技术栈 -->
    <div class="card">
      <h2>🔧 技术栈</h2>
      <div class="tech-list">
        <span class="tech-badge tech-spring">Spring 7.0.9 IoC</span>
        <span class="tech-badge tech-spring">Spring DI</span>
        <span class="tech-badge tech-aop">Spring AOP 审计日志</span>
        <span class="tech-badge tech-spring">声明式事务 @Transactional</span>
        <span class="tech-badge tech-mvc">Spring MVC 11 Controllers</span>
        <span class="tech-badge tech-mvc">REST + JSP 双视图</span>
        <span class="tech-badge tech-mybatis">MyBatis 3.5.19 注解SQL</span>
        <span class="tech-badge tech-mysql">MySQL 8 InnoDB</span>
        <span class="tech-badge tech-jsp">JSP + JSTL + EL (14页)</span>
        <span class="tech-badge tech-ai">DeepSeek AI</span>
        <span class="tech-badge tech-ai">通义千问</span>
        <span class="tech-badge tech-ai">豆包AI</span>
        <span class="tech-badge tech-ai">RAG 检索增强</span>
        <span class="tech-badge tech-frontend">原生JS SPA</span>
        <span class="tech-badge tech-frontend">CSS 3D VR 环景舱</span>
        <span class="tech-badge tech-frontend">Leaflet 地图</span>
      </div>
    </div>

    <!-- 五层架构: c:set 流程图 -->
    <div class="card">
      <h2>🏗 SSM 五层架构</h2>
      <div class="arch-flow">
        <span class="arch-node" style="background:#dbeafe;color:#1e40af">Controller</span>
        <span class="arch-arrow">→</span>
        <span class="arch-node" style="background:#d1fae5;color:#065f46">Service 接口</span>
        <span class="arch-arrow">→</span>
        <span class="arch-node" style="background:#d1fae5;color:#065f46">ServiceImpl</span>
        <span class="arch-arrow">→</span>
        <span class="arch-node" style="background:#fef3c7;color:#92400e">DAO 接口</span>
        <span class="arch-arrow">→</span>
        <span class="arch-node" style="background:#fef3c7;color:#92400e">DAOImpl</span>
        <span class="arch-arrow">→</span>
        <span class="arch-node" style="background:#fce7f3;color:#9d174d">Mapper (注解SQL)</span>
        <span class="arch-arrow">→</span>
        <span class="arch-node" style="background:#fce7f3;color:#9d174d">MySQL</span>
      </div>
    </div>

    <!-- 核心功能列表 -->
    <div class="card">
      <h2>✨ 核心功能</h2>
      <ul class="feature-list">
        <li><span class="icon">📍</span> 全国 219 个真实景点导览 (31 省)</li>
        <li><span class="icon">🗺</span> 16 条精选游览路线 + 虚拟打卡</li>
        <li><span class="icon">🤖</span> AI 助教三引擎故障转移 (DeepSeek → 通义 → 豆包 → 本地)</li>
        <li><span class="icon">🧭</span> 智能出游规划 (RAG 约束 AI 不编造景点)</li>
        <li><span class="icon">🎲</span> 云游盲盒随机抽景点</li>
        <li><span class="icon">🎧</span> 语音讲解 + 背景音乐</li>
        <li><span class="icon">🥽</span> CSS 3D VR 环景舱沉浸式体验</li>
        <li><span class="icon">📸</span> 真实景点图片流水线 (标题匹配 + MD5 去重 + 压缩)</li>
        <li><span class="icon">🗂</span> 管理端 CRUD + AOP 审计日志</li>
        <li><span class="icon">📄</span> 14 个 JSP 页面 (JSTL + EL 表达式)</li>
        <li><span class="icon">❤</span> 景点收藏 + 评论评分</li>
        <li><span class="icon">🗺</span> Leaflet 交互地图 (多源瓦片)</li>
      </ul>
    </div>

  </div>
  <div class="footer">
    <b>云游中国</b> · SSM (Spring + Spring MVC + MyBatis) 期末项目 · JSP + JSTL + EL + fmt:formatNumber
  </div>
</body>
</html>
