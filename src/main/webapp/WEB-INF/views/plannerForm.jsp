<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>🧭 智能出游规划 - 云游中国</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", sans-serif; background: #f0f4f8; color: #333; }
  .hero { background: linear-gradient(135deg, #0ea5e9, #7c3aed); color: #fff; padding: 34px 20px; text-align: center; }
  .hero h1 { font-size: 22px; }
  .hero p { font-size: 13px; opacity: .9; margin-top: 6px; }
  .nav { text-align: center; margin: 18px 0 10px; }
  .nav a { display: inline-block; padding: 7px 14px; margin: 0 4px; border-radius: 6px; background: #fff; color: #0ea5e9; text-decoration: none; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  .nav a:hover { background: #0ea5e9; color: #fff; }
  .container { max-width: 680px; margin: 0 auto; padding: 16px 16px 36px; }
  .card { background: #fff; border-radius: 14px; padding: 26px; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
  h2 { font-size: 17px; color: #0ea5e9; margin-bottom: 4px; }
  .lead { color: #94a3b8; font-size: 13px; margin-bottom: 20px; }
  .field { margin-bottom: 18px; }
  .field label { display: block; font-size: 13px; color: #334155; font-weight: 600; margin-bottom: 6px; }
  .field label .tip { color: #94a3b8; font-weight: 400; font-size: 12px; }
  .field input[type=text], .field input[type=number], .field select, .field textarea {
    width: 100%; padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 8px;
    font-size: 14px; font-family: inherit; transition: border-color .15s;
  }
  .field input:focus, .field select:focus, .field textarea:focus { outline: none; border-color: #0ea5e9; box-shadow: 0 0 0 3px rgba(14,165,233,.12); }
  .field textarea { min-height: 74px; resize: vertical; }
  /* 标签多选 */
  .tagbox { display: flex; flex-wrap: wrap; gap: 8px; }
  .tagbox input[type=checkbox] { display: none; }
  .tagbox label.tag {
    padding: 7px 14px; border: 1px solid #e2e8f0; border-radius: 999px;
    cursor: pointer; font-size: 13px; background: #fff; color: #475569; transition: all .15s;
  }
  .tagbox input[type=checkbox]:checked + label.tag {
    background: linear-gradient(135deg, #0ea5e9, #7c3aed); color: #fff; border-color: transparent;
  }
  .row2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
  .btn-submit {
    width: 100%; padding: 14px; border: 0; border-radius: 10px; color: #fff;
    background: linear-gradient(135deg, #0ea5e9, #7c3aed); font-size: 15px; font-weight: 700;
    cursor: pointer; box-shadow: 0 6px 20px rgba(14,165,233,.3); margin-top: 12px;
  }
  .btn-submit:hover { transform: translateY(-1px); box-shadow: 0 8px 24px rgba(14,165,233,.4); }
  .hint { text-align: center; color: #94a3b8; font-size: 12px; margin-top: 14px; }
  @media (max-width: 520px) { .row2 { grid-template-columns: 1fr; } }
</style>
</head>
<body>

<div class="hero">
  <h1>🧭 智能出游规划</h1>
  <p>填 5 项偏好 · AI 从 219 个真实景点里为你定制路线</p>
</div>
<div class="nav">
  <a href="${pageContext.request.contextPath}/page/home">首页</a>
  <a href="${pageContext.request.contextPath}/page/spots">景点</a>
  <a href="${pageContext.request.contextPath}/page/routes">路线</a>
  <a href="${pageContext.request.contextPath}/page/planner">🧭 智能规划</a>
</div>

<div class="container">
  <div class="card">
    <h2>告诉我你想去哪里</h2>
    <p class="lead">提交后由 AI 从平台 219 个真实景点中为你生成一条专属路线推荐(含逐日行程和门票参考)</p>

    <form action="${pageContext.request.contextPath}/page/planner/generate" method="post">
      <input type="hidden" name="id" value="${pref.id}"/>

      <div class="field">
        <label>计划天数 <span class="tip">(必填)</span></label>
        <input type="number" name="days" min="1" max="30" value="${pref.days}" required placeholder="例如 3 天"/>
      </div>

      <div class="field">
        <label>昵称 <span class="tip">(可选)</span></label>
        <input type="text" name="nickname" value="${pref.nickname}" placeholder="小王/小明..."/>
      </div>

      <div class="field">
        <label>意向省份 <span class="tip">(填省份中文名, 多个用逗号分隔, 留空=全国)</span></label>
        <input type="text" name="provinces" value="${pref.provinces}"
               placeholder="北京, 浙江, 四川"/>
      </div>

      <div class="field">
        <label>偏好类型 <span class="tip">(可多选, 不选=不限)</span></label>
        <div class="tagbox">
          <input type="checkbox" id="c1" name="categories" value="山水风光"/>
          <label class="tag" for="c1">🏞 山水风光</label>
          <input type="checkbox" id="c2" name="categories" value="古迹遗址"/>
          <label class="tag" for="c2">🏛 古迹遗址</label>
          <input type="checkbox" id="c3" name="categories" value="城市古镇"/>
          <label class="tag" for="c3">🏘 城市古镇</label>
          <input type="checkbox" id="c4" name="categories" value="主题乐园"/>
          <label class="tag" for="c4">🎡 主题乐园</label>
          <input type="checkbox" id="c5" name="categories" value="民俗文化"/>
          <label class="tag" for="c5">🎎 民俗文化</label>
          <input type="checkbox" id="c6" name="categories" value="自然奇观"/>
          <label class="tag" for="c6">🌋 自然奇观</label>
          <input type="checkbox" id="c7" name="categories" value="宗教名胜"/>
          <label class="tag" for="c7">⛪ 宗教名胜</label>
        </div>
        <!-- 需要逗号拼接: JS 收集 checked 值自动填回 hidden input -->
        <input type="hidden" id="catsHidden" name="categories" value="${pref.categories}"/>
      </div>

      <div class="row2">
        <div class="field">
          <label>预算区间</label>
          <select name="budget">
            <option value="">不限</option>
            <option value="免费" ${pref.budget=='免费'?'selected':''}>免费</option>
            <option value="100以内" ${pref.budget=='100以内'?'selected':''}>¥100 以内</option>
            <option value="100-300" ${pref.budget=='100-300'?'selected':''}>¥100 – 300</option>
            <option value="300-500" ${pref.budget=='300-500'?'selected':''}>¥300 – 500</option>
            <option value="500+" ${pref.budget=='500+'?'selected':''}>¥500 以上</option>
          </select>
        </div>
        <div class="field">
          <label>同行</label>
          <select name="companions">
            <option value="">不限</option>
            <option value="独自" ${pref.companions=='独自'?'selected':''}>独自</option>
            <option value="情侣" ${pref.companions=='情侣'?'selected':''}>情侣</option>
            <option value="家庭" ${pref.companions=='家庭'?'selected':''}>家庭亲子</option>
            <option value="朋友群" ${pref.companions=='朋友群'?'selected':''}>朋友群</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label>交通方式偏好</label>
        <select name="transport">
          <option value="">不限</option>
          <option value="自驾" ${pref.transport=='自驾'?'selected':''}>自驾</option>
          <option value="高铁" ${pref.transport=='高铁'?'selected':''}>高铁</option>
          <option value="飞机" ${pref.transport=='飞机'?'selected':''}>飞机</option>
          <option value="无所谓" ${pref.transport=='无所谓'?'selected':''}>无所谓</option>
        </select>
      </div>

      <div class="field">
        <label>特殊关注点 <span class="tip">(可选, 自由描述)</span></label>
        <textarea name="focus" placeholder="想拍古建筑摄影 / 想避开人潮 / 喜欢徒步 / 必须有儿童设施...">${pref.focus}</textarea>
      </div>

      <button type="submit" class="btn-submit">✨ 让 AI 为我规划路线</button>
      <p class="hint">由三引擎 AI 实时生成 · 从平台 219 个真实景点中挑选 · 不编造数据</p>
    </form>
  </div>
</div>

<script>
// 表单提交时把多选 checkbox 拼成逗号字符串写入 hidden input
document.querySelector('form').addEventListener('submit', function() {
  const cks = document.querySelectorAll('.tagbox input[type=checkbox]:checked');
  const arr = Array.from(cks).map(c => c.value);
  document.getElementById('catsHidden').value = arr.join(',');
});
// 编辑态回显: 从 hidden input 读逗号拆分打勾
window.addEventListener('DOMContentLoaded', function() {
  const v = document.getElementById('catsHidden').value;
  if (!v) return;
  v.split(',').forEach(function(val) {
    const c = document.querySelector('.tagbox input[type=checkbox][value="' + val.trim() + '"]');
    if (c) c.checked = true;
  });
});
</script>
</body>
</html>
