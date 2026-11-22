/* =============================================================
 * 云游中国 前端主逻辑 (原生 JS SPA)
 * 首页 / 全国景点导览 / 智能推荐 / 虚拟游览 / AI 助教 / 我的足迹
 * ============================================================= */
(function () {
  'use strict';

  /* ---------------- 工具 ---------------- */
  const $ = (sel, root) => (root || document).querySelector(sel);
  const $$ = (sel, root) => Array.from((root || document).querySelectorAll(sel));

  function esc(s) {
    if (s === null || s === undefined) return '';
    return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }
  const imgAttr = (url) => `src="${esc(url)}" onerror="this.style.opacity=0" loading="lazy"`;

  let toastTimer = null;
  function toast(msg) {
    const el = $('#toast');
    el.textContent = msg;
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.hidden = true; }, 2200);
  }

  function money(p) {
    const n = Number(p || 0);
    return n === 0 ? '<span class="price free">免费</span>' : `<span class="price">¥${n}</span>`;
  }
  function diffText(d) {
    return ['', '轻松', '较轻松', '中等', '较费力', '挑战'][d] || '轻松';
  }
  const CAT_ICONS = { mountain: '⛰️', water: '🌊', relic: '🏛️', garden: '🏯', town: '🏮', roller: '🎢', buddha: '🛕', beach: '🏖️' };

  /* 七大区域与 31 省级行政区(用于区域筛选与足迹地图) */
  const REGIONS = ['华北', '东北', '华东', '华中', '华南', '西南', '西北'];
  const REGION_ICON = { '华北': '🏯', '东北': '❄️', '华东': '🌊', '华中': '🌾', '华南': '🌴', '西南': '🐼', '西北': '🐪' };
  const PROVINCES = [
    '北京', '天津', '河北', '山西', '内蒙古',
    '辽宁', '吉林', '黑龙江',
    '上海', '江苏', '浙江', '安徽', '福建', '江西', '山东',
    '河南', '湖北', '湖南',
    '广东', '广西', '海南',
    '重庆', '四川', '贵州', '云南', '西藏',
    '陕西', '甘肃', '青海', '宁夏', '新疆',
  ];
  const PROVINCE_REGION = {};
  const REGION_PROVINCES = {
    '华北': ['北京', '天津', '河北', '山西', '内蒙古'],
    '东北': ['辽宁', '吉林', '黑龙江'],
    '华东': ['上海', '江苏', '浙江', '安徽', '福建', '江西', '山东'],
    '华中': ['河南', '湖北', '湖南'],
    '华南': ['广东', '广西', '海南'],
    '西南': ['重庆', '四川', '贵州', '云南', '西藏'],
    '西北': ['陕西', '甘肃', '青海', '宁夏', '新疆'],
  };
  REGIONS.forEach(r => REGION_PROVINCES[r].forEach(p => { PROVINCE_REGION[p] = r; }));

  /* ---------------- 全局状态 ---------------- */
  const state = {
    tab: 'home',
    me: null,
    categories: [],
    explore: { categoryId: null, province: '', region: '', freeOnly: false, keyword: '', tag: '', sort: 'default', maxDifficulty: null, page: 1, size: 9, totalPages: 1 },
    recommend: { interests: new Set(), durationMinutes: 180, budget: 200, crowd: '', energy: '适中', province: '', region: '' },
    provinces: [],
    tourSession: localStorage.getItem('tourSession') || '',
    tour: { route: null, index: 0, visited: new Set(), auto: false,
            travelMode: 'driving', plan: { travelDate: '', travelMinutes: 240, travelMode: 'driving' } },
    chat: [],
    aiProvider: localStorage.getItem('aiProvider') || 'auto',
    aiProviders: [],
    commentRating: 5,
    planner: { days: 3, loading: false, plan: null, err: null },
  };

  /* ---------------- 语音讲解 ---------------- */
  let speaking = false;
  function speak(text, onEnd) {
    if (!('speechSynthesis' in window)) { toast('当前浏览器不支持语音讲解'); return; }
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = 'zh-CN';
    u.rate = 1; u.pitch = 1;
    const voices = window.speechSynthesis.getVoices();
    const zh = voices.find(v => v.lang && v.lang.toLowerCase().startsWith('zh'));
    if (zh) u.voice = zh;
    u.onend = () => { speaking = false; onEnd && onEnd(); };
    u.onerror = () => { speaking = false; onEnd && onEnd(); };
    speaking = true;
    window.speechSynthesis.speak(u);
  }
  function stopSpeak() {
    if ('speechSynthesis' in window) window.speechSynthesis.cancel();
    speaking = false;
  }

  /* ---------------- 导航 ---------------- */
  function switchTab(tab) {
    if (spaceCleanup) { spaceCleanup(); spaceCleanup = null; }
    state.tab = tab;
    $$('.nav-item').forEach(a => a.classList.toggle('active', a.dataset.tab === tab));
    stopSpeak();
    render();
    window.scrollTo({ top: 0 });
  }

  $('#nav').addEventListener('click', e => {
    const item = e.target.closest('.nav-item');
    if (item) switchTab(item.dataset.tab);
  });
  $('.brand').addEventListener('click', () => switchTab('home'));
  $('#floatAi').addEventListener('click', () => switchTab('ai'));

  /* ---------------- 云游盲盒: 随机抽一个上架景点直接前往 ---------------- */
  $('#blindBox').addEventListener('click', async () => {
    const btn = $('#blindBox');
    if (btn.dataset.busy) return;
    btn.dataset.busy = '1';
    btn.style.opacity = '0.5';
    try {
      const spot = await API.get('/api/spots/random');
      if (spot && spot.id) {
        toast(`🎲 盲盒抽中: ${spot.name} · ${spot.province || ''}`);
        openSpot(spot.id);
      }
    } catch (e) {
      toast('盲盒开箱失败, 再试一次');
    } finally {
      delete btn.dataset.busy;
      btn.style.opacity = '';
    }
  });

  /* ---------------- 背景音乐(默认静音, 点击喇叭开启; 循环播放) ----------------
     《国风·云游》12134.com 免费商用本地 mp3 */
  (function initBgm() {
    const btn = $('#bgmToggle');
    if (!btn) return;
    const KEY = 'yunyou_bgm_on';
    const TRACKS = [
      { type: 'local', src: 'static/audio/bgm.mp3', name: '《国风·云游》· 12134.com 免费商用' }
    ];
    const enabled = () => localStorage.getItem(KEY) === '1';
    let idx = 0, host = null, swapTimer = null, curAudio = null;
    try { idx = +(localStorage.getItem(KEY + '_idx') || 0) % TRACKS.length; } catch (e) { /* 忽略 */ }
    function paint(on) {
      btn.textContent = on ? '🔊' : '🔈';
      btn.classList.toggle('on', on);
      btn.title = on ? '关闭背景音乐' : '播放背景音乐（国风·云游）';
    }
    function stop() {
      if (swapTimer) { clearTimeout(swapTimer); swapTimer = null; }
      if (curAudio) { const a = curAudio; curAudio = null; a.onended = null; a.pause(); }
      if (host) { host.remove(); host = null; }   // 移除 iframe 即停止播放
      paint(false);
    }
    function advance() {
      idx = (idx + 1) % TRACKS.length;
      try { localStorage.setItem(KEY + '_idx', idx); } catch (e) { /* 忽略 */ }
      if (enabled()) play();
    }
    function play() {
      stop();
      const t = TRACKS[idx];
      host = document.createElement('div');
      host.className = 'bgm-embed';
      let bodyHtml = '<div class="bgm-embed-audio"><span>🎶</span><span>🎵</span><span>🎶</span></div>';
      curAudio = new Audio(t.src);
      curAudio.volume = 0.45;
      curAudio.onended = () => { if (curAudio) advance(); };   // 播完自动循环下一轮
      curAudio.play().catch(() => { /* 浏览器拦截时等用户再点喇叭 */ });
      host.innerHTML = '<div class="bgm-embed-head"><span>🎵 ' + t.name + '</span>'
        + '<button type="button" aria-label="关闭音乐">×</button></div>' + bodyHtml;
      document.body.appendChild(host);
      host.querySelector('.bgm-embed-head button').addEventListener('click', () => { localStorage.setItem(KEY, '0'); stop(); });
      paint(true);
    }
    btn.addEventListener('click', () => {
      if (enabled()) { localStorage.setItem(KEY, '0'); stop(); return; }
      localStorage.setItem(KEY, '1');
      play();
      toast('🎵 ' + TRACKS[idx].name);
    });
    paint(enabled());
    // 上次开启过: 浏览器禁止无交互自动播放, 等用户首次点击页面后恢复播放
    if (enabled()) {
      document.addEventListener('click', function resumeBgm(e) {
        document.removeEventListener('click', resumeBgm);
        // 首次点击若恰好是喇叭自身, 交给按钮逻辑处理, 避免一开一关冲突
        if (e.target.closest && e.target.closest('#bgmToggle')) return;
        if (enabled() && !host && !curAudio) play();
      });
    }
  })();
  window.addEventListener('app:unauthorized', () => {
    state.me = null;
    renderUserArea();
    toast('请先登录');
  });

  /* ---------------- 用户区 ---------------- */
  async function loadMe() {
    try { state.me = await API.get('/api/auth/me'); } catch (e) { state.me = null; }
    renderUserArea();
  }
  function renderUserArea() {
    const area = $('#userArea');
    if (state.me) {
      area.innerHTML = `
        <div class="user-chip" data-tab="me" style="cursor:pointer">
          <span class="avatar">${esc((state.me.nickname || state.me.username).slice(0, 1))}</span>
          <span>${esc(state.me.nickname || state.me.username)}</span>
        </div>`;
      area.querySelector('.user-chip').onclick = () => switchTab('me');
    } else {
      area.innerHTML = `<button class="btn btn-primary" id="loginBtn">登录 / 注册</button>`;
      $('#loginBtn').onclick = () => switchTab('me');
    }
  }

  /* ---------------- 公共卡片 ---------------- */
  function spotCard(s) {
    const fav = s.favorited ? '❤️' : '🤍';
    return `
    <div class="spot-card" data-spot="${s.id}">
      <div class="spot-img">
        <img ${imgAttr(s.imageUrl)} alt="${esc(s.name)}" />
        <span class="spot-badge">${esc(s.categoryName)}</span>
        <button class="spot-fav" data-fav="${s.id}" title="收藏">${fav}</button>
      </div>
      <div class="spot-body">
        <div class="spot-name">${esc(s.name)} <span class="spot-rating">⭐ ${s.rating.toFixed(1)} (${s.ratingCount})</span></div>
        <div class="spot-loc">📍 ${esc(s.province)} · ${esc(s.city || '')}</div>
        <div class="spot-summary">${esc(s.summary)}</div>
        <div class="tag-row">${s.tags.slice(0, 3).map(t => `<span class="tag">${esc(t)}</span>`).join('')}</div>
        <div class="spot-meta"><span>⏱ ${s.durationMinutes}分钟 · ${diffText(s.difficulty)}</span>${money(s.price)}</div>
      </div>
    </div>`;
  }

  /* ============================================================
   * 首页
   * ============================================================ */
  async function renderHome() {
    const app = $('#app');
    app.innerHTML = `<div class="empty"><div class="big">🇨🇳</div>云游中国加载中…</div>`;
    try {
      const [stats, hot, topRated, routes, categories, vrHot] = await Promise.all([
        API.get('/api/stats'),
        API.get('/api/spots/hot?n=8'),
        API.get('/api/spots/top-rated?n=8'),
        API.get('/api/routes'),
        API.get('/api/categories'),
        API.get('/api/vr/hot?n=4').catch(() => []),   // SSM 落库的 VR 热度榜, 无记录时隐藏
      ]);
      if (state.tab !== 'home') return; // 用户已切走, 不覆盖当前页面
      state.categories = categories;
      state.provinces = stats.provinceDistribution || [];
      const heroImg = hot[0] ? hot[0].imageUrl : '';
      const pvMap = Object.fromEntries(state.provinces.map(p => [p.province, p]));
      const catCount = name => (stats.categoryDistribution.find(x => x.name === name) || {}).count || 0;
      app.innerHTML = `
      <section class="hero">
        <img ${imgAttr(heroImg)} alt="云游中国" />
        <div class="hero-content">
          <h1>云游中国 · 星际探索<br/>全国景点智能导览 × 星际科普，畅游山河与星河</h1>
          <p>${stats.spotCount} 处真实景点覆盖 ${stats.provinceCount} 个省级行政区，${stats.routeCount} 条跨区域主题路线；通义千问/DeepSeek/豆包AI 多引擎 + 图文语音虚拟游览，带你走遍中国。</p>
          <div class="hero-search">
            <input id="heroKw" placeholder="搜索景点 / 省份 / 城市，如：故宫、四川、免费…" />
            <button id="heroSearchBtn">🔍 搜索</button>
          </div>
          <div class="hero-actions">
            <button id="heroVrBtn" type="button">🥽 进入 VR 环景舱</button>
            <button id="heroBoxBtn" type="button">🎲 云游盲盒</button>
          </div>
          <div class="hero-stats">
            <div class="hero-stat"><b>${stats.spotCount}</b><span>全国景点</span></div>
            <div class="hero-stat"><b>${stats.provinceCount}</b><span>省级行政区</span></div>
            <div class="hero-stat"><b>${stats.routeCount}</b><span>主题路线</span></div>
            <div class="hero-stat"><b>${stats.commentCount}</b><span>真实点评</span></div>
          </div>
        </div>
      </section>

      <div class="section-title">🧭 七大区域速达</div>
      <div class="region-row">
        ${REGIONS.map(rg => {
          const rd = (stats.regionDistribution || []).find(x => x.region === rg) || {};
          return `<a class="region-card" data-region="${rg}">
            <div class="region-icon">${REGION_ICON[rg]}</div>
            <b>${rg}</b>
            <small>${rd.count || 0} 个景点 · ${(REGION_PROVINCES[rg] || []).length} 省市</small>
          </a>`;
        }).join('')}
      </div>

      <div class="section-title">🗺️ 全国省份导览<a class="more" data-go="explore">查看全部景点 →</a></div>
      <div class="province-grid">
        ${PROVINCES.map(p => {
          const d = pvMap[p] || {};
          const c = d.count || 0;
          return `<a class="province-cell ${c ? '' : 'dim'}" data-province="${p}">
            <b>${p}</b><span>${c ? c + ' 处景点' : '敬请期待'}</span>
          </a>`;
        }).join('')}
      </div>

      <div class="section-title">🏞️ 景点分类</div>
      <div class="cat-row">
        ${categories.map(c => `
          <a class="cat-card" data-cat="${c.id}">
            <div class="cat-icon">${CAT_ICONS[c.icon] || '📍'}</div>
            <b>${esc(c.name)}</b>
            <small>${catCount(c.name)} 个景点</small>
          </a>`).join('')}
      </div>

      <div class="section-title">🔥 全国人气热门<a class="more" data-go="explore">查看全部景点 →</a></div>
      <div class="grid">${hot.slice(0, 8).map(spotCard).join('')}</div>

      <div class="section-title">⭐ 全国口碑榜</div>
      <div class="grid-4">${topRated.slice(0, 8).map(spotCard).join('')}</div>

      ${vrHot && vrHot.length ? `
      <div class="section-title">🥽 VR 环景热度榜 <small style="font-size:12px;color:#94a3b8">实时统计自 vr_view_log 表 · VR 打卡落库 · 点卡片进详情开启环景</small></div>
      <div class="grid-4">${vrHot.map(v => `
        <div class="vr-hot-card" data-spot="${v.id}">
          <div class="vhc-cover"><img ${imgAttr(v.imageUrl)} alt="${esc(v.name)}" loading="lazy"/>
            <span class="vr-count-badge">🌐 VR 游览 ${v.vrCount} 次</span></div>
          <b>${esc(v.name)}</b><small>${esc(v.province || '')}</small>
        </div>`).join('')}</div>` : ''}

      <div class="section-title">🧳 跨区域主题虚拟游览路线<a class="more" data-go="tour">去虚拟游览 →</a></div>
      <div class="route-grid">
        ${routes.slice(0, 6).map(r => `
          <div class="route-card" data-route="${r.id}">
            <div class="route-cover"><img ${imgAttr(r.coverUrl)} alt="${esc(r.name)}"/><b>${esc(r.name)}</b></div>
            <div class="route-info">
              <p>${esc(r.subtitle)}</p>
              <div class="spot-meta"><span>${r.totalSpots} 站 · 约 ${r.durationMinutes} 分钟</span>${r.tags.slice(0,2).map(t=>`<span class="tag">${esc(t)}</span>`).join('')}</div>
            </div>
          </div>`).join('')}
      </div>

      <div class="section-title">🤖 智能服务</div>
      <div class="cat-row">
        <a class="cat-card" data-go="recommend"><div class="cat-icon">🎯</div><b>个性化推荐</b><small>兴趣/预算/目的地定制</small></a>
        <a class="cat-card" data-go="tour"><div class="cat-icon">🧭</div><b>虚拟游览打卡</b><small>路线地图连线 + 语音讲解</small></a>
        <a class="cat-card" data-go="ai"><div class="cat-icon">🤖</div><b>豆包AI 助教</b><small>通义千问/DeepSeek/豆包AI 在线问答</small></a>
        <a class="cat-card" data-go="me"><div class="cat-icon">🗺️</div><b>我的足迹地图</b><small>点亮省份收集徽章</small></a>
      </div>

      <div class="section-title">📄 JSP 传统视图页面 <small style="font-size:12px;color:#94a3b8">SSM 视图层 · JSTL + EL 表达式</small></div>
      <div class="cat-row">
        <a class="cat-card" href="/page/home"><div class="cat-icon">🏠</div><b>JSP 首页</b><small>统计概览 + 热门景点</small></a>
        <a class="cat-card" href="/page/spots"><div class="cat-icon">📍</div><b>景点列表</b><small>JSTL 分页 + forEach</small></a>
        <a class="cat-card" href="/page/ranking"><div class="cat-icon">🏆</div><b>景点排行榜</b><small>varStatus + c:choose 奖牌</small></a>
        <a class="cat-card" href="/page/provinces"><div class="cat-icon">🗺️</div><b>省份导览</b><small>嵌套 forEach + EL 算术</small></a>
        <a class="cat-card" href="/page/routes"><div class="cat-icon">🧳</div><b>路线列表</b><small>JSTL 渲染路线卡片</small></a>
        <a class="cat-card" href="/page/stats"><div class="cat-icon">📊</div><b>数据统计</b><small>分布图表 + JSTL</small></a>
        <a class="cat-card" href="/page/tour/records"><div class="cat-icon">👣</div><b>游览足迹</b><small>时间线 + 省份点亮</small></a>
        <a class="cat-card" href="/page/favorites"><div class="cat-icon">❤️</div><b>收藏夹</b><small>c:choose + empty 判断</small></a>
        <a class="cat-card" href="/page/planner"><div class="cat-icon">🧭</div><b>智能规划表单</b><small>@ModelAttribute + DeepSeek AI</small></a>
        <a class="cat-card" href="/page/about"><div class="cat-icon">ℹ️</div><b>关于平台</b><small>技术栈 + fmt:formatNumber</small></a>
      </div>`;

      $('#heroSearchBtn').onclick = () => {
        state.explore.keyword = $('#heroKw').value.trim();
        state.explore.page = 1;
        switchTab('explore');
      };
      $('#heroKw').addEventListener('keydown', e => { if (e.key === 'Enter') $('#heroSearchBtn').click(); });
      /* 首页一键直达: VR 环景舱(用热门第一的景点实拍图构建 3D 图墙) + 云游盲盒 */
      $('#heroVrBtn').onclick = () => { if (hot[0]) openVr(hot[0]); };
      $('#heroBoxBtn').onclick = () => { const bb = $('#blindBox'); if (bb) bb.click(); };
      bindCardEvents(app);
      app.querySelectorAll('[data-go]').forEach(el => el.onclick = () => switchTab(el.dataset.go));
      app.querySelectorAll('[data-cat]').forEach(el => el.onclick = () => {
        state.explore.categoryId = Number(el.dataset.cat);
        state.explore.page = 1;
        switchTab('explore');
      });
      app.querySelectorAll('[data-region]').forEach(el => el.onclick = () => {
        state.explore.region = el.dataset.region;
        state.explore.province = '';
        state.explore.page = 1;
        switchTab('explore');
      });
      app.querySelectorAll('[data-province]').forEach(el => el.onclick = () => {
        state.explore.province = el.dataset.province;
        state.explore.region = PROVINCE_REGION[el.dataset.province] || '';
        state.explore.page = 1;
        switchTab('explore');
      });
      app.querySelectorAll('[data-route]').forEach(el => el.onclick = () => startTour(Number(el.dataset.route)));
    } catch (e) {
      app.innerHTML = `<div class="empty"><div class="big">😢</div>首页加载失败：${esc(e.message)}<br/><button class="btn btn-primary" style="margin-top:14px" onclick="location.reload()">重试</button></div>`;
    }
  }

  /* ============================================================
   * 景点导览
   * ============================================================ */
  async function renderExplore() {
    const app = $('#app');
    const x = state.explore;
    if (!state.categories.length) {
      try { state.categories = await API.get('/api/categories'); } catch (e) {}
    }
    if (!state.provinces.length) {
      try { state.provinces = await API.get('/api/spots/provinces'); } catch (e) {}
    }
    app.innerHTML = `
      <div class="filter-bar">
        <div class="filter-line">
          <span class="filter-label">区域</span>
          <button class="chip ${x.region ? '' : 'active'}" data-region="">全国</button>
          ${REGIONS.map(r => `<button class="chip ${x.region === r ? 'active' : ''}" data-region="${r}">${REGION_ICON[r]} ${r}</button>`).join('')}
        </div>
        <div class="filter-line">
          <span class="filter-label">省份</span>
          <select class="input" id="pvSel" style="width:130px">
            <option value="">全部省份</option>
            ${(x.region ? REGION_PROVINCES[x.region] : PROVINCES).map(p => `<option value="${p}" ${x.province === p ? 'selected' : ''}>${p}</option>`).join('')}
          </select>
          <label class="free-switch"><input type="checkbox" id="freeChk" ${x.freeOnly ? 'checked' : ''}/> 只看免费</label>
        </div>
        <div class="filter-line">
          <span class="filter-label">分类</span>
          <button class="chip ${x.categoryId ? '' : 'active'}" data-cid="">全部</button>
          ${state.categories.map(c => `<button class="chip ${x.categoryId === c.id ? 'active' : ''}" data-cid="${c.id}">${CAT_ICONS[c.icon] || ''} ${esc(c.name)}</button>`).join('')}
        </div>
        <div class="filter-line">
          <span class="filter-label">强度</span>
          <button class="chip ${x.maxDifficulty ? '' : 'active'}" data-diff="">不限</button>
          <button class="chip ${x.maxDifficulty === 1 ? 'active' : ''}" data-diff="1">轻松可达</button>
          <button class="chip ${x.maxDifficulty === 3 ? 'active' : ''}" data-diff="3">一般体力</button>
          <button class="chip ${x.maxDifficulty === 5 ? 'active' : ''}" data-diff="5">全部挑战</button>
        </div>
        <div class="filter-line">
          <div class="filter-search">
            <input class="input" id="kw" value="${esc(x.keyword)}" placeholder="搜索景点名 / 省份 / 城市 / 标签 / 关键字" />
            <button class="btn btn-primary" id="searchBtn">搜索</button>
          </div>
          <span class="filter-label" style="margin-left:auto">排序</span>
          <select class="input" id="sortSel">
            <option value="default">人气优先</option>
            <option value="rating">评分最高</option>
            <option value="priceAsc">价格最低</option>
            <option value="priceDesc">价格从高到低</option>
            <option value="durationAsc">用时最短</option>
            <option value="difficultyAsc">最轻松</option>
            <option value="province">按省份排序</option>
          </select>
        </div>
      </div>
      <div id="spotGrid"><div class="empty"><div class="big">🧭</div>加载中…</div></div>`;
    $('#sortSel').value = x.sort;
    $('#sortSel').onchange = (e) => { x.sort = e.target.value; x.page = 1; loadSpots(); };
    $('#searchBtn').onclick = () => { x.keyword = $('#kw').value.trim(); x.page = 1; loadSpots(); };
    $('#kw').addEventListener('keydown', e => { if (e.key === 'Enter') $('#searchBtn').click(); });
    $('#pvSel').onchange = (e) => { x.province = e.target.value; x.page = 1; loadSpots(); };
    $('#freeChk').onchange = (e) => { x.freeOnly = e.target.checked; x.page = 1; loadSpots(); };
    app.querySelectorAll('[data-region]').forEach(b => b.onclick = () => {
      x.region = b.dataset.region;
      if (x.region && !(REGION_PROVINCES[x.region] || []).includes(x.province)) x.province = '';
      x.page = 1;
      renderExplore();
    });
    app.querySelectorAll('[data-cid]').forEach(b => b.onclick = () => {
      const v = b.dataset.cid;
      x.categoryId = v ? Number(v) : null;
      x.page = 1;
      renderExplore();
    });
    app.querySelectorAll('[data-diff]').forEach(b => b.onclick = () => {
      const v = b.dataset.diff;
      x.maxDifficulty = v ? Number(v) : null;
      x.page = 1;
      renderExplore();
    });
    loadSpots();
  }

  async function loadSpots() {
    const box = $('#spotGrid');
    try {
      const x = state.explore;
      const p = new URLSearchParams();
      p.set('page', x.page);
      p.set('size', x.size);
      p.set('sort', x.sort);
      if (x.categoryId) p.set('categoryId', x.categoryId);
      if (x.province) p.set('province', x.province);
      if (x.region) p.set('region', x.region);
      if (x.freeOnly) p.set('freeOnly', 'true');
      if (x.keyword) p.set('keyword', x.keyword);
      if (x.tag) p.set('tag', x.tag);
      if (x.maxDifficulty) p.set('maxDifficulty', x.maxDifficulty);
      const data = await API.get('/api/spots?' + p.toString());
      state.explore.totalPages = data.totalPages || 1;
      const scope = [x.region, x.province, x.keyword].filter(Boolean).join(' · ') || '全国';
      if (!data.list.length) {
        box.innerHTML = `<div class="empty"><div class="big">🔍</div>「${esc(scope)}」没有找到符合条件的景点，换个筛选试试吧</div>`;
        return;
      }
      box.innerHTML = `<div class="result-bar"><span>📍 ${esc(scope)} · 共找到 <b>${data.total}</b> 个景点</span></div>
        <div class="grid">${data.list.map(spotCard).join('')}</div>
        <div class="pager">
          <button class="btn" id="prevPage" ${x.page <= 1 ? 'disabled' : ''}>← 上一页</button>
          <span class="btn btn-ghost" style="cursor:default">第 ${data.page} / ${data.totalPages} 页</span>
          <button class="btn" id="nextPage" ${x.page >= data.totalPages ? 'disabled' : ''}>下一页 →</button>
        </div>`;
      $('#prevPage').onclick = () => { x.page--; loadSpots(); window.scrollTo({ top: 120 }); };
      $('#nextPage').onclick = () => { x.page++; loadSpots(); window.scrollTo({ top: 120 }); };
      bindCardEvents(box);
    } catch (e) {
      box.innerHTML = `<div class="empty"><div class="big">😢</div>${esc(e.message)}</div>`;
    }
  }

  /* ============================================================
   * 智能出游规划: 填写偏好 → AI 三引擎生成路线推荐
   * SPA 前后端分离版(与 JSP /page/planner 传统方案并存)
   * ============================================================ */
  async function renderPlanner() {
    const app = $('#app');
    const p = state.planner;
    const fmtProvider = n => ({ deepseek: '⚡ DeepSeek', qwen: '⚡ 通义千问', doubao: '⚡ 豆包AI', local: '📚 本地规则引擎(AI 暂不可用)' }[n] || n);

    function showForm() {
      app.innerHTML = `
      <section class="planner-wrap">
        <div class="planner-head">
          <h1>🧭 智能出游规划</h1>
          <p>填 5 项偏好，AI 从 219 个真实景点里为你定制一条专属路线</p>
        </div>
        <form class="planner-form" id="plannerForm">
          <div class="pf-row">
            <div class="pf-field">
              <label>计划天数 *</label>
              <input type="number" id="pfDays" min="1" max="30" value="${p.days}" required/>
            </div>
            <div class="pf-field">
              <label>昵称(可选)</label>
              <input type="text" id="pfNick" placeholder="小王/小明"/>
            </div>
          </div>
          <div class="pf-field">
            <label>意向省份(中文, 多个逗号分隔, 留空=全国)</label>
            <input type="text" id="pfProvinces" placeholder="北京, 浙江, 四川"/>
          </div>
          <div class="pf-field">
            <label>偏好类型(可多选)</label>
            <div class="pf-tags">
              ${['山水风光','古迹遗址','城市古镇','主题乐园','民俗文化','自然奇观','宗教名胜'].map(t =>
                `<label class="pf-tag"><input type="checkbox" value="${t}"/><span>${t}</span></label>`
              ).join('')}
            </div>
          </div>
          <div class="pf-row">
            <div class="pf-field">
              <label>预算</label>
              <select id="pfBudget">
                <option value="">不限</option>
                <option value="免费">免费</option>
                <option value="100以内">¥100 以内</option>
                <option value="100-300">¥100 – 300</option>
                <option value="300-500">¥300 – 500</option>
                <option value="500+">¥500 以上</option>
              </select>
            </div>
            <div class="pf-field">
              <label>同行</label>
              <select id="pfComp">
                <option value="">不限</option>
                <option value="独自">独自</option>
                <option value="情侣">情侣</option>
                <option value="家庭">家庭亲子</option>
                <option value="朋友群">朋友群</option>
              </select>
            </div>
          </div>
          <div class="pf-field">
            <label>交通方式</label>
            <select id="pfTrans">
              <option value="">不限</option>
              <option value="自驾">自驾</option>
              <option value="高铁">高铁</option>
              <option value="飞机">飞机</option>
              <option value="无所谓">无所谓</option>
            </select>
          </div>
          <div class="pf-field">
            <label>特殊关注点(可选)</label>
            <textarea id="pfFocus" placeholder="想拍古建筑摄影 / 避开人潮 / 喜欢徒步 / 有儿童设施..."></textarea>
          </div>
          <button type="submit" class="pf-submit" ${p.loading ? 'disabled' : ''}>
            ${p.loading ? '⏳ AI 正在规划中...' : '✨ 让 AI 为我规划路线'}
          </button>
          <p class="pf-hint">由三引擎 AI 实时生成 · 从平台 219 个真实景点中挑选 · 不编造数据</p>
        </form>
      </section>`;

      $('#plannerForm').addEventListener('submit', async e => {
        e.preventDefault();
        const cats = Array.from(document.querySelectorAll('.pf-tags input:checked')).map(c => c.value);
        const body = {
          days: +$('#pfDays').value,
          nickname: $('#pfNick').value.trim(),
          provinces: $('#pfProvinces').value.trim(),
          categories: cats.join(','),
          budget: $('#pfBudget').value,
          companions: $('#pfComp').value,
          transport: $('#pfTrans').value,
          focus: $('#pfFocus').value.trim(),
        };
        state.planner.loading = true;
        state.planner.err = null;
        renderPlanner();
        try {
          const res = await API.post('/api/planner/generate', body);
          state.planner.plan = res.data || res;
        } catch (err) {
          state.planner.err = err.message || 'AI 规划失败';
        } finally {
          state.planner.loading = false;
          renderPlanner();
        }
      });
    }

    function showPlan(plan) {
      const spots = plan.spots || [];
      const spotHtml = spots.length ? spots.map(s => {
        const priceTxt = (s.price == null || s.price === 0) ? '免费' : ('¥' + s.price);
        return `<div class="pl-spot" data-day="${s.day || '?'}">
          <b>${esc(s.name)}</b><small>${esc(s.province || '')}</small>
          <div class="pl-meta"><span>🎟 ${priceTxt}</span><span>⏱ ${s.stay_hours || 3}h</span>${s.rating ? `<span>⭐ ${Number(s.rating).toFixed(1)}</span>` : ''}</div>
        </div>`;
      }).join('') : '<div class="empty"><div class="big">🤖</div>AI 回复里没有景点列表</div>';

      app.innerHTML = `
      <section class="planner-wrap">
        <div class="planner-result-head">
          <h1>🗺 ${esc(plan.title || '我的 AI 路线')}</h1>
          <p>${esc(plan.summary || '')}</p>
          <span class="pl-engine" title="由哪个 AI 引擎生成">${fmtProvider(plan.provider)}</span>
        </div>
        <div class="planner-card">
          <h3>📍 推荐景点(按天数排序)</h3>
          <div class="pl-spot-list">${spotHtml}</div>
        </div>
        <div class="planner-card">
          <h3>📝 逐日行程(AI 生成)</h3>
          <pre class="pl-itinerary">${esc(plan.itinerary || '')}</pre>
        </div>
        <button class="pf-submit" id="pfAgain">🔄 换一批偏好 · 再规划一次</button>
      </section>`;
      $('#pfAgain').onclick = () => { state.planner.plan = null; state.planner.err = null; renderPlanner(); };
    }

    if (state.planner.loading) {
      app.innerHTML = `<div class="empty"><div class="big">🧭</div>AI 正在规划路线, 请稍候…</div>`;
    } else if (state.planner.plan) {
      showPlan(state.planner.plan);
    } else if (state.planner.err) {
      app.innerHTML = `<div class="empty"><div class="big">😢</div>${esc(state.planner.err)}</div>
        <button class="btn" onclick="switchTab('planner')">重试</button>`;
    } else {
      showForm();
    }
  }

  /* ============================================================
   * 智能推荐
   * ============================================================ */
  const ALL_TAGS = ['登山', '徒步', '摄影', '日出', '云海', '湖泊', '瀑布', '佛教', '历史', '石刻', '美食', '民俗', '非遗', '亲子', '研学', '刺激', '网红', '露营', '星空', '温泉', '康养', '茶园', '索道', '夜游', '咖啡'];

  function renderRecommend() {
    const r = state.recommend;
    $('#app').innerHTML = `
    <div class="reco-layout">
      <div class="panel">
        <h3>🎯 告诉 AI 你的偏好</h3>
        <div class="form-group">
          <label>兴趣标签（可多选）</label>
          <div class="opt-row" id="tagChips">
            ${ALL_TAGS.map(t => `<button class="chip ${r.interests.has(t) ? 'active' : ''}" data-tag="${t}">${t}</button>`).join('')}
          </div>
        </div>
        <div class="form-group">
          <label>本次可游玩时长</label>
          <select class="input" id="durationSel" style="width:100%">
            <option value="60">1 小时左右（打卡）</option>
            <option value="120">半天以内</option>
            <option value="180" selected>半天 ~ 一天</option>
            <option value="300">一整天</option>
            <option value="600">两天深度游</option>
          </select>
        </div>
        <div class="form-group">
          <label>单个项目预算</label>
          <select class="input" id="budgetSel" style="width:100%">
            <option value="0">只看免费景点</option>
            <option value="50">50 元以内</option>
            <option value="100">100 元以内</option>
            <option value="200" selected>200 元以内</option>
            <option value="9999">预算充足</option>
          </select>
        </div>
        <div class="form-group">
          <label>目的地区域偏好</label>
          <div class="opt-row" id="regionChips">
            <button class="chip ${r.region ? '' : 'active'}" data-region="">不限</button>
            ${REGIONS.map(c => `<button class="chip ${r.region === c ? 'active' : ''}" data-region="${c}">${REGION_ICON[c]} ${c}</button>`).join('')}
          </div>
        </div>
        <div class="form-group">
          <label>心仪省份（可细化区域内偏好）</label>
          <select class="input" id="pvChosen" style="width:100%">
            <option value="">不限省份</option>
            ${(r.region ? REGION_PROVINCES[r.region] : PROVINCES).map(p => `<option value="${p}" ${r.province === p ? 'selected' : ''}>${p}</option>`).join('')}
          </select>
        </div>
        <div class="form-group">
          <label>同行人群</label>
          <div class="opt-row" id="crowdChips">
            ${['亲子', '情侣', '老人', '朋友', '独自'].map(c => `<button class="chip ${r.crowd === c ? 'active' : ''}" data-crowd="${c}">${c}</button>`).join('')}
          </div>
        </div>
        <div class="form-group">
          <label>体力偏好</label>
          <div class="opt-row" id="energyChips">
            ${['轻松', '适中', '挑战'].map(c => `<button class="chip ${r.energy === c ? 'active' : ''}" data-energy="${c}">${c}</button>`).join('')}
          </div>
        </div>
        <button class="btn btn-primary" id="recoBtn" style="width:100%;padding:12px">✨ 生成我的专属推荐</button>
      </div>
      <div id="recoResult">
        <div class="empty"><div class="big">🎯</div>选择左侧偏好，点击「生成专属推荐」<br/>AI 将综合兴趣、口碑、热度、人群、时长与预算为你打分排序</div>
      </div>
    </div>`;
    $('#durationSel').value = String(r.durationMinutes);
    $('#budgetSel').value = String(r.budget);
    $('#tagChips').addEventListener('click', e => {
      const b = e.target.closest('[data-tag]'); if (!b) return;
      r.interests.has(b.dataset.tag) ? r.interests.delete(b.dataset.tag) : r.interests.add(b.dataset.tag);
      b.classList.toggle('active');
    });
    $('#crowdChips').addEventListener('click', e => {
      const b = e.target.closest('[data-crowd]'); if (!b) return;
      r.crowd = r.crowd === b.dataset.crowd ? '' : b.dataset.crowd;
      renderRecommend();
    });
    $('#energyChips').addEventListener('click', e => {
      const b = e.target.closest('[data-energy]'); if (!b) return;
      r.energy = b.dataset.energy; renderRecommend();
    });
    $('#durationSel').onchange = e => r.durationMinutes = Number(e.target.value);
    $('#budgetSel').onchange = e => r.budget = Number(e.target.value);
    $('#pvChosen').onchange = e => { r.province = e.target.value; };
    $('#regionChips').addEventListener('click', e => {
      const b = e.target.closest('[data-region]'); if (!b) return;
      r.region = b.dataset.region;
      if (r.region && !(REGION_PROVINCES[r.region] || []).includes(r.province)) r.province = '';
      renderRecommend();
    });
    $('#recoBtn').onclick = doRecommend;
  }

  async function doRecommend() {
    const box = $('#recoResult');
    box.innerHTML = `<div class="empty"><div class="big">🤖</div>AI 正在全国 ${state.provinces.length ? '200+' : ''} 个景点中分析与你的匹配度…</div>`;
    const r = state.recommend;
    try {
      const list = await API.post('/api/recommend', {
        interests: Array.from(r.interests),
        durationMinutes: r.durationMinutes,
        budget: r.budget,
        crowd: r.crowd,
        energy: r.energy,
        province: r.province || undefined,
        region: r.region || undefined,
      });
      if (!list.length) {
        box.innerHTML = `<div class="empty">当前条件（尤其体力限制）下没有可推荐景点，试试放宽条件</div>`;
        return;
      }
      box.innerHTML = list.map((it, i) => {
        const s = it.spot;
        const hue = it.score >= 85 ? '#0e9f6e' : it.score >= 70 ? '#0891b2' : it.score >= 55 ? '#d97706' : '#6b7280';
        return `
        <div class="reco-item" style="border-left-color:${hue}">
          <div class="reco-img"><img ${imgAttr(s.imageUrl)} /></div>
          <div class="reco-main">
            <div class="reco-head">
              <div><b style="font-size:16px">No.${i + 1} ${esc(s.name)}</b><span class="match-level">${esc(it.matchLevel)}</span><div class="spot-loc">📍 ${esc(s.province)} · ${esc(s.city || '')}</div></div>
              <div class="score-ring" style="background:${hue}">${it.score}</div>
            </div>
            <div class="spot-summary">${esc(s.summary)}</div>
            <ul class="reco-reasons">${it.reasons.map(x => `<li>✅ ${esc(x)}</li>`).join('')}</ul>
            <div class="tag-row" style="margin-bottom:8px">${s.tags.map(t => `<span class="tag">${esc(t)}</span>`).join('')}</div>
            <button class="btn" data-spot="${s.id}">查看详情</button>
            <button class="btn btn-ghost" data-listen="${s.id}">🎧 听讲解</button>
          </div>
        </div>`;
      }).join('') + `<div style="text-align:center;margin-top:8px">
        <button class="btn btn-primary" id="recoToTour">🗺 用推荐结果规划在线游览</button></div>`;
      bindCardEvents(box);
      box.querySelectorAll('[data-listen]').forEach(b => b.onclick = async () => {
        const s = await API.get('/api/spots/' + b.dataset.listen);
        speak(`${s.name}。${s.description}。看点：${s.highlights.join('、')}。`);
        toast('正在播放 AI 语音讲解');
      });
      $('#recoToTour').onclick = () => switchTab('tour');
    } catch (e) {
      box.innerHTML = `<div class="empty">推荐失败：${esc(e.message)}</div>`;
    }
  }

  /* ============================================================
   * 在线游览
   * ============================================================ */
  async function renderTour() {
    const app = $('#app');
    if (state.tour.route) { renderTourPlayer(); return; }
    app.innerHTML = `<div class="empty"><div class="big">🗺️</div>路线加载中…</div>`;
    try {
      const routes = await API.get('/api/routes');
      const today = new Date().toISOString().slice(0, 10);
      const plan = state.tour.plan;
      const modeBtn = m => `<button class="chip ${plan.travelMode === m ? 'active' : ''}" data-mode="${m}">${m === 'driving' ? '🚗 驾车' : m === 'walking' ? '🚶 步行' : '🚌 公交'}</button>`;
      app.innerHTML = `
        <div class="tour-planner">
          <h3 style="margin:0 0 12px">🧭 出行规划</h3>
          <div class="planner-row">
            <label>出行日期 <input type="date" id="planDate" value="${plan.travelDate || today}" min="${today}" /></label>
            <label>游览时长(分钟) <input type="number" id="planMinutes" min="60" step="30" value="${plan.travelMinutes}" /></label>
            <div class="mode-group">${modeBtn('driving')}${modeBtn('walking')}${modeBtn('transit')}</div>
          </div>
          <div class="planner-tip">出行方式将影响路线连线的渲染风格（驾车实线 / 步行虚线 / 公交点划线）</div>
        </div>
        <div class="section-title" style="margin-top:4px">选择一条路线，开启图文 + 语音 + AI 在线游览</div>
        <div class="route-grid">
          ${routes.map(r => `
          <div class="route-card" data-route="${r.id}">
            <div class="route-cover"><img ${imgAttr(r.coverUrl)} /><b>${esc(r.name)}</b></div>
            <div class="route-info">
              <p>${esc(r.subtitle)}</p>
              <div class="tag-row">${r.tags.map(t => `<span class="tag">${esc(t)}</span>`).join('')}</div>
              <div class="spot-meta" style="margin-top:8px">
                <span>📍 ${r.totalSpots} 站 · ⏱ ${r.durationMinutes} 分钟 · 强度${diffText(r.difficulty)}</span>
              </div>
              <div class="spot-meta"><span>适合：${r.suitableFor.join('、')}</span><button class="btn btn-primary" style="padding:4px 14px">开始游览 →</button></div>
            </div>
          </div>`).join('')}
        </div>`;
      $('#planDate').onchange = e => { plan.travelDate = e.target.value; };
      $('#planMinutes').onchange = e => { plan.travelMinutes = Number(e.target.value) || 240; };
      $$('.mode-group [data-mode]').forEach(b => b.onclick = () => {
        plan.travelMode = b.dataset.mode;
        $$('.mode-group [data-mode]').forEach(x => x.classList.toggle('active', x === b));
      });
      app.querySelectorAll('[data-route]').forEach(el => el.onclick = () => startTour(Number(el.dataset.route)));
    } catch (e) {
      app.innerHTML = `<div class="empty">路线加载失败：${esc(e.message)}</div>`;
    }
  }

  async function startTour(routeId) {
    try {
      state.tour.travelMode = state.tour.plan.travelMode;
      const data = await API.post('/api/tour/start', { routeId, sessionId: state.tourSession || undefined });
      if (data.sessionId) { state.tourSession = data.sessionId; localStorage.setItem('tourSession', data.sessionId); }
      state.tour.route = data.route;
      state.tour.visited = new Set(data.progress.visitedSpotIds);
      // 从第一个未打卡的景点开始
      const idx = data.route.spots.findIndex(s => !state.tour.visited.has(s.spotId));
      state.tour.index = idx >= 0 ? idx : 0;
      switchTab('tour');
    } catch (e) { toast(e.message); }
  }

  /* 高德瓦片 + OSM 回退工厂 (路线地图与足迹地图共用) */
  function createBaseLayers(map) {
    const gaode = L.tileLayer(
      'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
      { subdomains: ['1','2','3','4'], maxZoom: 18, minZoom: 3 }
    );
    const osm = L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      { subdomains: ['a','b','c'], maxZoom: 18, minZoom: 3 }
    );
    // 探测高德首瓦是否可达, 失败则回退 OSM
    const probe = new Image();
    probe.onerror = () => { if (map.hasLayer(gaode)) map.removeLayer(gaode); osm.addTo(map); };
    probe.src = 'https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=547&y=335&z=10';
    gaode.addTo(map);
    return { gaode, osm };
  }

  /* Leaflet 路线真实地图: 高德瓦片 + 出行方式决定连线样式 + 标记所有景点 */
  function initLeafletForRoute(spots, visitedSet, currentIndex, travelMode) {
    const el = document.getElementById('routeLeafletMap');
    if (!el || !window.L) return;
    if (el._leafletMap) { el._leafletMap.remove(); el._leafletInitialized = false; }
    el.style.height = '360px';
    const map = L.map('routeLeafletMap', { zoomControl: true, attributionControl: false, scrollWheelZoom: false })
      .setView([spots[0].spotLat, spots[0].spotLng], 6);
    createBaseLayers(map);
    // 连线样式按出行方式区分 (if-else, 不用 switch 表达式)
    const latlngs = spots.map(s => [s.spotLat, s.spotLng]);
    let polyOpt;
    if (travelMode === 'walking') {
      polyOpt = { color: '#16a34a', weight: 3, dashArray: '2 10', opacity: 0.85 };
    } else if (travelMode === 'transit') {
      polyOpt = { color: '#9333ea', weight: 2, dashArray: '10 6 2 6', opacity: 0.8 };
    } else {
      polyOpt = { color: '#0ea5e9', weight: 4, opacity: 0.85 };
    }
    L.polyline(latlngs, polyOpt).addTo(map);
    // 每个景点标记
    spots.forEach((s, i) => {
      const visited = visitedSet.has(s.spotId);
      const cur = i === currentIndex;
      const color = cur ? '#ea580c' : (visited ? '#16a34a' : '#64748b');
      const icon = L.divIcon({
        className: 'route-marker',
        html: `<div style="width:${cur?32:24}px;height:${cur?32:24}px;border-radius:50%;background:${color};border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;color:#fff;font-size:${cur?14:11}px;font-weight:700;">${i+1}</div>`,
        iconSize: [cur?32:24, cur?32:24], iconAnchor: [cur?16:12, cur?16:12]
      });
      L.marker([s.spotLat, s.spotLng], { icon }).addTo(map)
        .bindPopup(`<b>${esc(s.spotName)}</b><br/>${esc(s.spotProvince)}${esc(s.spotCity||'')}<br/>第${s.dayNo}天·第${s.stepNo}站${visited?' ✅':''}`)
        .on('click', () => { stopSpeak(); state.tour.index = i; renderTourPlayer(); });
    });
    if (latlngs.length > 1) map.fitBounds(latlngs, { padding: [30, 30] });
    setTimeout(() => map.invalidateSize(), 50);
    el._leafletInitialized = true;
    el._leafletMap = map;
  }

  function renderTourPlayer() {
    const route = state.tour.route;
    const app = $('#app');
    const step = route.spots[state.tour.index];
    const done = state.tour.visited.has(step.spotId);
    const total = route.spots.length;
    const isLast = state.tour.index === total - 1;
    app.innerHTML = `
      <button class="btn" id="exitTour">← 返回路线列表</button>
      <div class="section-title">${esc(route.name)}<span style="font-size:13px;color:var(--ink-3);font-weight:400">${esc(route.subtitle)}</span></div>
      <div class="tour-player">
        <div class="tour-stepper">
          <div class="progress-bar"><i style="width:${Math.round(state.tour.visited.size / total * 100)}%"></i></div>
          <div class="progress-meta"><span>已打卡 ${state.tour.visited.size}/${total} 站 · 途经 ${new Set(route.spots.map(s => s.spotProvince)).size} 个省份</span><span>第 ${step.dayNo} 天 · 第 ${step.stepNo} 站</span></div>
          <div class="step-dots">
            ${route.spots.map((s, i) => `<span class="step-dot ${state.tour.visited.has(s.spotId) ? 'done' : ''} ${i === state.tour.index ? 'current' : ''}" data-step="${i}" title="${esc(s.spotName)}">${i + 1}</span>`).join('')}
          </div>
        </div>

        <div class="route-map-wrap">
          <div id="routeLeafletMap" style="width:100%;height:360px;border-radius:14px;overflow:hidden;"></div>
        </div>

        <div class="tour-stage">
          <div class="tour-photo">
            <img ${imgAttr(step.imageUrl)} alt="${esc(step.spotName)}" />
            <span class="tour-day">第 ${step.dayNo} 天 · ${esc(step.spotProvince)}${esc(step.spotCity || '')}</span>
          </div>
          <div class="tour-panel">
            <h2>${esc(step.spotName)} ${done ? '✅' : ''}</h2>
            <div class="spot-summary">${esc(step.spotSummary)}</div>
            <div class="tour-tags tag-row">
              ${step.spotTags.map(t => `<span class="tag">${esc(t)}</span>`).join('')}
              <span class="tag">⏱ ${step.stayMinutes} 分钟</span>
              <span class="tag">⭐ ${step.spotRating.toFixed(1)}</span>
            </div>
            <div class="narration">${esc(step.narration)}</div>
            <div class="voice-row">
              <button class="btn btn-primary" id="voiceBtn">🔊 播放讲解</button>
              <button class="btn btn-accent" id="aiNarrationBtn">🤖 AI 深度讲解</button>
              <button class="btn btn-accent" id="vrStepBtn" title="沉浸式环景游览本站">🥽 VR 环景</button>
              <button class="btn" id="voiceStopBtn">⏹ 停止</button>
              <button class="btn ${state.tour.auto ? 'btn-accent' : ''}" id="autoBtn">${state.tour.auto ? '⏸ 暂停自动游览' : '▶ 自动连播游览'}</button>
              <button class="btn btn-ghost" data-spot="${step.spotId}">景点详情</button>
            </div>
            <div class="spot-meta"><span>🕘 ${esc(step.spotOpenTime)}</span><span>强度：${diffText(step.spotDifficulty)}</span>${money(step.spotPrice)}</div>
            <div class="tour-actions">
              <button class="btn" id="prevStep" ${state.tour.index === 0 ? 'disabled' : ''}>← 上一景</button>
              <button class="btn btn-accent" id="visitBtn">${isLast ? '打卡并完成游览 🏁' : '打卡本站，继续 →'}</button>
            </div>
          </div>
        </div>
      </div>`;
    $('#exitTour').onclick = () => { stopSpeak(); state.tour.route = null; state.tour.auto = false; renderTour(); };
    $('#prevStep').onclick = () => { stopSpeak(); state.tour.index--; renderTourPlayer(); };
    $('#voiceBtn').onclick = () => { speak(`${step.spotName}。${step.narration}`); toast('讲解播放中…'); };
    $('#aiNarrationBtn').onclick = () => openRouteAiNarration(step);
    /* VR 接入虚拟游览: 当前站点一键进入环景, 关闭后回到游览进度(打卡已自动落库) */
    $('#vrStepBtn').onclick = () => {
      stopSpeak();
      openVr({ id: step.spotId, name: step.spotName, imageUrl: step.imageUrl, gallery: [step.imageUrl] },
        'pano', { tourTag: `${route.name} · 第 ${step.dayNo} 天 · 第 ${step.stepNo} 站` });
    };
    $('#voiceStopBtn').onclick = () => { stopSpeak(); toast('已停止讲解'); };
    $('#autoBtn').onclick = () => {
      state.tour.auto = !state.tour.auto;
      if (state.tour.auto) {
        toast('自动游览已开启，将逐站讲解并打卡');
        speak(`${step.spotName}。${step.narration}`, autoNext);
      } else {
        stopSpeak();
      }
      renderTourPlayer();
    };
    $('#visitBtn').onclick = visitStep;
    app.querySelectorAll('[data-step]').forEach(d => d.onclick = () => { stopSpeak(); state.tour.index = Number(d.dataset.step); renderTourPlayer(); });
    // 初始化 Leaflet 路线真实地图
    initLeafletForRoute(route.spots, state.tour.visited, state.tour.index, state.tour.travelMode);
    bindCardEvents(app);
  }

  function autoNext() {
    if (!state.tour.auto || !state.tour.route) return;
    const route = state.tour.route;
    if (state.tour.index < route.spots.length - 1) {
      visitStep(true);
    } else {
      state.tour.auto = false;
    }
  }

  async function visitStep(auto) {
    const route = state.tour.route;
    const step = route.spots[state.tour.index];
    try {
      const progress = await API.post('/api/tour/visit', {
        sessionId: state.tourSession, routeId: route.id, spotId: step.spotId,
      });
      state.tour.visited = new Set(progress.visitedSpotIds);
      playCheckinBurst();
      if (progress.finished) {
        stopSpeak();
        state.tour.auto = false;
        const pCount = new Set(route.spots.map(s => s.spotProvince)).size;
        $('#app').innerHTML = `
          <div class="tour-player finish-banner">
            <div class="big">🏆</div>
            <h2>恭喜完成《${esc(route.name)}》！</h2>
            <p style="color:var(--ink-2);margin:10px 0 6px">你已打卡全部 ${progress.total} 个景点，横跨 ${pCount} 个省份，获得「神州行者」成就徽章 🎖️</p>
            <p style="color:var(--ink-3);margin:0 0 20px;font-size:13px">去「我的足迹」查看点亮的中国省份地图吧</p>
            <button class="btn btn-primary" id="backRoutes">选择其他路线</button>
            <button class="btn" data-go="me">查看我的足迹地图</button>
          </div>`;
        $('#backRoutes').onclick = () => { state.tour.route = null; renderTour(); };
        $('#app').querySelector('[data-go]').onclick = () => switchTab('me');
        return;
      }
      if (state.tour.index < route.spots.length - 1) {
        state.tour.index++;
        renderTourPlayer();
        if (auto && state.tour.auto) {
          const next = route.spots[state.tour.index];
          speak(`${next.spotName}。${next.narration}`, autoNext);
        }
      }
      else renderTourPlayer();
    } catch (e) { toast(e.message); }
  }

  /* AI 深度讲解弹窗 (调用通义千问/DeepSeek/豆包AI大模型) */
  async function openRouteAiNarration(step) {
    const modal = $('#spotModal'), body = $('#spotModalBody');
    modal.hidden = false;
    body.innerHTML = `
      <div class="detail-body">
        <h2>${esc(step.spotName)} · AI 深度讲解</h2>
        <div class="ai-explain-tip">调用通义千问/DeepSeek/豆包AI大模型结合景点数据库生成结构化讲解</div>
        <div class="ai-explain-body" id="aiExplainBody"><div class="empty">🤖 AI 生成中…</div></div>
        <div class="voice-row" id="explainVoiceRow" hidden>
          <button class="btn btn-primary" id="modalListen">🔊 朗读讲解</button>
          <button class="btn" id="modalStop">⏹ 停止</button>
        </div>
      </div>`;
    $('#spotModalClose').onclick = () => { modal.hidden = true; stopSpeak(); };
    let explainText = '';
    const badge = engine => engine === 'doubao' ? '<span class="reply-badge llm">⚡ 豆包AI</span>'
      : engine === 'qwen' ? '<span class="reply-badge llm">⚡ 通义千问</span>'
      : engine === 'deepseek' ? '<span class="reply-badge llm">⚡ DeepSeek</span>'
      : engine === 'llm' ? '<span class="reply-badge llm">⚡ AI 大模型</span>'
      : '<span class="reply-badge local">📚 本地知识库</span>';
    try {
      const r = await API.get('/api/ai/explain?spotId=' + step.spotId);
      explainText = r.answer || '';
      $('#aiExplainBody').innerHTML = `<div class="explain-content">${badge(r.engine)}${esc(r.answer).replace(/\n/g, '<br/>')}</div>`;
      $('#explainVoiceRow').hidden = false;
    } catch (e) {
      $('#aiExplainBody').innerHTML = `<div class="empty">讲解生成失败：${esc(e.message)}</div>`;
    }
    $('#modalListen').onclick = () => { if (explainText) speak(explainText); };
    $('#modalStop').onclick = () => stopSpeak();
  }

  /* 打卡动画: ✅ + pulse 圆圈 + confetti 碎片 */
  function playCheckinBurst() {
    const burst = document.createElement('div');
    burst.className = 'checkin-burst';
    burst.innerHTML = '<div class="checkin-pulse"></div><div class="checkin-emoji">✅</div>';
    for (let i = 0; i < 14; i++) {
      const s = document.createElement('span');
      s.className = 'confetti';
      s.style.setProperty('--dx', (Math.random() * 240 - 120).toFixed(0) + 'px');
      s.style.setProperty('--dy', (Math.random() * -180 - 40).toFixed(0) + 'px');
      s.style.setProperty('--rot', (Math.random() * 720 - 360).toFixed(0) + 'deg');
      s.style.background = ['#f97316', '#0ea5e9', '#16a34a', '#eab308', '#9333ea'][i % 5];
      burst.appendChild(s);
    }
    document.body.appendChild(burst);
    setTimeout(() => burst.remove(), 1500);
  }

  /* ============================================================
   * AI 助教
   * ============================================================ */
  function renderAi() {
    // 拉取可用供应商(只拉一次)
    if (!state.aiProviders.length) {
      API.get('/api/ai/providers').then(list => { state.aiProviders = list || []; paintModelSwitch(); }).catch(() => {});
    }
    $('#app').innerHTML = `
    <div class="ai-wrap">
      <div class="chat-box">
        <div class="chat-head">
          <span class="live-dot"></span>
          <b>云游中国 · 星际 AI 助教 小云</b>
          <div class="model-switch" id="modelSwitch"></div>
        </div>
        <div class="chat-msgs" id="chatMsgs"></div>
        <div class="suggestions" id="chatSuggest"></div>
        <div class="chat-input">
          <input class="input" id="chatInput" placeholder="问我任何问题，如：去四川玩什么？故宫门票多少钱？" />
          <button class="btn btn-primary" id="chatSend">发送</button>
        </div>
      </div>
      <div class="ai-side">
        <div class="xy-stage" id="xyStage">
          <div class="xiaoyun" id="xiaoyun">
            <div class="xy-bubble" id="xyBubble">点我打个招呼 👋</div>
            <div class="xy-shadow"></div>
            <div class="xy-body">
              <div class="xy-hat"></div>
              <div class="xy-head">
                <div class="xy-eye left"></div>
                <div class="xy-eye right"></div>
                <div class="xy-cheek left"></div>
                <div class="xy-cheek right"></div>
                <div class="xy-mouth"></div>
              </div>
              <div class="xy-arm left"></div>
              <div class="xy-arm right"></div>
            </div>
          </div>
        </div>
        <div class="fun-row">
          <button class="fun-btn" data-fun="joke">😄 讲个笑话</button>
          <button class="fun-btn" data-fun="poem">📜 背首诗</button>
          <button class="fun-btn" data-fun="guess">🎯 猜景点</button>
          <button class="fun-btn" data-fun="quiz" style="background:var(--accent);color:#fff;border-color:var(--accent)">🎮 极速挑战</button>
        </div>
        <h4 style="margin-top:14px">💡 你可以问我</h4>
        <ul>
          <li data-q="推荐一条丝绸之路路线">推荐一条丝绸之路路线</li>
          <li data-q="介绍一下故宫博物院">介绍一下故宫博物院</li>
          <li data-q="九寨沟门票多少钱">九寨沟门票多少钱</li>
          <li data-q="带孩子去北京玩什么">带孩子去北京玩什么</li>
          <li data-q="三亚有什么好玩的">三亚有什么好玩的</li>
          <li data-q="西安有什么好吃的">西安有什么好吃的</li>
          <li data-q="去西藏旅游要注意什么">去西藏旅游要注意什么</li>
          <li data-q="免费又好玩的景点推荐">免费又好玩的景点推荐</li>
        </ul>
      </div>
    </div>`;
    const msgs = $('#chatMsgs');
    if (!state.chat.length) {
      state.chat.push({ role: 'bot', reply: {
        answer: '你好呀！我是「云游中国」AI 助教小云 🤖\n我由通义千问 + DeepSeek + 豆包AI 多引擎驱动，已接入全国 200+ 景点知识库，可以为你讲解景点、查询门票开放时间、规划跨省长线、推荐美食住宿。试试下面的问题吧～',
        suggestions: ['推荐一条丝绸之路路线', '带孩子去北京玩什么', '九寨沟门票多少钱'],
        type: 'text',
        engine: 'llm',
      } });
    }
    paintChat();
    paintModelSwitch();
    xyBind();
    $('#chatSend').onclick = sendChat;
    $('#chatInput').addEventListener('keydown', e => { if (e.key === 'Enter') sendChat(); });
    $('#chatMsgs').addEventListener('click', e => {
      const link = e.target.closest('[data-chat-action]');
      if (!link) return;
      const type = link.dataset.type, id = Number(link.dataset.id);
      if (type === 'spot') openSpot(id);
      if (type === 'route') startTour(id);
    });
    document.querySelectorAll('.fun-btn').forEach(b => b.onclick = () => funPlay(b.dataset.fun));
    // 来自星际探索的联动提问: 自动填入并发送
    if (pendingChatPrompt) {
      const q = pendingChatPrompt; pendingChatPrompt = null;
      $('#chatInput').value = q;
      sendChat();
    }
  }

  /* ---- 小云拟人形象控制 ---- */
  let xyTimer = null;
  function xySet(cls, ms) {
    const el = $('#xiaoyun');
    if (!el) return;
    el.classList.remove('thinking', 'speaking', 'happy', 'wave');
    if (cls) el.classList.add(cls);
    if (xyTimer) { clearTimeout(xyTimer); xyTimer = null; }
    if (ms > 0) xyTimer = setTimeout(() => { el.classList.remove(cls); xyTimer = null; }, ms);
  }
  function xySay(text, ms = 1600) {
    const b = $('#xyBubble');
    if (!b) return;
    b.textContent = text;
    b.classList.add('show');
    clearTimeout(b._t);
    b._t = setTimeout(() => b.classList.remove('show'), ms);
  }
  function xyBind() {
    const el = $('#xiaoyun');
    if (!el || el._bound) return;
    el._bound = true;
    const greetings = ['你好呀～', '点我干嘛呀？', '我是小云！', '有问题问我吧～', '想去哪里玩？'];
    el.addEventListener('click', () => {
      xySet('wave', 2400);
      xySay(greetings[Math.floor(Math.random() * greetings.length)], 1500);
    });
  }

  /* ---- 模型切换器 ---- */
  function paintModelSwitch() {
    const box = $('#modelSwitch');
    if (!box) return;
    const providers = state.aiProviders.length ? state.aiProviders : ['deepseek', 'qwen', 'doubao'];
    const label = { deepseek: '⚡ DeepSeek', qwen: '⚡ 通义千问', doubao: '⚡ 豆包' };
    const chips = [`<span class="lbl">引擎:</span>`,
      `<button class="model-chip auto ${state.aiProvider === 'auto' ? 'on' : ''}" data-p="auto">🎲 自动切换</button>`];
    providers.forEach(p => chips.push(
      `<button class="model-chip ${state.aiProvider === p ? 'on' : ''}" data-p="${p}">${label[p] || p}</button>`));
    box.innerHTML = chips.join('');
    box.querySelectorAll('.model-chip').forEach(c => c.onclick = () => {
      state.aiProvider = c.dataset.p;
      localStorage.setItem('aiProvider', state.aiProvider);
      paintModelSwitch();
      xySay(state.aiProvider === 'auto' ? '已开启自动切换模式' : `切换到 ${label[state.aiProvider] || state.aiProvider}`, 1400);
    });
  }

  /* ---- 趣味玩法 ---- */
  function funPlay(kind) {
    const map = {
      joke: '讲一个和旅行有关的笑话',
      poem: '背一首和中国名山大川有关的古诗，并解释它写的是哪里',
      guess: '说一个景点的三个线索，让我猜是哪里（你出题）',
    };
    if (kind === 'quiz') { startQuiz(); return; }
    if (map[kind]) { $('#chatInput').value = map[kind]; sendChat(); }
  }

  /* ============================================================
   * 🎮 极速景点大挑战: 5题限时答题, 小云陪玩
   * ============================================================ */
  let quiz = { running: false, idx: 0, score: 0, combo: 0, best: Number(localStorage.getItem('quizBest') || 0), total: 5, timer: null, left: 0, pool: [], q: null, locked: false };

  async function startQuiz() {
    if (quiz.running) return;
    xySet('wave', 1500);
    xySay('极速挑战开始！', 1200);
    // 拉一批景点作为题库
    try {
      const list = await API.get('/api/spots?size=40&sort=default&page=1');
      quiz.pool = (list && list.list ? list.list : Array.isArray(list) ? list : []).filter(s => s.name && s.province);
    } catch (e) { quiz.pool = []; }
    if (quiz.pool.length < 8) {
      toast('题库加载失败, 稍后再试');
      return;
    }
    quiz.running = true; quiz.idx = 0; quiz.score = 0; quiz.combo = 0;
    openQuizModal();
    nextQuestion();
  }

  function openQuizModal() {
    let m = $('#quizModal');
    if (!m) {
      m = document.createElement('div');
      m.id = 'quizModal';
      m.className = 'quiz-mask';
      m.innerHTML = `<div class="quiz-card" id="quizCard">
        <div class="quiz-head">
          <span class="quiz-title">🎮 极速景点大挑战</span>
          <span class="quiz-score">得分 <b id="quizScore">0</b></span>
          <span class="quiz-best">最佳 <b id="quizBest">${quiz.best}</b></span>
          <button class="quiz-close" id="quizClose">✕</button>
        </div>
        <div class="quiz-body" id="quizBody"></div>
      </div>`;
      document.body.appendChild(m);
    }
    $('#quizBest').textContent = quiz.best;
    m.classList.add('show');
    $('#quizClose').onclick = endQuiz;
  }

  function endQuiz() {
    if (quiz.timer) { clearInterval(quiz.timer); quiz.timer = null; }
    quiz.running = false;
    const m = $('#quizModal');
    if (m) m.classList.remove('show');
  }

  function nextQuestion() {
    if (quiz.idx >= quiz.total) { showQuizResult(); return; }
    quiz.idx++;
    quiz.locked = false;
    const q = buildQuestion(quiz.pool);
    quiz.q = q;
    const body = $('#quizBody');
    body.innerHTML = `
      <div class="quiz-progress">第 ${quiz.idx} / ${quiz.total} 题 ${quiz.combo > 1 ? `<span class="quiz-combo">🔥 ${quiz.combo} 连击</span>` : ''}</div>
      <div class="quiz-timer" id="quizTimer">⏱ 10</div>
      <div class="quiz-q">${esc(q.text)}</div>
      <div class="quiz-options">
        ${q.options.map((o, i) => `<button class="quiz-opt" data-i="${i}">${String.fromCharCode(65 + i)}. ${esc(o)}</button>`).join('')}
      </div>`;
    body.querySelectorAll('.quiz-opt').forEach(b => b.onclick = () => answerQuiz(Number(b.dataset.i)));
    quiz.left = 10;
    paintTimer();
    quiz.timer = setInterval(() => {
      quiz.left--;
      paintTimer();
      if (quiz.left <= 0) { clearInterval(quiz.timer); quiz.timer = null; answerQuiz(-1); }
    }, 1000);
  }

  function paintTimer() {
    const t = $('#quizTimer');
    if (!t) return;
    t.textContent = '⏱ ' + quiz.left;
    t.style.color = quiz.left <= 3 ? '#ef4444' : '';
  }

  function answerQuiz(i) {
    if (quiz.locked) return;
    quiz.locked = true;
    if (quiz.timer) { clearInterval(quiz.timer); quiz.timer = null; }
    const q = quiz.q;
    const opts = document.querySelectorAll('.quiz-opt');
    opts.forEach((b, idx) => {
      b.disabled = true;
      if (idx === q.answer) b.classList.add('right');
      if (idx === i && i !== q.answer) b.classList.add('wrong');
    });
    if (i === q.answer) {
      quiz.combo++;
      const mult = Math.min(quiz.combo, 5);
      const gain = (10 + quiz.left) * mult;
      quiz.score += gain;
      $('#quizScore').textContent = quiz.score;
      xySet('happy', 900);
      xySay((quiz.combo >= 3 ? `${quiz.combo}连击！` : '答对啦！') + '+' + gain, 1100);
    } else {
      quiz.combo = 0;
      if (i === -1) { xySet('thinking'); xySay('超时啦～', 1100); }
      else { xySet('sad', 900); xySay('差一点～', 1100); }
    }
    $('#quizBody').insertAdjacentHTML('beforeend',
      `<div class="quiz-answer">正确答案：<b>${String.fromCharCode(65 + q.answer)}. ${esc(q.options[q.answer])}</b>${q.hint ? ' — ' + esc(q.hint) : ''}</div>`);
    setTimeout(nextQuestion, 1500);
  }

  function showQuizResult() {
    const s = quiz.score, max = quiz.total * 20 * 5;
    const isNewBest = s > quiz.best;
    if (isNewBest) { quiz.best = s; localStorage.setItem('quizBest', s); }
    let title, emoji;
    if (s >= max * 0.85) { title = '云游大师'; emoji = '🏆'; }
    else if (s >= max * 0.6) { title = '资深驴友'; emoji = '🎒'; }
    else if (s >= max * 0.35) { title = '旅行达人'; emoji = '🧭'; }
    else { title = '旅行小白'; emoji = '🐣'; }
    $('#quizBody').innerHTML = `
      <div class="quiz-result">
        <div class="quiz-emoji">${emoji}</div>
        <div class="quiz-title-big">${title}${isNewBest ? ' 🎉 新纪录！' : ''}</div>
        <div class="quiz-final-score">${s} 分</div>
        <div class="quiz-tip">历史最佳：${quiz.best} 分 · ${s >= max * 0.6 ? '小云为你点赞！继续探索更多景点吧～' : '多看看景点详情，下次一定更棒！'}</div>
        <button class="btn btn-primary" id="quizAgain">🔄 再来一局</button>
      </div>`;
    xySet('happy', 1200);
    xySay(title + '！', 1500);
    $('#quizAgain').onclick = () => { quiz.idx = 0; quiz.score = 0; quiz.combo = 0; $('#quizScore').textContent = 0; $('#quizBest').textContent = quiz.best; nextQuestion(); };
  }

  // 从景点池随机组题
  function buildQuestion(pool) {
    const spot = pool[Math.floor(Math.random() * pool.length)];
    const type = Math.floor(Math.random() * 3);
    if (type === 0) {
      // 位于哪个省
      const provinces = [...new Set(pool.map(s => s.province).filter(Boolean))];
      const wrongs = shuffle(provinces.filter(p => p !== spot.province)).slice(0, 3);
      const options = shuffle([spot.province, ...wrongs]);
      return { text: `「${spot.name}」位于我国哪个省级行政区？`, options, answer: options.indexOf(spot.province), hint: spot.city };
    } else if (type === 1) {
      // 属于哪类景观
      const cats = [...new Set(pool.map(s => s.categoryName).filter(Boolean))];
      const wrongs = shuffle(cats.filter(c => c !== spot.categoryName)).slice(0, 3);
      const options = shuffle([spot.categoryName, ...wrongs]);
      return { text: `「${spot.name}」属于哪一类景观？`, options, answer: options.indexOf(spot.categoryName), hint: spot.tags && spot.tags[0] ? `标签: ${spot.tags[0]}` : '' };
    } else {
      // 门票价格档位
      const price = spot.price == null ? 0 : Number(spot.price);
      const tier = price === 0 ? '免费开放' : price < 60 ? '经济实惠(60元以下)' : price < 150 ? '中等价位(60-150元)' : '较高价位(150元以上)';
      const tiers = ['免费开放', '经济实惠(60元以下)', '中等价位(60-150元)', '较高价位(150元以上)'];
      const options = shuffle([...tiers]);
      return { text: `「${spot.name}」的门票价格大约属于哪个档位？`, options, answer: options.indexOf(tier), hint: `参考价 ${price} 元` };
    }
  }
  function shuffle(arr) { const a = [...arr]; for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; }

  function paintChat() {
    const box = $('#chatMsgs');
    box.innerHTML = state.chat.map(m => {
      if (m.role === 'user') return `<div class="msg user">${esc(m.text)}</div>`;
      const r = m.reply;
      let action = '';
      if (r.type === 'spot' && r.spotId) action = `<br/><span class="msg-link" data-chat-action="1" data-type="spot" data-id="${r.spotId}">查看「景点详情」→</span>`;
      if (r.type === 'route' && r.routeId) action = `<br/><span class="msg-link" data-chat-action="1" data-type="route" data-id="${r.routeId}">开始「虚拟游览」→</span>`;
      const badge = r.engine === 'doubao'
        ? '<span class="reply-badge llm">⚡ 豆包AI</span>'
        : r.engine === 'qwen'
        ? '<span class="reply-badge llm">⚡ 通义千问</span>'
        : r.engine === 'deepseek'
        ? '<span class="reply-badge llm">⚡ DeepSeek</span>'
        : r.engine === 'llm'
        ? '<span class="reply-badge llm">⚡ AI 大模型</span>'
        : '<span class="reply-badge local">📚 本地知识库</span>';
      return `<div class="msg bot">${badge}${esc(r.answer).replace(/\n/g, '<br/>')}${action}</div>`;
    }).join('');
    box.scrollTop = box.scrollHeight;
    const last = state.chat[state.chat.length - 1];
    const sugg = $('#chatSuggest');
    sugg.innerHTML = (last && last.role === 'bot' ? last.reply.suggestions : ['推荐一条经典路线', '全国热门景点', '有什么好吃的'])
      .map(s => `<button class="suggest-chip" data-q="${esc(s)}">${esc(s)}</button>`).join('');
    sugg.querySelectorAll('[data-q]').forEach(b => b.onclick = () => { $('#chatInput').value = b.dataset.q; sendChat(); });
  }

  async function sendChat() {
    const input = $('#chatInput');
    const text = input.value.trim();
    if (!text) return;
    state.chat.push({ role: 'user', text });
    input.value = '';
    paintChat();
    const box = $('#chatMsgs');
    box.insertAdjacentHTML('beforeend', `<div class="msg bot" id="typing">AI 正在思考…</div>`);
    box.scrollTop = box.scrollHeight;
    xySet('thinking');
    xySay('让我想想…', 4000);
    try {
      const provider = state.aiProvider === 'auto' ? null : state.aiProvider;
      const reply = await API.post('/api/ai/chat', { message: text, sessionId: state.tourSession || undefined, provider });
      $('#typing')?.remove();
      state.chat.push({ role: 'bot', reply });
      paintChat();
      xySet('speaking', 1500);
      xySay('回答完毕！', 1200);
      setTimeout(() => xySet('happy', 800), 1500);
    } catch (e) {
      $('#typing')?.remove();
      state.chat.push({ role: 'bot', reply: { answer: '抱歉，我这边出了点小问题：' + e.message, suggestions: ['推荐一条经典路线'], type: 'text' } });
      paintChat();
      xySet(null);
    }
  }

  /* ============================================================
   * 我的（登录/注册/收藏/足迹）
   * ============================================================ */
  function renderMe() {
    if (!state.me) {
      $('#app').innerHTML = `
      <div class="me-grid">
        <div class="auth-box">
          <div class="auth-tabs">
            <button id="tabLogin" class="on">登录</button>
            <button id="tabReg">注册</button>
          </div>
          <input class="input" id="authUser" placeholder="用户名（演示账号 demo / demo123）" />
          <input class="input" id="authPwd" type="password" placeholder="密码" />
          <input class="input" id="authNick" placeholder="昵称（仅注册需要，可选）" style="display:none" />
          <button class="btn btn-primary" id="authSubmit" style="width:100%;padding:11px">登录</button>
          <div class="auth-tip">演示账号：demo / demo123，也可直接注册新账号</div>
        </div>
        <div>
          <div class="me-section"><h3>🗺️ 我的中国足迹地图</h3><div id="footprintBox" class="empty">加载中…</div></div>
          <div class="me-section"><h3>🧭 游览足迹明细</h3><div id="historyBox" class="empty">加载中…</div></div>
        </div>
      </div>`;
      let mode = 'login';
      const switchMode = m => {
        mode = m;
        $('#tabLogin').classList.toggle('on', m === 'login');
        $('#tabReg').classList.toggle('on', m === 'register');
        $('#authNick').style.display = m === 'register' ? 'block' : 'none';
        $('#authSubmit').textContent = m === 'login' ? '登录' : '注册并登录';
      };
      $('#tabLogin').onclick = () => switchMode('login');
      $('#tabReg').onclick = () => switchMode('register');
      $('#authSubmit').onclick = async () => {
        const u = $('#authUser').value.trim(), p = $('#authPwd').value, n = $('#authNick').value.trim();
        try {
          const data = mode === 'login'
            ? await API.post('/api/auth/login', { username: u, password: p })
            : await API.post('/api/auth/register', { username: u, password: p, nickname: n });
          state.me = data; renderUserArea(); toast(mode === 'login' ? '登录成功' : '注册成功'); renderMe();
        } catch (e) { toast(e.message); }
      };
      loadFootprint();
      loadHistory();
      return;
    }
    $('#app').innerHTML = `
    <div class="me-grid">
      <div>
        <div class="profile-card" style="margin-bottom:18px">
          <div style="display:flex;align-items:center;gap:12px">
            <span class="avatar">${esc((state.me.nickname || state.me.username).slice(0, 1))}</span>
            <div><b style="font-size:18px">${esc(state.me.nickname || state.me.username)}</b><br/><small>@${esc(state.me.username)}</small></div>
          </div>
        </div>
        <div class="auth-box">
          <h3 style="margin-bottom:12px">⚙️ 个人设置</h3>
          <label class="filter-label">昵称</label>
          <input class="input" id="nickInput" value="${esc(state.me.nickname || '')}" style="width:100%;margin-bottom:12px"/>
          <label class="filter-label">我的偏好标签（逗号分隔）</label>
          <input class="input" id="prefInput" value="${esc(state.me.preferenceTags || '')}" placeholder="如：摄影,温泉,亲子" style="width:100%;margin-bottom:12px"/>
          <button class="btn btn-primary" id="saveProfile" style="width:100%">保存</button>
          <button class="btn" id="logoutBtn" style="width:100%;margin-top:10px">退出登录</button>
        </div>
      </div>
      <div>
        <div class="me-section"><h3>🗺️ 我的中国足迹地图</h3><div id="footprintBox" class="empty">加载中…</div></div>
        <div class="me-section"><h3>❤️ 我的收藏</h3><div id="favBox" class="empty">加载中…</div></div>
        <div class="me-section"><h3>🧭 游览足迹明细</h3><div id="historyBox" class="empty">加载中…</div></div>
      </div>
    </div>`;
    $('#saveProfile').onclick = async () => {
      try {
        state.me = await API.post('/api/auth/profile', { nickname: $('#nickInput').value, preferenceTags: $('#prefInput').value });
        renderUserArea(); toast('已保存');
      } catch (e) { toast(e.message); }
    };
    $('#logoutBtn').onclick = async () => {
      try { await API.post('/api/auth/logout', {}); } catch (e) {}
      state.me = null; renderUserArea(); toast('已退出'); renderMe();
    };
    loadFootprint();
    loadFavorites();
    loadHistory();
  }

  async function loadFootprint() {
    const box = $('#footprintBox');
    if (!box) return;
    try {
      const fp = await API.get('/api/tour/footprint?sessionId=' + encodeURIComponent(state.tourSession || 'none'));
      if (fp.sessionId && !state.tourSession) { state.tourSession = fp.sessionId; localStorage.setItem('tourSession', fp.sessionId); }
      const records = await API.get('/api/tour/records?sessionId=' + encodeURIComponent(state.tourSession || 'none'));
      box.className = '';
      box.innerHTML = `
        <div class="fp-stats">
          <div><b>${fp.provinceCount}</b><span>点亮省份</span></div>
          <div><b>${fp.spotCount}</b><span>打卡景点</span></div>
          <div><b>${31 - fp.provinceCount}</b><span>待探索</span></div>
        </div>
        <div id="footprintLeafletMap" class="fp-leaflet"></div>
        <div class="fp-badges">
          ${(fp.badges && fp.badges.length ? fp.badges : ['尚未获得徽章']).map(b => `<span class="badge ${b === '尚未获得徽章' ? 'locked' : ''}">🏅 ${esc(b)}</span>`).join('')}
        </div>`;
      initFootprintMap(records);
    } catch (e) { box.className = 'empty'; box.innerHTML = esc(e.message); }
  }

  function initFootprintMap(records) {
    const el = document.getElementById('footprintLeafletMap');
    if (!el || !window.L) return;
    if (el._leafletMap) { el._leafletMap.remove(); el._leafletInitialized = false; }
    if (!el.style.height) el.style.height = '380px';
    const map = L.map('footprintLeafletMap', { zoomControl: true, attributionControl: false, scrollWheelZoom: false })
      .setView([35, 105], 4);
    createBaseLayers(map);
    if (!records.length) {
      L.marker([35, 105]).bindPopup('暂无打卡，去「虚拟游览」开启第一站吧').addTo(map);
      setTimeout(() => map.invalidateSize(), 50);
      el._leafletInitialized = true;
      el._leafletMap = map;
      return;
    }
    const latlngs = records.filter(r => r.lat && r.lng).map(r => [r.lat, r.lng]);
    records.forEach(r => {
      const icon = L.divIcon({
        className: 'fp-marker',
        html: `<div class="fp-pin"><span>📍</span><b>${esc(r.spotName)}</b></div>`,
        iconSize: [20, 28], iconAnchor: [10, 28]
      });
      L.marker([r.lat, r.lng], { icon }).addTo(map)
        .bindPopup(`<b>${esc(r.spotName)}</b><br/>${esc(r.province || '')}${esc(r.city || '')}<br/>${new Date(r.visitedAt).toLocaleString('zh-CN')}`);
    });
    if (latlngs.length > 1) {
      L.polyline(latlngs, { color: '#f97316', weight: 2, dashArray: '4 6', opacity: 0.7 }).addTo(map);
      map.fitBounds(latlngs, { padding: [40, 40] });
    }
    setTimeout(() => map.invalidateSize(), 50);
    el._leafletInitialized = true;
    el._leafletMap = map;
  }

  async function loadFavorites() {
    const box = $('#favBox');
    try {
      const list = await API.get('/api/favorites');
      if (!list.length) { box.className = 'empty'; box.innerHTML = '还没有收藏，去景点导览看看吧'; return; }
      box.className = '';
      box.innerHTML = `<div class="grid" style="grid-template-columns:repeat(2,1fr)">${list.map(f => `
        <div class="spot-card" data-spot="${f.spotId}">
          <div class="spot-img" style="aspect-ratio:16/7"><img ${imgAttr(f.imageUrl)} /></div>
          <div class="spot-body">
            <div class="spot-name">${esc(f.spotName)} <span class="spot-rating">⭐${Number(f.rating || 0).toFixed(1)}</span></div>
            <div class="spot-summary">${esc(f.spotSummary)}</div>
          </div>
        </div>`).join('')}</div>`;
      bindCardEvents(box);
    } catch (e) { box.className = 'empty'; box.innerHTML = esc(e.message); }
  }

  async function loadHistory() {
    const box = $('#historyBox');
    if (!box) return;
    try {
      const list = await API.get('/api/tour/records?sessionId=' + encodeURIComponent(state.tourSession || 'none'));
      if (!list.length) { box.className = 'empty'; box.innerHTML = '还没有打卡记录，去「在线游览」开启第一站吧'; return; }
      box.className = '';
      box.innerHTML = list.map(r => `
        <div class="history-step" data-spot="${r.spotId}" style="cursor:pointer">
          <img ${imgAttr(r.imageUrl)} />
          <div style="flex:1"><b>${esc(r.spotName)}</b><br/><small style="color:var(--ink-3)">📍 ${esc(r.province || '')} · ${new Date(r.visitedAt).toLocaleString('zh-CN')}</small></div>
          <span class="tag">已打卡</span>
        </div>`).join('');
      bindCardEvents(box);
    } catch (e) { box.className = 'empty'; box.innerHTML = esc(e.message); }
  }

  /* 详情页经纬度迷你地图(免 key, 纯 SVG 示意 + 经纬度坐标) */
  function miniMapSvg(s) {
    const W = 560, H = 180, pad = 30;
    const px = pad + ((s.lng - 73) / 62) * (W - pad * 2);
    const py = H - pad - ((s.lat - 18) / 36) * (H - pad * 2);
    return `
      <svg viewBox="0 0 ${W} ${H}" class="mini-map-svg" preserveAspectRatio="xMidYMid meet">
        <defs>
          <linearGradient id="miniBg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#f0f9ff"/><stop offset="1" stop-color="#ecfdf5"/>
          </linearGradient>
        </defs>
        <rect width="${W}" height="${H}" rx="12" fill="url(#miniBg)"/>
        <path d="M${pad},${H/2} L${W-pad},${H/2}" stroke="#bae6fd" stroke-dasharray="4 4"/>
        <path d="M${W/2},${pad} L${W/2},${H-pad}" stroke="#bae6fd" stroke-dasharray="4 4"/>
        <circle cx="${px.toFixed(1)}" cy="${py.toFixed(1)}" r="22" fill="#f97316" opacity="0.18"/>
        <circle cx="${px.toFixed(1)}" cy="${py.toFixed(1)}" r="9" fill="#ea580c" stroke="#fff" stroke-width="2.5"/>
        <text x="${px.toFixed(1)}" y="${(py - 16).toFixed(1)}" text-anchor="middle" class="map-label cur">${esc(s.name)}</text>
        <text x="${pad}" y="${H - 8}" class="map-coord">东经 ${s.lng.toFixed(2)}° · 北纬 ${s.lat.toFixed(2)}°</text>
      </svg>`;
  }

  /* 景点详情: 在线地图预览(免 key OSM 嵌入) + 位置示意 SVG 切换 + 一键跳转高德/百度 */
  function mapPanel(s) {
    const amapSrc = `https://uri.amap.com/marker?position=${s.lng.toFixed(6)},${s.lat.toFixed(6)}&name=${encodeURIComponent(s.name)}&coordinate=wgs84&callnative=1`;
    const baiduQ = encodeURIComponent((s.name + ' ' + (s.city || '')).trim());
    const baiduSrc = `https://map.baidu.com/search/?querytype=s&wd=${baiduQ}`;
    const mapId = 'spotLeaflet_' + s.id;
    return `
      <div class="map-panel">
        <div class="map-tabs" id="mapTabs_${s.id}">
          <button class="map-tab active" data-map="leaflet">🌐 交互地图</button>
          <button class="map-tab" data-map="schematic">🗺️ 位置示意</button>
          <div class="map-open">
            <a class="map-open-link" href="${amapSrc}" target="_blank" rel="noopener">高德导航 ↗</a>
            <a class="map-open-link" href="${baiduSrc}" target="_blank" rel="noopener">百度地图 ↗</a>
          </div>
        </div>
        <div class="map-stage">
          <div class="map-leaflet" id="${mapId}" data-pane="leaflet"></div>
          <div class="map-schematic" data-pane="schematic" hidden>${miniMapSvg(s)}</div>
        </div>
        <div class="map-coord-bar">📍 ${esc(s.province)} · ${esc(s.city || '')} · ${esc(s.address)}
          &nbsp;|&nbsp; 东经 ${s.lng.toFixed(4)}° · 北纬 ${s.lat.toFixed(4)}°</div>
      </div>`;
  }

  /* 初始化 Leaflet 地图 (高德瓦片 + 真实定位) */
  function initLeafletForSpot(spotId, lng, lat, name) {
    const mapId = 'spotLeaflet_' + spotId;
    const el = document.getElementById(mapId);
    if (!el || !window.L) return;
    // Leaflet 需要容器有高度
    if (!el.style.height) el.style.height = '340px';
    // 防止重复初始化
    if (el._leafletInitialized) {
      const m = el._leafletMap;
      if (m) { m.setView([lat, lng], 15); m.invalidateSize(); return; }
    }
    const map = L.map(mapId, { zoomControl: true, attributionControl: false })
        .setView([lat, lng], 15);
    // 瓦片源按优先级尝试: OSM 官方 (通用) → 高德 (国内快, 需要 DNS) → 百度 → 天地图 → Stamen
    const tileSources = [
      { url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', subdomains: ['a','b','c'], name: 'OSM' },
      { url: 'https://wprd{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', subdomains: ['0','1','2','3'], name: '高德' },
      { url: 'https://{s}.stamen-tiles-a.a.ssl.fastly.net/terrain/{z}/{x}/{y}.jpg', subdomains: ['a','b','c','d'], name: 'Stamen' },
    ];
    // 先尝试 OSM, 加载失败时 Fallback 到 Stamen 卫星
    let firstUrl = tileSources[0].url, firstSub = tileSources[0].subdomains;
    try {
      // 检查 OSM 是否可达 (同步 img 预加载)
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => { /* 正常使用 */ };
      img.onerror = () => {
        // OSM 不可达, 自动 fallback
        if (map) {
          map.eachLayer(l => { if (l && l.options && l.options.maxZoom !== undefined && !l._isMarker) map.removeLayer(l); });
          L.tileLayer(tileSources[2].url, { subdomains: tileSources[2].subdomains, maxZoom: 18, minZoom: 3 }).addTo(map);
        }
      };
      img.src = firstUrl.replace('{s}', 'a').replace('{z}', '10').replace('{x}', '547').replace('{y}', '335');
    } catch(e) { /* ignore */ }
    L.tileLayer(firstUrl, { subdomains: firstSub, maxZoom: 18, minZoom: 3 }).addTo(map);
    // 标记点 + 弹窗
    const icon = L.divIcon({
      className: 'spot-marker',
      html: '<div style="width:28px;height:28px;border-radius:50%;background:#ff4444;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.3);"></div>',
      iconSize: [28, 28], iconAnchor: [14, 14]
    });
    L.marker([lat, lng], { icon }).addTo(map)
        .bindPopup(`<b>${esc(name)}</b><br/>东经 ${lng.toFixed(4)}° · 北纬 ${lat.toFixed(4)}°`)
        .openPopup();
    // 强制重绘 (DOM 刚插入时尺寸可能为 0)
    setTimeout(() => map.invalidateSize(), 50);
    el._leafletInitialized = true;
    el._leafletMap = map;
  }

  /* 模拟沙箱支付网关弹窗 (支付宝风格, 支持 mock 支付和真实沙箱跳转) */
  function openSandboxPay(order, buyBtn) {
    // 创建遮罩 + 弹窗 (独立于景点详情 modal)
    const mask = document.createElement('div');
    mask.className = 'modal-mask';
    mask.style.zIndex = 200;
    const qrSeed = Math.random().toString(36).slice(2, 10);
    mask.innerHTML = `
      <div class="sandbox-gateway">
        <div class="gateway-header">
          <div class="gateway-brand">
            <span class="gateway-logo">支</span>
            <div>
              <div class="gateway-title">沙箱支付网关</div>
              <div class="gateway-sub">模拟支付宝 · 测试环境</div>
            </div>
          </div>
          <button class="gateway-close" id="gwClose">×</button>
        </div>
        <div class="gateway-body">
          <div class="gw-pay-info">
            <div class="gw-label">订单号</div>
            <div class="gw-order-no">${esc(order.orderNo)}</div>
          </div>
          <div class="gw-pay-info">
            <div class="gw-label">景区</div>
            <div class="gw-spot-name">${esc(order.spotName)}</div>
          </div>
          <div class="gw-pay-info">
            <div class="gw-label">数量</div>
            <div>${order.quantity} 张</div>
          </div>
          <div class="gw-amount-row">
            <div class="gw-label">应付金额</div>
            <div class="gw-amount">¥<span>${Number(order.totalAmount).toFixed(2)}</span></div>
          </div>
          <div class="gw-qr-area">
            <div class="gw-qr-title">扫码支付 / 点击确认</div>
            <div class="gw-qr-box">
              <div class="gw-qr-img" id="gwQr"></div>
              <div class="gw-qr-hint">模拟二维码 · 点击下方按钮即可支付</div>
            </div>
          </div>
          <div class="gw-methods">
            <label class="gw-method active"><input type="radio" name="gwMethod" value="SANDBOX" checked />
              <span>🧪 沙箱模拟支付</span></label>
            <label class="gw-method"><input type="radio" name="gwMethod" value="ALIPAY" />
              <span>📱 支付宝支付</span></label>
            <label class="gw-method"><input type="radio" name="gwMethod" value="WECHAT" />
              <span>💳 微信支付</span></label>
          </div>
          <button class="gw-confirm" id="gwConfirm">确认支付 ¥${Number(order.totalAmount).toFixed(2)}</button>
          <div class="gw-note" id="gwNote">💡 沙箱模拟模式, 不会真实扣费, 支付 100% 成功</div>
          <button class="gw-fail-btn" id="gwMockFail">🧪 模拟支付失败 (演示)</button>
        </div>
      </div>`;
    document.body.appendChild(mask);
    // 确定性伪二维码 (与电子凭证同源算法, 同一订单号每次渲染相同)
    const qrBox = $('#gwQr', mask);
    if (qrBox) {
      qrBox.innerHTML = `<div style="width:140px;height:140px;background:#fff;padding:6px;border-radius:8px;box-shadow:0 0 0 1px #e5e7eb">${qrSvg(order.orderNo)}</div>`;
    }
    $('#gwClose', mask).onclick = () => mask.remove();
    mask.onclick = (e) => { if (e.target === mask) mask.remove(); };
    $$('.gw-method', mask).forEach(el => el.onclick = () => {
      $$('.gw-method', mask).forEach(m => m.classList.toggle('active', m === el));
      const method = $('input[name="gwMethod"]:checked', mask)?.value || 'SANDBOX';
      const note = $('#gwNote', mask);
      const failBtn = $('#gwMockFail', mask);
      if (method === 'ALIPAY') {
        note.textContent = '💡 支付宝通道, 演示用 mock 模式';
        failBtn.style.display = '';
      } else if (method === 'WECHAT') {
        note.textContent = '💡 微信支付通道, 演示用 mock 模式';
        failBtn.style.display = 'none';
      } else {
        note.textContent = '💡 沙箱模拟模式, 不会真实扣费, 支付 100% 成功';
        failBtn.style.display = '';
      }
    });
    // 模拟失败按钮
    $('#gwMockFail', mask)?.addEventListener('click', () => {
      const method = ($('input[name="gwMethod"]:checked', mask)?.value) || 'SANDBOX';
      const failBtn = $('#gwMockFail', mask);
      failBtn.disabled = true; failBtn.textContent = '模拟中…';
      API.post(`/api/orders/${order.orderNo}/pay?method=${method}&mockFail=true`)
        .then(() => { /* 不应成功 */ })
        .catch(e => {
          mask.remove();
          showPayFailure(order, e.message, () => {
            // 重试: mockFail=false 正常支付
            API.post(`/api/orders/${order.orderNo}/pay?method=${method}`)
              .then(paid => { showPaySuccess(paid); if (buyBtn) { buyBtn.textContent = '🎉 已购票'; buyBtn.disabled = true; } })
              .catch(e2 => showPayFailure(order, e2.message, null));
          });
        });
    });
    $('#gwConfirm', mask).onclick = async () => {
      const btn = $('#gwConfirm', mask);
      const method = ($('input[name="gwMethod"]:checked', mask)?.value) || 'SANDBOX';
      // 三种支付方式 (SANDBOX/ALIPAY/WECHAT) 均走 mock 状态机
      // 真实沙箱调用代码见 AlipayPayServiceImpl / WeChatPayServiceImpl
      {
        // 沙箱 mock 支付
        btn.disabled = true; btn.textContent = '支付处理中…';
        try {
          const paid = await API.post(`/api/orders/${order.orderNo}/pay?method=${method}`);
          mask.remove();
          showPaySuccess(paid);
          if (buyBtn) { buyBtn.textContent = '🎉 已购票'; buyBtn.disabled = true; }
        } catch (e) {
          showPayFailure(order, e.message, async () => {
            btn.disabled = true; btn.textContent = '支付处理中…';
            try {
              const paid2 = await API.post(`/api/orders/${order.orderNo}/pay?method=${method}`);
              mask.remove();
              showPaySuccess(paid2);
              if (buyBtn) { buyBtn.textContent = '🎉 已购票'; buyBtn.disabled = true; }
            } catch (e2) {
              showPayFailure(order, e2.message, null);
              btn.disabled = false; btn.textContent = '确认支付';
            }
          });
          btn.disabled = false; btn.textContent = '确认支付';
        }
      }
    };
  }

  /* 支付失败弹窗 (替代 toast, 显示详细原因 + 重试/取消) */
  function showPayFailure(order, reason, onRetry) {
    // 关掉已有失败弹窗
    document.querySelectorAll('.pay-fail-mask').forEach(el => el.remove());
    const mask = document.createElement('div');
    mask.className = 'modal-mask pay-fail-mask';
    mask.style.zIndex = 300;
    const reasonText = reason || '未知错误, 请稍后重试';
    mask.innerHTML = `
      <div class="sandbox-gateway pay-fail">
        <div class="gateway-header" style="background: linear-gradient(135deg,#dc2626,#991b1b)">
          <div class="gateway-brand">
            <span class="gateway-logo" style="background:#fff;color:#dc2626">!</span>
            <div>
              <div class="gateway-title">支付失败</div>
              <div class="gateway-sub">沙箱环境 · 支付未能完成</div>
            </div>
          </div>
          <button class="gateway-close" id="pfClose">×</button>
        </div>
        <div class="gateway-body">
          <div class="pf-icon">❌</div>
          <div class="pf-title">支付未完成</div>
          <div class="pf-reason">${esc(reasonText)}</div>
          <div class="pf-order-info">
            <div class="pf-row"><span>订单号</span><b>${esc(order.orderNo)}</b></div>
            <div class="pf-row"><span>景区</span><b>${esc(order.spotName)}</b></div>
            <div class="pf-row"><span>金额</span><b class="pf-price">¥${Number(order.totalAmount).toFixed(2)}</b></div>
            <div class="pf-row"><span>状态</span><b style="color:#dc2626">待支付 (PENDING)</b></div>
          </div>
          <div class="pf-tip">💡 订单已创建但未支付, 可重试支付或稍后在「我的订单」中处理</div>
          <div class="pf-actions">
            <button class="btn" id="pfCancel">取消支付</button>
            ${onRetry ? `<button class="btn btn-primary" id="pfRetry">🔄 立即重试</button>` : `<button class="btn btn-primary" id="pfClose2">知道了</button>`}
          </div>
        </div>
      </div>`;
    document.body.appendChild(mask);
    $('#pfClose', mask).onclick = () => mask.remove();
    mask.onclick = (e) => { if (e.target === mask) mask.remove(); };
    $('#pfCancel', mask).onclick = () => mask.remove();
    const retryBtn = $('#pfRetry', mask);
    if (retryBtn && onRetry) {
      retryBtn.onclick = () => { mask.remove(); onRetry(); };
    }
    const close2 = $('#pfClose2', mask);
    if (close2) close2.onclick = () => mask.remove();
  }

  /* 购票成功弹窗 (独立弹窗, 不依赖 #ticketPanel) */
  function showPaySuccess(order) {
    // 关掉已有成功弹窗
    document.querySelectorAll('.pay-success-mask').forEach(el => el.remove());
    // 也关掉支付网关弹窗
    document.querySelectorAll('.modal-mask').forEach(el => { if (el.querySelector('.sandbox-gateway:not(.pay-fail)')) el.remove(); });
    const mask = document.createElement('div');
    mask.className = 'modal-mask pay-success-mask';
    mask.style.zIndex = 300;
    mask.innerHTML = `
      <div class="sandbox-gateway pay-success">
        <div class="gateway-header" style="background:linear-gradient(135deg,#16a34a,#15803d)">
          <div class="gateway-brand">
            <span class="gateway-logo" style="background:#fff;color:#16a34a">✓</span>
            <div>
              <div class="gateway-title">支付成功</div>
              <div class="gateway-sub">沙箱环境 · 支付已完成</div>
            </div>
          </div>
          <button class="gateway-close" id="psClose">×</button>
        </div>
        <div class="gateway-body" style="text-align:center">
          <div style="font-size:48px;margin:12px 0">🎉</div>
          <div style="font-size:18px;font-weight:700;color:#16a34a;margin-bottom:12px">购票成功！</div>
          <div class="pf-order-info" style="text-align:left">
            <div class="pf-row"><span>订单号</span><b>${esc(order.orderNo)}</b></div>
            <div class="pf-row"><span>景区</span><b>${esc(order.spotName)}</b></div>
            <div class="pf-row"><span>数量</span><b>${order.quantity} 张</b></div>
            <div class="pf-row"><span>金额</span><b class="pf-price" style="color:#16a34a">¥${Number(order.totalAmount).toFixed(2)}</b></div>
            <div class="pf-row"><span>游玩日期</span><b>${esc(order.visitDate) || '请在开放时间内使用'}</b></div>
            <div class="pf-row"><span>支付方式</span><b>${esc(order.payMethod || '沙箱')}</b></div>
          </div>
          <div class="pf-actions" style="margin-top:16px">
            <button class="btn" id="psClose2">关闭</button>
            <button class="btn btn-primary" id="psGoOrders">查看我的订单</button>
          </div>
        </div>
      </div>`;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    $('#psClose', mask).onclick = close;
    $('#psClose2', mask).onclick = close;
    mask.onclick = (e) => { if (e.target === mask) close(); };
    $('#psGoOrders', mask).onclick = () => { mask.remove(); switchTab('me'); };
    toast('🎫 购票成功');
  }

  /* 景点详情: 购票面板 (沙箱) */
  function ticketBuyPanel(s) {
    const today = new Date();
    const d = today.toISOString().slice(0,10);
    return `
      <div class="ticket-panel" id="ticketPanel">
        <div class="ticket-head">
          <div>
            <div class="ticket-title">🎫 在线购票 <span class="sandbox-badge">沙箱支付</span></div>
            <div class="ticket-price">¥${Number(s.price).toFixed(2)} / 人</div>
          </div>
          <div class="ticket-qty">
            <button class="qty-btn" data-act="minus">−</button>
            <input class="qty-input" id="ticketQty" value="1" min="1" max="20" />
            <button class="qty-btn" data-act="plus">+</button>
          </div>
        </div>
        <div class="ticket-form">
          <input class="input" id="ticketName" placeholder="联系人姓名" value="游客" />
          <input class="input" id="ticketPhone" placeholder="手机号" />
          <input class="input" id="ticketDate" type="date" value="${d}" />
        </div>
        <div class="ticket-foot">
          合计 <b class="ticket-total">¥${Number(s.price).toFixed(2)}</b>
          <button class="btn btn-primary" id="ticketBuyBtn">立即购买</button>
        </div>
        <div class="ticket-note">💡 提示: 当前为沙箱环境, 不会真实扣费, 支付 100% 成功</div>
      </div>`;
  }

  /* 景点详情: AI 深度讲解面板(拉取 /api/ai/explain + 朗读) */
  function aiExplainPanel(s) {
    return `
      <div class="ai-explain-panel" id="aiExplainPanel">
        <div class="ai-explain-head">
          <h3 style="margin:0">🤖 AI 深度讲解</h3>
          <button class="btn btn-primary" id="modalExplain">✨ 生成深度讲解</button>
        </div>
        <div class="ai-explain-tip">点击生成由通义千问/DeepSeek/豆包AI大模型驱动的景点讲解词，可朗读聆听</div>
        <div class="ai-explain-body" id="aiExplainBody" hidden></div>
        <div class="voice-row" id="explainVoiceRow" hidden>
          <button class="btn btn-primary" id="modalListen">🔊 朗读讲解</button>
          <button class="btn" id="modalStop">⏹ 停止</button>
        </div>
      </div>`;
  }

  async function loadNearby(s) {
    const box = $('#nearbyBox');
    if (!box) return;
    try {
      const list = await API.get(`/api/spots/nearby?lng=${s.lng}&lat=${s.lat}&n=6`);
      const others = list.filter(x => x.spot.id !== s.id).slice(0, 5);
      if (!others.length) { box.innerHTML = '<div class="empty" style="padding:10px">周边暂无其他景点</div>'; return; }
      box.innerHTML = others.map(x => `
        <div class="nearby-card" data-spot="${x.spot.id}">
          <img ${imgAttr(x.spot.imageUrl)} alt=""/>
          <div class="nearby-info">
            <b>${esc(x.spot.name)}</b>
            <small>📍 ${esc(x.spot.province)} · 直线约 ${x.distanceKm} km</small>
            <small>⭐ ${x.spot.rating.toFixed(1)} ${x.spot.price ? '· ¥' + x.spot.price : '· 免费'}</small>
          </div>
        </div>`).join('');
      bindCardEvents(box);
    } catch (e) { box.innerHTML = '<div class="empty" style="padding:10px">附近景点加载失败</div>'; }
  }

  /* ============================================================
   * VR 环景舱: 双模式
   *   🌐 360°全景(默认): 实拍宽图切 12 片环绕柱面, 拖拽环视 + 自动旋转
   *   🖼 3D图墙: 四面立方体照片墙
   * ============================================================ */
  let vrEl = null;
  /* VR 配乐(独立于主 BGM): 主背景音乐没开时, 进入 VR 临时注入本地国风配乐(淡入), 退出即停;
     主 BGM 已开启则让音乐随页面继续, 不重复注入 */
  let vrMusicHost = null, vrMusicAudio = null;
  const vrMusicEnsure = () => {
    if (vrMusicHost || vrMusicAudio) return;
    if (document.querySelector('.bgm-embed')) return;   // 主 BGM 正在播, 不双播
    vrMusicAudio = new Audio('static/audio/bgm.mp3');
    vrMusicAudio.loop = true; vrMusicAudio.volume = 0;
    vrMusicAudio.play().catch(() => {});
    const fade = setInterval(() => {
      if (!vrMusicAudio) return clearInterval(fade);
      vrMusicAudio.volume = Math.min(0.32, vrMusicAudio.volume + 0.03);
      if (vrMusicAudio.volume >= 0.32) clearInterval(fade);
    }, 80);
  };
  const vrMusicRelease = () => {
    if (vrMusicHost) { vrMusicHost.remove(); vrMusicHost = null; }
    if (vrMusicAudio) {
      const a = vrMusicAudio; vrMusicAudio = null;
      const fade = setInterval(() => { a.volume = Math.max(0, a.volume - 0.06); if (a.volume <= 0) { clearInterval(fade); a.pause(); } }, 60);
    }
  };
  function openVr(s, mode, ctx) {
    mode = mode || 'pano';
    const switching = !!vrEl;          // 模式切换时复用配乐, 不重启动画
    if (vrEl) vrEl.remove();
    const tourTag = ctx && ctx.tourTag ? ` · 🧭 ${esc(ctx.tourTag)}` : '';
    const size = Math.round(Math.max(window.innerWidth, window.innerHeight) * 0.72);
    vrEl = document.createElement('div');
    vrEl.className = 'vr-overlay';
    vrEl.innerHTML = `
      <div class="vr-scene"></div>
      <div class="vr-particles">${Array.from({ length: 12 }, () => {
        const l = (4 + Math.random() * 92).toFixed(1), sz = (4 + Math.random() * 5).toFixed(1),
              d = (9 + Math.random() * 9).toFixed(1), de = (-Math.random() * 16).toFixed(1);
        return `<i style="left:${l}%;width:${sz}px;height:${sz}px;animation-duration:${d}s;animation-delay:${de}s"></i>`;
      }).join('')}</div>
      <button class="vr-close" type="button">✕ 退出环景</button>
      <div class="vr-hint">🥽 ${esc(s.name)}${tourTag} ·
        <button class="vr-mode-btn" type="button">🔁 切换视图</button>
        <button class="vr-mode-btn" id="vrAiBtn" type="button" title="AI 生成讲解词并朗读">🔊 AI 讲解</button>
        <span id="vrCount"></span> · 拖拽环视视角</div>
      <div class="vr-caption" id="vrCaption" hidden></div>`;

    const scene = vrEl.querySelector('.vr-scene');
    let raf = 0, ry = 0, rx = -6, dragging = false, lx = 0, ly = 0;
    let basePersp = 950, zoom = 1, flingVel = 0;   // FOV 缩放基值 / 缩放系数 / 拖拽惯性

    /* ---- 构建 360° 环绕柱面: 多张实拍图轮铺 12 片, 每片 cover 近原比例显示, 拒绝拉伸糊图 ---- */
    const buildPano = () => {
      const N = 12, w = Math.round(Math.max(window.innerHeight * 0.92, 720));  // 大画幅: 占满屏高 92%, cover 不超源图分辨率
      const h = Math.round(w * 0.6);
      const R = Math.round(w / (2 * Math.tan(Math.PI / N)));   // 内接圆柱半径
      const imgs = [s.gallery[0] || s.imageUrl, s.gallery[1] || s.imageUrl,
                    s.gallery[2] || s.imageUrl, s.gallery[3] || s.gallery[0] || s.imageUrl]
                   .filter(Boolean);
      const list = imgs.length ? imgs : [''];
      const D = Math.round(R * 2.3), dm = -D / 2, halfH = Math.round(h * 0.55);
      scene.innerHTML = `<div class="vr-ring">${Array.from({ length: N }, (_, i) =>
        `<div class="vr-seg" style="width:${w}px;height:${h}px;
          margin:${-h / 2}px 0 0 ${-w / 2}px;
          background-image:url('${list[i % list.length]}');
          transform:rotateY(${(i * 360 / N).toFixed(1)}deg) translateZ(${-R}px)"></div>`).join('')}
        <div class="vr-sky" style="width:${D}px;height:${D}px;margin:${dm}px 0 0 ${dm}px;transform:rotateX(90deg) translateZ(${halfH}px)"></div>
        <div class="vr-floor" style="width:${D}px;height:${D}px;margin:${dm}px 0 0 ${dm}px;transform:rotateX(90deg) translateZ(${-halfH}px)"></div></div>`;
      basePersp = Math.round(R * 1.2);
      scene.style.perspective = basePersp + 'px';
      rx = -4;
    };

    /* ---- 构建 3D 图墙立方体(原版) ---- */
    const buildCube = () => {
      const faces = [s.gallery[0] || s.imageUrl, s.gallery[1] || s.imageUrl,
                     s.gallery[2] || s.imageUrl, s.gallery[3] || s.gallery[0] || s.imageUrl];
      scene.style.perspective = '950px';
      basePersp = 950; zoom = 1;
      scene.innerHTML = `<div class="vr-cube">
        ${faces.map(u => `<div class="vr-face" style="background-image:url('${u}')"></div>`).join('')}
        <div class="vr-ground"></div><div class="vr-ceiling"></div></div>`;
      const cube = scene.querySelector('.vr-cube'), half = size / 2;
      scene.querySelectorAll('.vr-face').forEach((f, i) => {
        const ang = ['rotateY(0deg)', 'rotateY(90deg)', 'rotateY(180deg)', 'rotateY(270deg)'][i];
        f.style.cssText += `width:${size}px;height:${size}px;left:50%;top:50%;margin:${-half}px 0 0 ${-half}px;transform:${ang} translateZ(${half}px);`;
      });
      const flat = (cls, up) => {
        const el = scene.querySelector(cls), big = Math.round(size * 2.2), bm = -big / 2;
        el.style.cssText += `width:${big}px;height:${big}px;left:50%;top:50%;margin:${bm}px 0 0 ${bm}px;transform:rotateX(90deg) translateZ(${up ? half : -half}px);`;
      };
      flat('.vr-ceiling', true);
      flat('.vr-ground', false);
      rx = -8;
    };

    /* ---- 真·360° 立方体全景: 实拍六面贴图(_f/_r/_b/_l/_u/_d), 相机置于立方体中心 ---- */
    let hfov = 95;                                   // 视场角(度), 滚轮缩放即改变 FOV
    let userSwitched = false, panoFaces = null, panoTitle = '';
    const applyFov = () => {
      basePersp = Math.round((window.innerWidth / 2) / Math.tan((hfov / 2) * Math.PI / 180));
      scene.style.perspective = basePersp + 'px';
    };
    const buildSphere = () => {
      const F = Math.round(Math.max(window.innerWidth, window.innerHeight) * 1.06);
      const R = Math.round(F / 2);
      const face = (u, t) => `<div class="vr-sface" style="width:${F}px;height:${F}px;margin:${-F / 2}px 0 0 ${-F / 2}px;background-image:url('${u}');transform:${t} translateZ(${-R}px)"></div>`;
      scene.innerHTML = `<div class="vr-sphere">`
        + face(panoFaces.f, 'rotateY(0deg)') + face(panoFaces.r, 'rotateY(-90deg)')
        + face(panoFaces.b, 'rotateY(180deg)') + face(panoFaces.l, 'rotateY(90deg)')
        + face(panoFaces.u, 'rotateX(-90deg)') + face(panoFaces.d, 'rotateX(90deg)')
        + `</div>`;
      applyFov();
      zoom = 1; rx = -4;
      const hint = vrEl.querySelector('.vr-hint');
      if (panoTitle && hint && !vrEl.querySelector('#vrSrc')) {
        const sp = document.createElement('span');
        sp.id = 'vrSrc';
        sp.textContent = ` · 🌐 ${panoTitle} · 素材 © AirPano`;
        hint.appendChild(sp);
      }
    };
    /* 模式调度: sphere(真360°全景) / pano(实拍柱面环景) / cube(3D 图墙) */
    const modesAvail = () => panoFaces ? ['sphere', 'pano', 'cube'] : ['pano', 'cube'];
    const nextMode = () => { const a = modesAvail(); return a[(a.indexOf(mode) + 1) % a.length]; };
    const updateModeBtn = () => {
      const b = vrEl.querySelector('.vr-mode-btn');
      if (b) { const n = nextMode(); b.textContent = n === 'sphere' ? '🧊 切真·360°全景' : n === 'pano' ? '🖼 切实拍环景' : '🖼 切3D图墙'; }
    };
    const buildScene = () => {
      flingVel = 0;
      if (mode === 'sphere' && panoFaces) buildSphere();
      else if (mode === 'cube') buildCube();
      else buildPano();
    };
    /* SSM 数据驱动: 查询景点是否有真实全景素材(PanoController→PanoService 白名单匹配),
       有则自动切入真 360° 立方体模式; 无素材/接口不可用则保持现有环景, 体验不降级 */
    API.get('/api/pano/' + s.id).then(p => {
      if (!p || !p.available || !p.faces || p.faces.length !== 6) return;
      panoFaces = { f: p.faces[0], r: p.faces[1], b: p.faces[2], l: p.faces[3], u: p.faces[4], d: p.faces[5] };
      panoTitle = p.title || '';
      if (!userSwitched) { mode = 'sphere'; buildScene(); }
      updateModeBtn();
    }).catch(() => { /* 保持现有模式 */ });

    const spin = () => {
      if (!dragging) {
        if (Math.abs(flingVel) > 0.05) { ry += flingVel; flingVel *= 0.94; }   // 惯性甩动衰减
        else ry += (mode === 'sphere' ? 0.04 : mode === 'pano' ? 0.055 : 0.12);   // 静止时缓慢自动环视
      }
      const el = scene.firstElementChild;
      if (el) el.style.transform = `rotateX(${rx}deg) rotateY(${ry}deg)`;
      raf = requestAnimationFrame(spin);
    };
    scene.addEventListener('pointerdown', e => { dragging = true; flingVel = 0; lx = e.clientX; ly = e.clientY; scene.setPointerCapture(e.pointerId); });
    scene.addEventListener('pointermove', e => {
      if (!dragging) return;
      const dx = e.clientX - lx;
      if (mode === 'sphere') {                                                  // 立方体全景: 角速度随 FOV 缩放, 俯仰 ±85°
        const k = hfov / window.innerWidth * 1.5;
        ry += dx * k;
        flingVel = dx * k * 0.4;
        rx = Math.max(-85, Math.min(85, rx - (e.clientY - ly) * (hfov / window.innerHeight) * 1.5));
      } else {
        ry += dx * 0.35;
        flingVel = dx * 0.14;                                                   // 记录速度用于松手惯性
        rx = Math.max(-42, Math.min(28, rx - (e.clientY - ly) * 0.2));
      }
      lx = e.clientX; ly = e.clientY;
    });
    scene.addEventListener('pointerup', () => dragging = false);
    scene.addEventListener('pointercancel', () => dragging = false);

    /* 滚轮 / 双指捏合缩放(FOV 视角拉近拉远, 街景手感) */
    scene.addEventListener('wheel', e => {
      e.preventDefault();
      if (mode === 'sphere') {                                                  // 立方体全景: 缩放 = 改变视场角 FOV
        hfov = Math.max(50, Math.min(115, hfov * (e.deltaY > 0 ? 1.08 : 0.92)));
        applyFov();
      } else {
        zoom = Math.max(0.55, Math.min(1.9, zoom * (e.deltaY > 0 ? 0.92 : 1.08)));
        scene.style.perspective = Math.round(basePersp * zoom) + 'px';
      }
    }, { passive: false });

    /* 手机陀螺仪: 转动手机即可环视(桌面无此事件自动忽略) */
    const onGyro = e => {
      if (dragging || e.alpha == null) return;
      ry = 360 - e.alpha;
      const b = (e.beta || 0) - 70;
      rx = mode === 'sphere' ? Math.max(-85, Math.min(85, b)) : Math.max(-42, Math.min(28, b));
    };
    let gyroBound = false;
    const bindGyro = () => {
      if (gyroBound) return;
      gyroBound = true;
      window.addEventListener('deviceorientation', onGyro);
    };
    try {
      if (window.DeviceOrientationEvent && typeof DeviceOrientationEvent.requestPermission === 'function') {
        DeviceOrientationEvent.requestPermission().then(r => { if (r === 'granted') bindGyro(); }).catch(() => {});
      } else {
        const probe = e => { if (e && e.alpha != null) { bindGyro(); window.removeEventListener('deviceorientation', probe); } };
        window.addEventListener('deviceorientation', probe);
      }
    } catch (err) { /* 陀螺仪不可用不影响环景 */ }

    /* 真·全屏沉浸: 先请求浏览器全屏(地址栏/标签栏隐藏), 再按全屏尺寸构建场景 */
    const enterFs = () => {
      try {
        const el = document.documentElement;
        if (!document.fullscreenElement && el.requestFullscreen) {
          return el.requestFullscreen().catch(() => {});
        }
      } catch (e) { /* iPhone Safari 不支持, 遮罩本身已铺满视口 */ }
      return Promise.resolve();
    };
    enterFs().then(() => {
      document.body.appendChild(vrEl);
      buildScene();
    });

    /* ---- SSM 后端打卡: 进入 VR 即落库 vr_view_log(Controller→Service→Mapper→MySQL) ---- */
    API.post('/api/vr/view', { spotId: s.id, sessionId: state.tourSession || undefined })
      .then(d => {
        const el = vrEl && vrEl.querySelector('#vrCount');
        if (el && d && d.vrCount != null) el.textContent = `🌐 已被 VR 游览 ${d.vrCount} 次`;
      })
      .catch(() => { /* 打卡失败不影响环景体验 */ });

    /* ---- AI 融合: 进入环景自动生成讲解词, 字幕 + 自动朗读(可点按钮重听) ---- */
    const aiBtn = vrEl.querySelector('#vrAiBtn'), cap = vrEl.querySelector('#vrCaption');
    const startExplain = async () => {
      if (aiBtn.dataset.busy) return;
      aiBtn.dataset.busy = '1';
      cap.innerHTML = '🤖 AI 正在结合知识库生成本景点讲解…';
      cap.hidden = false;
      try {
        const r = await API.get('/api/ai/explain?spotId=' + s.id);
        const text = (r && r.answer) || '';
        cap.dataset.text = text;
        cap.innerHTML = `🎙 ${esc(text).replace(/\n/g, '<br/>')}`;
        speak(text);
        aiBtn.dataset.done = '1'; aiBtn.textContent = '🔄 重听讲解';
      } catch (err) {
        cap.innerHTML = '🎙 讲解生成失败, 点击「🔊 AI 讲解」重试';
        aiBtn.textContent = '🔊 AI 讲解';
      } finally { delete aiBtn.dataset.busy; }
    };
    aiBtn.addEventListener('click', () => {
      if (aiBtn.dataset.done) { cap.hidden = false; speak(cap.dataset.text || ''); return; }
      startExplain();
    });
    if (!switching) setTimeout(startExplain, 600);   // 首次进入自动开讲; 模式切换不重复生成

    /* VR 配乐: 主 BGM 没开则临时注入 B站 iframe 播放; 模式切换时已存在的不重复注入 */
    if (!switching) vrMusicEnsure();

    const close = () => {
      cancelAnimationFrame(raf);
      stopSpeak();
      window.removeEventListener('deviceorientation', onGyro);
      vrMusicRelease();
      if (vrEl) { vrEl.remove(); vrEl = null; }
      /* 退出浏览器全屏, 还原页面 */
      try { if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(() => {}); } catch (e) { /* 忽略 */ }
      document.removeEventListener('keydown', onKey);
    };
    const onKey = e => { if (e.key === 'Escape') close(); };
    vrEl.querySelector('.vr-close').addEventListener('click', close);
    vrEl.querySelector('.vr-mode-btn').addEventListener('click', () => {
      userSwitched = true;
      mode = nextMode();
      updateModeBtn();
      buildScene();                                                             // 原地切换: 配乐/讲解/打卡全部保留
    });
    document.addEventListener('keydown', onKey);
    spin();
  }

  /* ============================================================
   * 景点详情弹窗
   * ============================================================ */
  async function openSpot(id) {
    const modal = $('#spotModal'), body = $('#spotModalBody');
    modal.hidden = false;
    body.innerHTML = `<div class="empty">加载中…</div>`;
    try {
      const s = await API.get('/api/spots/' + id);
      const comments = await API.get('/api/comments?spotId=' + id + '&size=5');
      body.innerHTML = `
        <div class="detail-gallery">
          <img class="g-main" ${imgAttr(s.gallery[0] || s.imageUrl)} />
          <img ${imgAttr(s.gallery[1] || s.imageUrl)} />
          <img ${imgAttr(s.gallery[2] || s.imageUrl)} />
        </div>
        <div class="detail-body">
          <h2>${esc(s.name)} <span class="spot-rating" style="font-size:15px">⭐ ${s.rating.toFixed(1)}（${s.ratingCount} 条评分）</span>
            <button class="btn" id="vrBtn" title="进入 3D 环景预览" style="margin-left:auto">🥽 环景</button>
            <button class="btn" id="journalBtn" title="AI 生成一句朋友圈游记">✍️ AI游记</button>
            <button class="btn" id="modalFav">${s.favorited ? '❤️ 已收藏' : '🤍 收藏'}</button>
          </h2>
          <div id="journalBody" hidden style="margin-top:10px"></div>
          <div class="tag-row" style="margin-top:8px">
            <span class="tag" style="background:#e0f2fe;color:#0369a1">${esc(s.categoryName)}</span>
            ${s.tags.map(t => `<span class="tag">${esc(t)}</span>`).join('')}
          </div>
          <div class="info-grid">
            <div class="info-cell">门票<b>${s.price && Number(s.price) > 0 ? '¥' + Number(s.price) : '免费'}</b></div>
            <div class="info-cell">开放时间<b>${esc(s.openTime)}</b></div>
            <div class="info-cell">建议游玩<b>${s.durationMinutes} 分钟</b></div>
            <div class="info-cell">体力强度<b>${diffText(s.difficulty)}</b></div>
            <div class="info-cell" style="grid-column:span 2">位置<b>${esc(s.province)} · ${esc(s.city || '')} · ${esc(s.address)}</b></div>
            <div class="info-cell" style="grid-column:span 2">适合人群<b>${s.suitableFor.join('、')}</b></div>
          </div>
          ${Number(s.price) > 0 ? ticketBuyPanel(s) : ''}
          ${mapPanel(s)}
          <div class="detail-desc">${esc(s.description)}</div>
          <div><b>🌟 核心看点</b><div class="tag-row" style="margin-top:6px">${s.highlights.map(h => `<span class="tag">${esc(h)}</span>`).join('')}</div></div>
          <div class="tip-box">⚠️ 温馨提示：${esc(s.tips)}</div>
          ${aiExplainPanel(s)}
          <h3 style="margin:18px 0 10px">📍 附近的景点（按经纬度直线距离）</h3>
          <div id="nearbyBox" class="nearby-row"><div class="empty" style="padding:10px">计算中…</div></div>

          <h3 style="margin:18px 0 10px">💬 用户点评（${comments.total}）</h3>
          <div id="commentForm">
            <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
              <span style="font-size:13px">我的评分：</span>
              <div class="stars" id="starRow">${[1,2,3,4,5].map(i => `<span data-star="${i}" class="${i <= 5 ? 'on' : ''}">★</span>`).join('')}</div>
            </div>
            <textarea class="input" id="commentText" rows="2" style="width:100%" placeholder="分享你的游玩感受（2-300 字）"></textarea>
            <button class="btn btn-primary" id="commentSubmit" style="margin-top:8px">发表点评</button>
          </div>
          <div id="commentList" style="margin-top:14px">
            ${comments.list.length ? comments.list.map(c => `
              <div class="comment-item">
                <span class="avatar">${esc((c.nickname || '游').slice(0, 1))}</span>
                <div><div class="comment-meta"><b>${esc(c.nickname || '游客')}</b> · ${'⭐'.repeat(c.rating)} · ${new Date(c.createdAt).toLocaleDateString('zh-CN')}</div>
                <div class="comment-text">${esc(c.content)}</div></div>
              </div>`).join('') : '<div class="empty" style="padding:20px">还没有点评，来做第一个分享的人吧</div>'}
          </div>
        </div>`;
      state.commentRating = 5;
      bindModalInteractions(s, comments);
      loadNearby(s);
      // 初始化 Leaflet 交互式地图 (真实经纬度定位)
      initLeafletForSpot(s.id, s.lng, s.lat, s.name);
    } catch (e) {
      body.innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
    }
  }

  function bindModalInteractions(s) {
    let explainText = '';
    $('#spotModalClose').onclick = () => { $('#spotModal').hidden = true; stopSpeak(); };
    // AI 一句话游记: 朋友圈风格文案, RAG 约束真实景点, 一键复制去发圈
    $('#journalBtn').onclick = async () => {
      const btn = $('#journalBtn'), box = $('#journalBody');
      btn.disabled = true; btn.textContent = '✍️ 生成中…';
      box.hidden = false;
      box.innerHTML = '<div class="empty" style="padding:10px">🤖 AI 正在结合景点资料写游记…</div>';
      try {
        const r = await API.get('/api/ai/journal?spotId=' + s.id);
        const badge = r.engine === 'deepseek' ? '<span class="reply-badge llm">⚡ DeepSeek</span>'
          : r.engine === 'qwen' ? '<span class="reply-badge llm">⚡ 通义千问</span>'
          : r.engine === 'doubao' ? '<span class="reply-badge llm">⚡ 豆包AI</span>'
          : '<span class="reply-badge local">📚 本地文案</span>';
        box.innerHTML = `<div class="explain-content">${badge}${esc(r.answer)}
          <div style="margin-top:8px"><button class="btn" id="journalCopy">📋 复制文案</button></div></div>`;
        $('#journalCopy').onclick = () => {
          const done = () => toast('游记已复制，去发朋友圈吧 📸');
          if (navigator.clipboard?.writeText) navigator.clipboard.writeText(r.answer).then(done);
          else done();
        };
        btn.disabled = false; btn.textContent = '🔄 换一句';
        toast('游记已生成');
      } catch (e) {
        box.innerHTML = '<div class="empty" style="padding:10px">生成失败，点按钮再试一次</div>';
        btn.disabled = false; btn.textContent = '✍️ AI游记';
      }
    };
    // 地图 Tab 切换: 交互地图 ↔ 位置示意
    const mapTabs = $(`#mapTabs_${s.id}`);
    if (mapTabs) {
      $$('.map-tab', mapTabs).forEach(tab => tab.onclick = () => {
        $$('.map-tab', mapTabs).forEach(t => t.classList.toggle('active', t === tab));
        const mode = tab.dataset.map;
        // 切换 Leaflet 与 SVG 示意
        const leaflet = $('#spotLeaflet_' + s.id);
        const schematic = $('.map-schematic', mapTabs.parentElement.parentElement);
        if (mode === 'leaflet') {
          if (leaflet) leaflet.hidden = false;
          if (schematic) schematic.hidden = true;
          // 重绘 Leaflet
          if (leaflet && leaflet._leafletMap) leaflet._leafletMap.invalidateSize();
        } else {
          if (leaflet) leaflet.hidden = true;
          if (schematic) schematic.hidden = false;
        }
      });
    }
    // AI 深度讲解: 优先调用专用 /explain 端点; 旧服务未重启时回退 /chat 端点
    $('#modalExplain').onclick = async () => {
      const btn = $('#modalExplain'), body = $('#aiExplainBody'), voiceRow = $('#explainVoiceRow');
      const badge = engine => (engine === 'doubao' || engine === 'llm')
        ? '<span class="reply-badge llm">⚡ 豆包AI</span>'
        : engine === 'deepseek'
        ? '<span class="reply-badge llm">⚡ DeepSeek</span>'
        : engine === 'qwen'
        ? '<span class="reply-badge llm">⚡ 通义千问</span>'
        : '<span class="reply-badge local">📚 本地知识库</span>';
      const render = r => {
        explainText = r.answer || '';
        body.innerHTML = `<div class="explain-content">${badge(r.engine)}${esc(r.answer).replace(/\n/g, '<br/>')}</div>`;
        voiceRow.hidden = false;
        btn.textContent = '🔄 重新生成';
        toast('讲解已生成，可点击朗读聆听');
      };
      btn.disabled = true; btn.textContent = '✨ 讲解生成中…';
      body.hidden = false;
      body.innerHTML = '<div class="empty" style="padding:12px">🤖 AI 正在结合知识库生成深度讲解…</div>';
      voiceRow.hidden = true;
      try {
        const r = await API.get('/api/ai/explain?spotId=' + s.id);
        render(r);
      } catch (e) {
        // 回退: 用对话接口 + 引导话术生成深度讲解(兼容旧服务)
        try {
          const r = await API.post('/api/ai/chat', {
            message: `请为${s.name}（${s.province}${s.city || ''}）生成一段深度讲解，包含历史文化背景、核心看点解读、游览建议和实用贴士。`
          });
          render(r);
        } catch (e2) {
          body.innerHTML = '<div class="empty" style="padding:12px">讲解生成失败：' + esc(e2.message) + '</div>';
          btn.textContent = '✨ 生成深度讲解';
        }
      } finally {
        btn.disabled = false;
      }
    };
    $('#modalListen').onclick = () => {
      const text = explainText || `${s.name}。${s.description}。看点：${s.highlights.join('、')}。温馨提示：${s.tips}`;
      speak(text); toast('AI 讲解播放中');
    };
    $('#modalStop').onclick = () => { stopSpeak(); toast('已停止'); };

    // 购票面板交互
    const qtyInput = $('#ticketQty');
    if (qtyInput) {
      const updateTotal = () => {
        const q = Math.max(1, Math.min(20, parseInt(qtyInput.value) || 1));
        qtyInput.value = q;
        const total = (Number(s.price) * q).toFixed(2);
        $('.ticket-total').textContent = '¥' + total;
      };
      $$('.qty-btn', $('#ticketPanel')).forEach(btn => btn.onclick = () => {
        const q = parseInt(qtyInput.value) || 1;
        qtyInput.value = btn.dataset.act === 'plus' ? q + 1 : Math.max(1, q - 1);
        updateTotal();
      });
      qtyInput.oninput = updateTotal;
    }
    const buyBtn = $('#ticketBuyBtn');
    if (buyBtn) {
      buyBtn.onclick = async () => {
        if (!state.me) { toast('请先登录后再购票'); $('#spotModal').hidden = true; switchTab('me'); return; }
        const q = parseInt(qtyInput.value) || 1;
        const phone = $('#ticketPhone').value.trim();
        const name = $('#ticketName').value.trim();
        const date = $('#ticketDate').value;
        if (phone.length > 0 && !/^\d{11}$/.test(phone)) { toast('手机号请输入 11 位数字'); return; }
        buyBtn.disabled = true; buyBtn.textContent = '下单中…';
        try {
          const order = await API.post('/api/orders', {
            spotId: s.id, quantity: q, contactName: name, contactPhone: phone,
            visitDate: date, remark: ''
          });
          buyBtn.textContent = '立即购买'; buyBtn.disabled = false;
          // 弹模拟支付网关
          openSandboxPay(order, buyBtn);
        } catch (e) {
          toast(e.message);
          buyBtn.disabled = false; buyBtn.textContent = '立即购买';
        }
      };
    }
    /* VR 环景舱: 用该景点实拍照片构建 CSS 3D 四面图墙, 拖拽旋转视角 */
    const vrBtn = $('#vrBtn');
    if (vrBtn) vrBtn.onclick = () => openVr(s);

    $('#modalFav').onclick = async (e) => {
      if (!state.me) { toast('请先登录后再收藏'); $('#spotModal').hidden = true; switchTab('me'); return; }
      try {
        const r = await API.post('/api/favorites', { spotId: s.id });
        e.target.textContent = r.favorited ? '❤️ 已收藏' : '🤍 收藏';
        toast(r.favorited ? '已加入收藏' : '已取消收藏');
      } catch (err) { toast(err.message); }
    };
    $$('#starRow span').forEach(star => star.onclick = () => {
      state.commentRating = Number(star.dataset.star);
      $$('#starRow span').forEach(x => x.classList.toggle('on', Number(x.dataset.star) <= state.commentRating));
    });
    $('#commentSubmit').onclick = async () => {
      if (!state.me) { toast('请先登录后再点评'); $('#spotModal').hidden = true; switchTab('me'); return; }
      const content = $('#commentText').value.trim();
      try {
        await API.post('/api/comments', { spotId: s.id, rating: state.commentRating, content });
        toast('点评发表成功');
        openSpot(s.id);
      } catch (e) { toast(e.message); }
    };
  }
  $('#spotModal').addEventListener('click', e => { if (e.target.id === 'spotModal') { e.target.hidden = true; stopSpeak(); } });

  /* 卡片通用事件：点卡片打开详情，点收藏按钮单独处理 */
  function bindCardEvents(root) {
    root.querySelectorAll('[data-spot]').forEach(el => {
      if (el.dataset.bound) return;
      el.dataset.bound = '1';
      el.addEventListener('click', async e => {
        if (e.target.closest('[data-fav]')) return;
        openSpot(Number(el.dataset.spot));
      });
    });
    root.querySelectorAll('[data-fav]').forEach(btn => {
      if (btn.dataset.bound) return;
      btn.dataset.bound = '1';
      btn.addEventListener('click', async e => {
        e.stopPropagation();
        if (!state.me) { toast('请先登录后再收藏'); switchTab('me'); return; }
        try {
          const r = await API.post('/api/favorites', { spotId: Number(btn.dataset.fav) });
          btn.textContent = r.favorited ? '❤️' : '🤍';
          toast(r.favorited ? '已收藏' : '已取消收藏');
        } catch (err) { toast(err.message); }
      });
    });
  }

  /* 确定性伪二维码: 同一订单号永远生成同一张码 (FNV 哈希 + xorshift 种子), 沙箱演示用 */
  function qrSvg(seed) {
    let h = 2166136261;
    for (let i = 0; i < seed.length; i++) { h ^= seed.charCodeAt(i); h = Math.imul(h, 16777619); }
    const rand = () => { h ^= h << 13; h ^= h >>> 17; h ^= h << 5; return ((h >>> 0) / 4294967296); };
    const N = 21, M = 7;
    const inFinder = (r, c) => (r < 7 && c < 7) || (r < 7 && c >= N - 7) || (r >= N - 7 && c < 7);
    let cells = '';
    for (let r = 0; r < N; r++) for (let c = 0; c < N; c++) {
      if (!inFinder(r, c) && rand() > 0.52)
        cells += `<rect x="${c * M}" y="${r * M}" width="6.2" height="6.2" fill="#1f2937"/>`;
    }
    const finder = (ox, oy) => `<rect x="${ox}" y="${oy}" width="${7 * M}" height="${7 * M}" fill="#1f2937"/>`
      + `<rect x="${ox + M}" y="${oy + M}" width="${5 * M}" height="${5 * M}" fill="#fff"/>`
      + `<rect x="${ox + 2 * M}" y="${oy + 2 * M}" width="${3 * M}" height="${3 * M}" fill="#1f2937"/>`;
    cells += finder(0, 0) + finder((N - 7) * M, 0) + finder(0, (N - 7) * M);
    return `<svg viewBox="0 0 ${N * M} ${N * M}" width="100%" height="100%" style="display:block">${cells}</svg>`;
  }

  /* 电子入园凭证放大弹窗 */
  function openVoucher(o) {
    const mask = document.createElement('div');
    mask.className = 'modal-mask';
    mask.style.zIndex = 200;
    mask.innerHTML = `
      <div class="voucher-modal">
        <button class="voucher-close" id="vcClose">×</button>
        <div class="vm-head">🎫 电子入园凭证</div>
        <div class="vm-qr">${qrSvg(o.orderNo)}</div>
        <div class="vm-no">${esc(o.orderNo)}</div>
        <div class="vm-row"><span>景点</span><b>${esc(o.spotName)}</b></div>
        <div class="vm-row"><span>门票</span><b>${o.quantity} 张 · ¥${Number(o.totalAmount).toFixed(2)}</b></div>
        ${o.visitDate ? `<div class="vm-row"><span>游玩日期</span><b>${esc(o.visitDate)}</b></div>` : ''}
        ${o.contactName ? `<div class="vm-row"><span>联系人</span><b>${esc(o.contactName)}</b></div>` : ''}
        <div class="vm-tip">入园时向工作人员出示此码 · 虚拟二维码 (沙箱演示, 不可扫描)</div>
      </div>`;
    document.body.appendChild(mask);
    $('#vcClose', mask).onclick = () => mask.remove();
    mask.onclick = (e) => { if (e.target === mask) mask.remove(); };
  }

  /* ---------------- 我的订单 ---------------- */
  async function renderOrders(filter) {
    filter = filter || 'ALL';
    const app = $('#app');
    if (!state.me) {
      app.innerHTML = `
      <div class="me-section" style="max-width:560px;margin:46px auto;text-align:center">
        <h3>🧾 我的订单</h3>
        <div class="empty">登录后可查看门票订单，支持在线支付与退订</div>
        <button class="btn btn-primary" id="ordGoLogin" style="padding:10px 40px">去登录</button>
      </div>`;
      $('#ordGoLogin').onclick = () => switchTab('me');
      return;
    }
    app.innerHTML = `
      <div class="me-section">
        <h3>🧾 我的订单 <small style="font-weight:400;color:var(--ink-3);font-size:12.5px">下单 → 支付 → 出票 → 退订，全流程在线闭环</small></h3>
        <div id="ordStats" class="ord-stats"><div class="empty">加载中…</div></div>
        <div id="ordFilter" class="ord-filter"></div>
        <div id="orderList"><div class="empty">加载中…</div></div>
      </div>`;
    let orders = [];
    try { orders = await API.get('/api/orders'); }
    catch (e) { $('#orderList').innerHTML = `<div class="empty">订单加载失败：${esc(e.message)}</div>`; $('#ordStats').innerHTML = ''; $('#ordFilter').innerHTML = ''; return; }

    /* 创新点1: 消费看板 — 客户端实时聚合订单数据 */
    const paid = orders.filter(o => o.payStatus === 'PAID');
    const total = paid.reduce((s, o) => s + Number(o.totalAmount || 0), 0);
    const tickets = orders.reduce((s, o) => s + (o.payStatus === 'CANCELED' ? 0 : (o.quantity || 0)), 0);
    const pending = orders.filter(o => o.payStatus === 'PENDING').length;
    $('#ordStats').innerHTML = `
      <div class="ostat"><b>¥${total.toFixed(2)}</b><span>累计消费</span></div>
      <div class="ostat"><b>${orders.length}</b><span>全部订单</span></div>
      <div class="ostat"><b>${tickets}</b><span>持票张数</span></div>
      <div class="ostat ${pending ? 'warn' : ''}"><b>${pending}</b><span>待支付</span></div>`;

    /* 创新点2: 状态筛选 chips */
    const chips = [['ALL', `全部 ${orders.length}`], ['PENDING', `待支付 ${pending}`],
      ['PAID', `已支付 ${paid.length}`], ['CANCELED', `已取消 ${orders.filter(o => o.payStatus === 'CANCELED').length}`]];
    $('#ordFilter').innerHTML = chips.map(([k, label]) =>
      `<button class="ofchip ${filter === k ? 'on' : ''}" data-f="${k}">${label}</button>`).join('');
    $('#ordFilter').onclick = (ev) => {
      const b = ev.target.closest('.ofchip'); if (b) renderOrders(b.dataset.f);
    };

    const list = filter === 'ALL' ? orders : orders.filter(o => o.payStatus === filter);
    if (!list.length) {
      $('#orderList').innerHTML = `<div class="empty">${filter === 'ALL' ? '还没有订单 · 去「景点导览」挑个景点买票吧 🎫' : '该状态下暂无订单'}</div>`;
      return;
    }
    const badge = (st) => st === 'PAID'
      ? '<span class="ord-badge ok">✅ 已支付</span>'
      : st === 'CANCELED' ? '<span class="ord-badge off">已取消</span>'
      : '<span class="ord-badge wait">⏳ 待支付</span>';
    /* 创新点3: 票根式门票卡片( perforation 打孔 + 存根区 ); 已支付订单内嵌电子凭证二维码 */
    $('#orderList').innerHTML = list.map(o => `
      <div class="order-card ${o.payStatus === 'CANCELED' ? 'is-off' : ''}" data-no="${esc(o.orderNo)}">
        <div class="oc-stub">
          <div class="oc-stub-spot">${esc((o.spotName || '').slice(0, 6))}</div>
          <div class="oc-stub-price">¥${o.totalAmount}</div>
          <div class="oc-stub-no">${esc(o.orderNo)}</div>
        </div>
        <div class="oc-main">
          <div class="oc-top">
            <b>🎫 ${esc(o.spotName)}</b>${badge(o.payStatus)}
            <span class="oc-kind">${o.payStatus === 'PAID' ? '电子票 · 可入园' : '预订单'}</span>
          </div>
          <div class="oc-info">
            ${o.quantity} 张 × ¥${o.unitPrice}
            ${o.visitDate ? ` · 游玩日期 ${esc(o.visitDate)}` : ''}
            ${o.contactName ? ` · 联系人 ${esc(o.contactName)}` : ''}
            · 下单于 ${(o.createdAt || '').replace('T', ' ').slice(0, 16)}
          </div>
          ${o.payStatus === 'PAID' ? `
          <div class="oc-voucher">
            <div class="ocv-qr">${qrSvg(o.orderNo)}</div>
            <div class="ocv-info">
              <b>🎫 电子入园凭证</b>
              <span>凭码 + 联系人手机号核销入园 · 码随单号唯一固化</span>
            </div>
          </div>` : ''}
          <div class="oc-actions">
            ${o.payStatus === 'PENDING'
              ? `<button class="btn btn-primary oc-pay">💳 模拟支付</button><button class="btn oc-cancel">取消订单</button>`
              : o.payStatus === 'PAID'
                ? `<button class="btn btn-primary ocv-big">🔍 放大出示凭证</button><button class="btn oc-refund" title="沙箱退款, 费用原路退回">↩ 申请退款</button>`
                : `<button class="btn oc-del">🗑 删除记录</button>`}
            <button class="btn oc-again" data-spot="${o.spotId}">🔁 再来一单</button>
          </div>
        </div>
      </div>`).join('');
    /* 支付 / 退款 / 删除 / 凭证放大 (事件委托) */
    $('#orderList').onclick = async (ev) => {
      if (ev.target.classList.contains('oc-again')) {
        switchTab('explore'); openSpot(Number(ev.target.dataset.spot)); return;   // 一键复购: 跳详情直达买票
      }
      const card = ev.target.closest('.order-card'); if (!card) return;
      const no = card.dataset.no;
      const order = orders.find(x => x.orderNo === no);
      if (ev.target.classList.contains('ocv-big') || ev.target.closest('.ocv-qr')) {
        if (order) openVoucher(order);   // 打开放大版电子凭证
      } else if (ev.target.classList.contains('oc-pay')) {
        ev.target.disabled = true; ev.target.textContent = '支付中…';
        try { await API.post(`/api/orders/${no}/pay`); toast('支付成功，已出票 🎉'); }
        catch (e) { toast(e.message); }
        renderOrders(filter);
      } else if (ev.target.classList.contains('oc-cancel') || ev.target.classList.contains('oc-refund')) {
        const isRefund = ev.target.classList.contains('oc-refund');
        if (!confirm(isRefund ? '申请退款将取消该订单, 费用原路退回 (沙箱)。确定吗?' : '确定取消该订单吗?')) return;
        try { await API.post(`/api/orders/${no}/cancel`); toast(isRefund ? '退款成功, 费用已原路退回 (沙箱)' : '订单已取消'); }
        catch (e) { toast(e.message); }
        renderOrders(filter);
      } else if (ev.target.classList.contains('oc-del')) {
        if (!confirm('删除后不可恢复, 确定删除该订单记录吗?')) return;
        try { await API.post(`/api/orders/${no}/delete`); toast('订单记录已删除'); }
        catch (e) { toast(e.message); }
        renderOrders(filter);
      }
    };
  }

  /* ---------------- 全国景点地图 (Leaflet) ---------------- */
  let mapInstance = null;
  async function renderMap() {
    const app = $('#app');
    app.innerHTML = `
      <h2 class="section-title">🗺 全国景点地图</h2>
      <p style="color:var(--ink-2); margin-bottom:14px; font-size:14px;">
        219 个真实景点分布 · 点击标记查看详情 · 滚轮缩放 · 拖拽平移
      </p>
      <div id="spotMap" style="width:100%; height:580px; border-radius:14px; overflow:hidden; box-shadow:var(--shadow); z-index:1;"></div>
      <div id="mapLegend" style="display:flex; gap:16px; margin-top:12px; font-size:13px; color:var(--ink-2);"></div>
    `;
    const mapEl = $('#spotMap');
    if (!mapEl || typeof L === 'undefined') {
      app.innerHTML = '<p style="padding:40px;text-align:center;color:var(--ink-3);">地图库未加载, 请刷新重试</p>';
      return;
    }
    if (mapInstance) { try { mapInstance.remove(); } catch(e){} mapInstance = null; }
    // 初始化地图 (中国中心)
    const map = L.map(mapEl, { center: [34.5, 108.0], zoom: 4, zoomControl: true, scrollWheelZoom: true });
    mapInstance = map;
    // 高德瓦片 (国内可访问, 多源 fallback)
    const addTile = (url, opts) => L.tileLayer(url, opts).addTo(map);
    try {
      addTile('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
        { subdomains: ['1','2','3','4'], attribution: '© 高德地图', maxZoom: 18 });
    } catch(e) {
      addTile('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
        { subdomains: ['a','b','c'], attribution: '© OpenStreetMap', maxZoom: 18 });
    }
    try {
      const spots = await API.get('/api/spots/map');
      let count = 0;
      // 按省份分组的颜色
      const colors = ['#0e9f6e','#ff7a45','#2563eb','#f59e0b','#8b5cf6','#ec4899','#06b6d4','#84cc16'];
      spots.forEach(s => {
        if (s.lat && s.lng) {
          const marker = L.circleMarker([s.lat, s.lng], {
            radius: 7, fillColor: '#0e9f6e', color: '#fff', weight: 2, fillOpacity: 0.85
          }).addTo(map);
          // 鼠标悬停显示景点名
          marker.bindTooltip(esc(s.name), { direction: 'top', offset: [0, -7] });
          // 点击弹出详情 + 简介
          const summary = s.summary ? esc(s.summary).substring(0, 80) + (s.summary.length > 80 ? '…' : '') : '';
          marker.bindPopup(
            `<div style="min-width:240px;max-width:300px;">
              <div style="font-size:15px;font-weight:700;color:#1f2937;margin-bottom:4px;">${esc(s.name)}</div>
              <div style="color:#9ca3af;font-size:12px;margin-bottom:6px;">${esc(s.province || '')} ${esc(s.city || '')} ${esc(s.region || '')}</div>
              <div style="margin-bottom:6px;">
                <span style="color:#f59e0b;">★ ${s.rating || '-'}</span>
                <span style="color:#9ca3af;font-size:12px;">(${s.ratingCount || 0}评)</span>
                ${s.price > 0 ? `<span style="color:#ff7a45;margin-left:10px;font-weight:600;">¥${s.price}</span>` : '<span style="color:#0e9f6e;margin-left:10px;font-weight:600;">免费</span>'}
              </div>
              ${summary ? `<div style="color:#4b5563;font-size:12.5px;line-height:1.5;margin-bottom:8px;">${summary}</div>` : ''}
              <button onclick="openSpot(${s.id})" style="padding:5px 14px;border:0;border-radius:999px;background:#0e9f6e;color:#fff;cursor:pointer;font-size:13px;font-weight:500;">进入景点详情 →</button>
            </div>`, { maxWidth: 320 });
          count++;
        }
      });
      const legend = $('#mapLegend');
      if (legend) legend.innerHTML = `📊 已标注 <b style="color:var(--primary)">${count}</b> 个景点 · 🟢 标记为景点位置 · 点击查看详情`;
      toast(`🗺 地图已加载 ${count} 个景点`);
    } catch(e) {
      toast('景点数据加载失败: ' + e.message);
    }
  }

  /* ---------------- 渲染分发 ---------------- */
  /* ============================================================
   * 🌌 星际探索: 魔幻银河系 3D 行星轨道图 + 天文科普
   * ============================================================ */
  const PLANETS = [
    { name: '水星 Mercury', color: '#b5b5b5', size: 8, orbit: 70, speed: 8, tilt: 0.03, tex: 'mercury.jpg', note: 220,
      spot: { province: '甘肃', theme: '戈壁荒漠', icon: '🏜️' },
      desc: '距太阳最近的行星，表面布满陨石坑，无大气层，昼夜温差超 600℃。', diameter: '4,879 km', dist: '5,790 万 km', day: '58.6 地球日', year: '88 地球日', moons: 0, temp: '-173~427℃', gravity: '3.7 m/s²' },
    { name: '金星 Venus', color: '#e8c07d', size: 12, orbit: 105, speed: 12, tilt: 177.4, tex: 'venus.jpg', note: 233.08,
      spot: { province: '云南', theme: '热带雨林', icon: '🌴' },
      desc: '太阳系最热行星，浓密二氧化碳大气造成失控温室效应，表面温度达 462℃。', diameter: '12,104 km', dist: '1.082 亿 km', day: '243 地球日', year: '225 地球日', moons: 0, temp: '462℃', gravity: '8.87 m/s²' },
    { name: '地球 Earth', color: '#4a90d9', size: 13, orbit: 145, speed: 16, tilt: 23.4, cloud: true, tex: 'earth.jpg', cloudTex: 'earth_clouds.jpg', note: 261.63,
      spot: { province: '', theme: '全国热门', icon: '🌏' },
      desc: '我们的家园，目前已知唯一存在生命的星球，71% 被海洋覆盖，拥有一颗天然卫星月球。', diameter: '12,742 km', dist: '1.496 亿 km', day: '24 小时', year: '365.25 天', moons: 1, temp: '-89~58℃', gravity: '9.81 m/s²' },
    { name: '火星 Mars', color: '#d9603b', size: 10, orbit: 185, speed: 20, tilt: 25.2, tex: 'mars.jpg', note: 293.66,
      spot: { province: '新疆', theme: '火星地貌', icon: '🔴' },
      desc: '红色星球，拥有太阳系最高山奥林匹斯山(21km)和最大峡谷水手谷，人类移居首选。', diameter: '6,779 km', dist: '2.279 亿 km', day: '24.6 小时', year: '687 地球日', moons: 2, temp: '-140~20℃', gravity: '3.71 m/s²' },
    { name: '木星 Jupiter', color: '#d4a574', size: 30, orbit: 250, speed: 30, tilt: 3.1, tex: 'jupiter.jpg', note: 196,
      spot: { province: '西藏', theme: '巨型高原', icon: '🏔️' },
      desc: '太阳系最大行星，气态巨行星，大红斑是持续数百年的巨型风暴，质量是其他行星总和的2.5倍。', diameter: '139,820 km', dist: '7.785 亿 km', day: '9.9 小时', year: '11.86 地球年', moons: 95, temp: '-108℃', gravity: '24.79 m/s²' },
    { name: '土星 Saturn', color: '#e8d5a3', size: 26, orbit: 315, speed: 40, tilt: 26.7, ring: true, tex: 'saturn.jpg', note: 174.61,
      spot: { province: '贵州', theme: '梯田瀑布', icon: '🌾' },
      desc: '以壮观环系闻名，环由冰和岩石碎片组成，密度比水还小，拥有太阳系最多的卫星。', diameter: '116,460 km', dist: '14.34 亿 km', day: '10.7 小时', year: '29.46 地球年', moons: 146, temp: '-139℃', gravity: '10.44 m/s²' },
    { name: '天王星 Uranus', color: '#7dd3c4', size: 18, orbit: 375, speed: 55, tilt: 97.8, tex: 'uranus.jpg', note: 329.63,
      spot: { province: '青海', theme: '青蓝盐湖', icon: '💠' },
      desc: '侧躺着自转的冰巨星，自转轴倾斜 98°，大气层含甲烷使其呈青蓝色。', diameter: '50,724 km', dist: '28.71 亿 km', day: '17.2 小时', year: '84 地球年', moons: 27, temp: '-197℃', gravity: '8.69 m/s²' },
    { name: '海王星 Neptune', color: '#4166f5', size: 17, orbit: 430, speed: 70, tilt: 28.3, tex: 'neptune.jpg', note: 146.83,
      spot: { province: '海南', theme: '深蓝海洋', icon: '🌊' },
      desc: '最远行星，深蓝色冰巨星，风速可达 2,100 km/h 为太阳系之最，仅被旅行者2号探测过一次。', diameter: '49,244 km', dist: '44.95 亿 km', day: '16.1 小时', year: '164.8 地球年', moons: 14, temp: '-201℃', gravity: '11.15 m/s²' },
  ];
  // 供「AI 助教 / 景点」联动使用的待发送提问
  let pendingChatPrompt = null;

  let spaceCleanup = null;
  function renderSpace() {
    $('#app').innerHTML = `
    <div class="space-wrap">
      <div class="space-header">
        <h2>🌌 星际探索 · 魔幻银河系</h2>
        <p>Three.js 真实 3D · 拖拽旋转 · 滚轮缩放 · 点击行星查看科普</p>
        <div class="sp-toolbar">
          <button class="sp-reset" id="spReset">↻ 重置视角</button>
          <button class="sp-music" id="spMusic">🎵 背景音乐</button>
        </div>
      </div>
      <div class="space-scene" id="spaceScene">
        <div class="sp-loading" id="spLoading"><div class="sp-loading-ring"></div><span id="spLoadTxt">正在加载真实银河全景 0%</span></div>
      </div>
      <div class="sp-planet-bar" id="spPlanetBar"></div>
      <div class="sp-control-panel">
        <div class="sp-ctrl"><label>⚡ 公转速度</label><input type="range" id="spSpeed" min="0" max="5" step="0.1" value="1"><span id="spSpeedVal">1.0x</span></div>
        <div class="sp-ctrl"><label>☀️ 太阳亮度</label><input type="range" id="spBright" min="0.2" max="4" step="0.1" value="1"><span id="spBrightVal">1.0</span></div>
        <div class="sp-ctrl"><label>🔭 行星大小</label><input type="range" id="spSize" min="0.5" max="3" step="0.1" value="1"><span id="spSizeVal">1.0x</span></div>
      </div>
      <div class="sp-info" id="spInfo">
        <div class="sp-info-tip">👆 点击行星或下方按钮探索宇宙奥秘</div>
      </div>
      <div class="sp-hint">🖱 拖拽旋转 · 滚轮缩放 · 点击行星联动 AI 与景点</div>
    </div>`;

    // ---- 底部行星导航条 (与 3D 场景双向联动) ----
    $('#spPlanetBar').innerHTML = PLANETS.map((p, i) =>
      `<button class="sp-pchip" data-idx="${i}"><i style="background:${p.color}"></i>${p.name.split(' ')[0]}</button>`
    ).join('');
    const chips = $$('.sp-pchip');

    const container = $('#spaceScene');
    const W = container.clientWidth, H = 560;

    // ---- 场景 / 相机 / 渲染器 ----
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(60, W / H, 0.1, 5000);
    camera.position.set(0, 80, 180);
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(W, H);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    container.appendChild(renderer.domElement);

    // ---- 真实星空: 柔光星点 + 螺旋银河带 + 星云 ----
    const dotCanvas = hex => {
      const c = document.createElement('canvas'); c.width = c.height = 64;
      const x = c.getContext('2d');
      const g = x.createRadialGradient(32, 32, 0, 32, 32, 32);
      g.addColorStop(0, 'rgba(255,255,255,1)');
      g.addColorStop(0.22, hex);
      g.addColorStop(1, 'rgba(255,255,255,0)');
      x.fillStyle = g; x.fillRect(0, 0, 64, 64);
      const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; return t;
    };
    const dotWhite = dotCanvas('rgba(255,255,255,0.85)');
    const dotBlue = dotCanvas('rgba(170,205,255,0.9)');
    const dotWarm = dotCanvas('rgba(255,215,170,0.9)');

    const stars = new THREE.Group(); scene.add(stars);
    const makeStarLayer = (count, rMin, rMax, size, tex, opacity) => {
      const pos = [];
      for (let i = 0; i < count; i++) {
        const r = rMin + Math.random() * (rMax - rMin);
        const th = Math.random() * Math.PI * 2;
        const ph = Math.acos(2 * Math.random() - 1);
        pos.push(r * Math.sin(ph) * Math.cos(th), r * Math.sin(ph) * Math.sin(th), r * Math.cos(ph));
      }
      const geo = new THREE.BufferGeometry();
      geo.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
      return new THREE.Points(geo, new THREE.PointsMaterial({
        size, map: tex, transparent: true, opacity, depthWrite: false,
        blending: THREE.AdditiveBlending, sizeAttenuation: true
      }));
    };
    // ---- 真实银河全景天空 (ESO 8K 全天球, 带加载进度) ----
    const skyLoader = new THREE.TextureLoader();
    const skyTex = skyLoader.load('static/lib/three/sky/milkyway.jpg', () => {
      const el = $('#spLoading'); if (el) el.remove();
    }, ev => {
      const el = $('#spLoadTxt');
      if (el && ev.total) el.textContent = `正在加载真实银河全景 ${Math.round(ev.loaded / ev.total * 100)}%`;
    }, () => {
      const el = $('#spLoading'); if (el) el.remove();
      galaxy.visible = true;   // 全景失败时回退到程序化银河
    });
    skyTex.colorSpace = THREE.SRGBColorSpace;
    skyTex.anisotropy = renderer.capabilities.getMaxAnisotropy();
    const sky = new THREE.Mesh(new THREE.SphereGeometry(4300, 64, 48),
      new THREE.MeshBasicMaterial({ map: skyTex, side: THREE.BackSide, depthWrite: false, toneMapped: false }));
    sky.rotation.y = Math.PI * 0.6;
    scene.add(sky);

    stars.add(makeStarLayer(5000, 2200, 3600, 5, dotWhite, 0.16));
    stars.add(makeStarLayer(1500, 2000, 3200, 9, dotBlue, 0.22));
    stars.add(makeStarLayer(300, 1900, 3000, 15, dotWarm, 0.3));

    // 螺旋银河盘：侧向观察形成横贯天穹的银河光带
    const galaxy = new THREE.Group();
    const gPos = [], gCol = [];
    const cCore = new THREE.Color('#ffe6b8'), cArm1 = new THREE.Color('#bcd4ff'), cArm2 = new THREE.Color('#ffc9a3');
    for (let i = 0; i < 18000; i++) {
      const rr = Math.pow(Math.random(), 0.55) * 1900 + 120;
      const arm = i % 3;
      const theta = Math.random() * Math.PI * 2 + rr * 0.0016 + arm * (Math.PI * 2 / 3);
      const spread = (1 - rr / 2200) * 140 + 30;
      const x = Math.cos(theta) * rr + (Math.random() - 0.5) * spread;
      const z = Math.sin(theta) * rr + (Math.random() - 0.5) * spread;
      const y = (Math.random() - 0.5) * (60 + rr * 0.05);
      gPos.push(x, y, z);
      const c = rr < 520 ? cCore : (arm === 0 ? cArm1 : (arm === 1 ? cArm2 : cCore));
      const k = 0.55 + Math.random() * 0.45;
      gCol.push(c.r * k, c.g * k, c.b * k);
    }
    const gGeo = new THREE.BufferGeometry();
    gGeo.setAttribute('position', new THREE.Float32BufferAttribute(gPos, 3));
    gGeo.setAttribute('color', new THREE.Float32BufferAttribute(gCol, 3));
    galaxy.add(new THREE.Points(gGeo, new THREE.PointsMaterial({
      size: 8, map: dotWhite, transparent: true, opacity: 0.3, depthWrite: false,
      blending: THREE.AdditiveBlending, vertexColors: true, sizeAttenuation: true
    })));
    galaxy.rotation.set(0.95, 0.4, 0.25);
    stars.add(galaxy);
    galaxy.visible = false;   // 已使用真实全景，隐藏程序化银河避免重影

    // 星云柔光
    const nebTex = (c1, c2) => {
      const cv = document.createElement('canvas'); cv.width = cv.height = 256;
      const x = cv.getContext('2d');
      const g = x.createRadialGradient(128, 128, 0, 128, 128, 128);
      g.addColorStop(0, c1); g.addColorStop(0.45, c2); g.addColorStop(1, 'rgba(0,0,0,0)');
      x.fillStyle = g; x.fillRect(0, 0, 256, 256);
      const t = new THREE.CanvasTexture(cv); t.colorSpace = THREE.SRGBColorSpace; return t;
    };
    [[-1400, 500, -900, 'rgba(140,110,255,0.55)', 'rgba(80,50,180,0.16)', 1500],
     [1250, -320, -1100, 'rgba(60,170,255,0.5)', 'rgba(30,90,200,0.14)', 1300],
     [300, 950, -1600, 'rgba(255,140,200,0.35)', 'rgba(140,40,120,0.1)', 1100]].forEach(([x, y, z, a, b, s]) => {
      const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: nebTex(a, b), transparent: true, blending: THREE.AdditiveBlending, depthWrite: false, opacity: 0.5 }));
      sp.position.set(x, y, z); sp.scale.set(s, s, 1); sp.visible = false; stars.add(sp);
    });

    // ---- 光照 ----
    scene.add(new THREE.AmbientLight(0x2a3350, 0.35));
    const sunLight = new THREE.PointLight(0xfff5e0, 3, 0, 0.5);
    scene.add(sunLight);

    // ---- 纹理加载器 (真实 NASA 贴图) ----
    const loader = new THREE.TextureLoader();
    const texPath = 'static/lib/three/planets/';
    const loadTex = name => { const t = loader.load(texPath + name); t.colorSpace = THREE.SRGBColorSpace; return t; };
    function makeTex(drawFn, w = 512, h = 256) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const ctx = c.getContext('2d'); drawFn(ctx, w, h);
      const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; return t;
    }
    function noise(ctx, w, h, base, spots) {
      ctx.fillStyle = base; ctx.fillRect(0, 0, w, h);
      for (let i = 0; i < spots; i++) {
        const x = Math.random() * w, y = Math.random() * h;
        const r = Math.random() * 30 + 5;
        const g = ctx.createRadialGradient(x, y, 0, x, y, r);
        g.addColorStop(0, 'rgba(255,255,255,0.15)'); g.addColorStop(1, 'rgba(255,255,255,0)');
        ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fill();
      }
    }

    // ---- 太阳 (真实贴图) ----
    const sun = new THREE.Mesh(new THREE.SphereGeometry(12, 48, 48),
      new THREE.MeshBasicMaterial({ map: loadTex('sun.jpg') }));
    scene.add(sun);
    // 太阳光晕
    const glowTex = makeTex((ctx, w, h) => {
      const g = ctx.createRadialGradient(w / 2, h / 2, 0, w / 2, h / 2, w / 2);
      g.addColorStop(0, 'rgba(255,230,150,0.85)');
      g.addColorStop(0.18, 'rgba(255,200,80,0.45)');
      g.addColorStop(0.45, 'rgba(255,140,40,0.16)');
      g.addColorStop(0.75, 'rgba(255,100,0,0.05)');
      g.addColorStop(1, 'rgba(255,100,0,0)');
      ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
    }, 256, 256);
    const glow = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowTex, transparent: true, blending: THREE.AdditiveBlending, depthWrite: false }));
    glow.scale.set(58, 58, 1); scene.add(glow);
    // 外层日冕 (更淡更大)
    const corona = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowTex, transparent: true, blending: THREE.AdditiveBlending, depthWrite: false, opacity: 0.35 }));
    corona.scale.set(130, 130, 1); scene.add(corona);

    // ---- FBM 分形噪声 (真实感纹理) ----
    function fbm(x, y, octaves = 4) {
      let v = 0, amp = 0.5, freq = 1;
      for (let o = 0; o < octaves; o++) {
        v += amp * Math.sin(x * freq) * Math.cos(y * freq * 1.3);
        freq *= 2; amp *= 0.5;
      }
      return v;
    }

    // ---- 行星 ----
    const planetObjs = [];
    PLANETS.forEach((p, i) => {
      const pivot = new THREE.Object3D(); scene.add(pivot);
      // 轨道线
      const orbitPts = [];
      for (let a = 0; a <= 128; a++) { const t = a / 128 * Math.PI * 2; orbitPts.push(new THREE.Vector3(Math.cos(t) * p.orbit, 0, Math.sin(t) * p.orbit)); }
      const orbitLine = new THREE.Line(new THREE.BufferGeometry().setFromPoints(orbitPts),
        new THREE.LineBasicMaterial({ color: 0x6c5ce7, transparent: true, opacity: 0.3 }));
      scene.add(orbitLine);
      // 倾角节点 (真实轴倾角)
      const tiltNode = new THREE.Object3D(); tiltNode.rotation.z = (p.tilt || 0) * Math.PI / 180; pivot.add(tiltNode);
      const spin = new THREE.Object3D(); tiltNode.add(spin);
      // 行星纹理 (真实 NASA 贴图)
      const baseSize = p.size * 0.4;
      const mesh = new THREE.Mesh(new THREE.SphereGeometry(baseSize, 48, 48),
        new THREE.MeshStandardMaterial({ map: loadTex(p.tex), roughness: 0.85, metalness: 0.05 }));
      spin.add(mesh);
      // 地球大气层 (蓝色辉光)
      if (p.name.includes('地球')) {
        const atmo = new THREE.Mesh(new THREE.SphereGeometry(baseSize * 1.15, 48, 48),
          new THREE.MeshBasicMaterial({ color: 0x4a90d9, transparent: true, opacity: 0.15, side: THREE.BackSide, blending: THREE.AdditiveBlending, depthWrite: false }));
        spin.add(atmo);
      }
      // 地球云层 (真实云图)
      if (p.cloud) {
        const clouds = new THREE.Mesh(new THREE.SphereGeometry(baseSize * 1.02, 48, 48),
          new THREE.MeshStandardMaterial({ map: loadTex(p.cloudTex), transparent: true, opacity: 0.65, depthWrite: false }));
        spin.add(clouds);
        mesh.userData.clouds = clouds;
      }
      // 月球 (绕地球公转)
      if (p.name.includes('地球')) {
        const moonOrbit = new THREE.Object3D();
        const moon = new THREE.Mesh(new THREE.SphereGeometry(baseSize * 0.27, 24, 24),
          new THREE.MeshStandardMaterial({ map: loadTex('moon.jpg'), roughness: 1, metalness: 0 }));
        moon.position.set(baseSize * 2.6, 0, 0);
        moonOrbit.add(moon);
        mesh.add(moonOrbit);
        mesh.userData.moonOrbit = moonOrbit;
      }
      // 土星环 (含卡西尼缝)
      if (p.ring) {
        const ringTex = makeTex((ctx, w, h) => {
          for (let x = 0; x < w; x++) {
            const t = x / w;
            let a = 0;
            if (t > 0.05 && t < 0.95) {
              a = 0.4 + Math.random() * 0.4;
              // 卡西尼缝
              if (t > 0.62 && t < 0.68) a *= 0.15;
              if (t < 0.12) a *= 0.4;
            }
            ctx.fillStyle = `rgba(${210 + Math.random() * 45},${185 + Math.random() * 40},${140 + Math.random() * 40},${a})`;
            ctx.fillRect(x, 0, 1, h);
          }
        }, 512, 64);
        const ring = new THREE.Mesh(new THREE.RingGeometry(baseSize * 1.4, baseSize * 2.3, 96),
          new THREE.MeshBasicMaterial({ map: ringTex, side: THREE.DoubleSide, transparent: true, opacity: 0.9 }));
        ring.rotation.x = Math.PI / 2; spin.add(ring);
        mesh.userData.ring = ring;
      }
      pivot.rotation.y = Math.random() * Math.PI * 2;
      mesh.position.set(p.orbit, 0, 0);
      planetObjs.push({ pivot, spin, mesh, baseSize, data: p });
    });

    // ---- 小行星带 (火星与木星之间) ----
    const beltPos = [], beltCol = [];
    for (let i = 0; i < 4000; i++) {
      const r = 206 + Math.random() * 26;
      const th = Math.random() * Math.PI * 2;
      beltPos.push(Math.cos(th) * r, (Math.random() - 0.5) * 4, Math.sin(th) * r);
      const g = 0.55 + Math.random() * 0.35;
      beltCol.push(g, g * 0.9, g * 0.75);
    }
    const beltGeo = new THREE.BufferGeometry();
    beltGeo.setAttribute('position', new THREE.Float32BufferAttribute(beltPos, 3));
    beltGeo.setAttribute('color', new THREE.Float32BufferAttribute(beltCol, 3));
    const belt = new THREE.Points(beltGeo, new THREE.PointsMaterial({
      size: 2.2, map: dotWhite, transparent: true, opacity: 0.85, depthWrite: false,
      blending: THREE.AdditiveBlending, vertexColors: true, sizeAttenuation: true
    }));
    scene.add(belt);

    // ---- 选中高亮环 (Sprite 恒定朝向相机) ----
    const selRingTex = makeTex((ctx, w, h) => {
      ctx.strokeStyle = '#a78bfa'; ctx.lineWidth = 6;
      ctx.beginPath(); ctx.arc(w / 2, h / 2, w / 2 - 6, 0, Math.PI * 2); ctx.stroke();
      ctx.strokeStyle = 'rgba(167,139,250,0.35)'; ctx.lineWidth = 14;
      ctx.beginPath(); ctx.arc(w / 2, h / 2, w / 2 - 12, 0, Math.PI * 2); ctx.stroke();
    }, 128, 128);
    const selRing = new THREE.Sprite(new THREE.SpriteMaterial({ map: selRingTex, transparent: true, depthWrite: false, depthTest: false }));
    selRing.visible = false; scene.add(selRing);

    // ---- 简易轨道控制器 (不依赖外部 OrbitControls) ----
    let azimuth = 0, polar = Math.PI / 3, dist = 200, target = new THREE.Vector3(0, 0, 0);
    let dragging = false, lastX = 0, lastY = 0, velA = 0, velP = 0;
    const updateCam = () => {
      camera.position.x = target.x + dist * Math.sin(polar) * Math.sin(azimuth);
      camera.position.y = target.y + dist * Math.cos(polar);
      camera.position.z = target.z + dist * Math.sin(polar) * Math.cos(azimuth);
      camera.lookAt(target);
    };
    updateCam();
    const onDown = e => { dragging = true; const p = e.touches ? e.touches[0] : e; lastX = p.clientX; lastY = p.clientY; velA = 0; velP = 0; container.style.cursor = 'grabbing'; };
    const onMove = e => {
      if (!dragging) return;
      const p = e.touches ? e.touches[0] : e;
      const dx = p.clientX - lastX, dy = p.clientY - lastY;
      lastX = p.clientX; lastY = p.clientY;
      azimuth -= dx * 0.005; polar -= dy * 0.005;
      polar = Math.max(0.1, Math.min(Math.PI - 0.1, polar));
      velA = -dx * 0.005; velP = -dy * 0.005;
      updateCam();
    };
    const onUp = () => {
      dragging = false; container.style.cursor = 'grab';
      const inertia = () => {
        if (Math.abs(velA) < 0.0005 && Math.abs(velP) < 0.0005) return;
        azimuth += velA; polar += velP;
        polar = Math.max(0.1, Math.min(Math.PI - 0.1, polar));
        velA *= 0.93; velP *= 0.93; updateCam();
        requestAnimationFrame(inertia);
      };
      requestAnimationFrame(inertia);
    };
    renderer.domElement.addEventListener('mousedown', onDown);
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    renderer.domElement.addEventListener('touchstart', onDown, { passive: true });
    renderer.domElement.addEventListener('touchmove', e => { onMove(e); e.preventDefault(); }, { passive: false });
    renderer.domElement.addEventListener('touchend', onUp);
    renderer.domElement.addEventListener('wheel', e => {
      e.preventDefault();
      dist = Math.max(30, Math.min(600, dist + e.deltaY * 0.2));
      updateCam();
    }, { passive: false });
    const controls = { update: () => {}, dispose: () => {} };

    // ---- 点击 / 悬停选行星 ----
    const raycaster = new THREE.Raycaster(); const mouse = new THREE.Vector2();
    const pickAt = e => {
      const rect = renderer.domElement.getBoundingClientRect();
      mouse.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
      mouse.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
      raycaster.setFromCamera(mouse, camera);
      const hits = raycaster.intersectObjects(planetObjs.map(o => o.mesh));
      return hits.length ? planetObjs.find(o => o.mesh === hits[0].object) : null;
    };
    let hovered = null;
    renderer.domElement.addEventListener('mousemove', e => {
      if (dragging) return;
      const obj = pickAt(e);
      if (obj !== hovered) {
        if (hovered) hovered.mesh.scale.setScalar(sizeMul);
        hovered = obj;
        if (hovered) hovered.mesh.scale.setScalar(sizeMul * 1.15);
        container.style.cursor = hovered ? 'pointer' : 'grab';
      }
    });
    renderer.domElement.addEventListener('click', e => {
      const obj = pickAt(e);
      if (obj) focusPlanet(obj);
    });

    // ---- 飞行探测 + 三向联动 ----
    let flying = false, flyTarget = null, flyFrom = new THREE.Vector3(), flyT = 0;
    let selectedObj = null, spotReqId = 0;

    function syncChips() {
      chips.forEach(c => c.classList.toggle('on', selectedObj && c.dataset.idx === String(planetObjs.indexOf(selectedObj))));
    }

    function focusPlanet(obj) {
      const p = obj.data;
      selectedObj = obj;
      syncChips();
      // 选中高亮环
      selRing.visible = true;
      selRing.scale.setScalar(obj.baseSize * 4.2);
      // 音乐随行星变调
      retuneMusic(p.note);
      // 信息卡
      $('#spInfo').innerHTML = `
        <div class="sp-info-card">
          <div class="sp-info-name"><i class="sp-dot" style="background:${p.color}"></i>${p.name}</div>
          <div class="sp-info-desc">${p.desc}</div>
          <div class="sp-info-meta">
            <div><span>直径</span><b>${p.diameter}</b></div>
            <div><span>距太阳</span><b>${p.dist}</b></div>
            <div><span>自转周期</span><b>${p.day}</b></div>
            <div><span>公转周期</span><b>${p.year}</b></div>
            <div><span>卫星数</span><b>${p.moons}</b></div>
            <div><span>表面温度</span><b>${p.temp}</b></div>
            <div><span>重力</span><b>${p.gravity}</b></div>
          </div>
          <div class="sp-actions">
            <button class="sp-ai" id="spAi">🤖 让小云讲讲</button>
            <button class="sp-back" id="spBack">🚀 返回</button>
          </div>
          <button class="sp-voice" id="spVoice">🔊 语音讲解</button>
          <div class="sp-spots-box" id="spSpotsBox">
            <div class="sp-spots-title">${p.spot.icon} 联动云游景点 · ${p.spot.theme}</div>
            <div class="sp-spots-loading">正在从景点库匹配…</div>
          </div>
        </div>`;

      // ① 联动 AI 助教：带着行星上下文跳到 AI 页并直接提问
      $('#spAi').onclick = () => {
        pendingChatPrompt = `我在「星际探索」里点到${p.name}，请用两三句话讲讲它的天文趣闻，再推荐一个地球上气质相近的云游中国目的地。`;
        switchTab('ai');
      };
      $('#spBack').onclick = () => flyBack();
      // 语音讲解
      $('#spVoice').onclick = () => {
        speak(`${p.name.split(' ')[0]}。${p.desc}直径${p.diameter}，距太阳${p.dist}，自转周期${p.day}，公转周期${p.year}，共有${p.moons}颗卫星。`);
      };

      // ② 联动云游景点：按行星气质拉取真实景点
      const reqId = ++spotReqId;
      const q = p.spot.province
        ? `/api/spots?size=3&page=1&sort=default&province=${encodeURIComponent(p.spot.province)}`
        : `/api/spots?size=3&page=1&sort=default`;
      API.get(q).then(res => {
        if (reqId !== spotReqId) return;
        const list = (res && res.list) || [];
        const box = $('#spSpotsBox');
        if (!box) return;
        if (!list.length) { box.innerHTML = `<div class="sp-spots-title">${p.spot.icon} 联动云游景点 · ${p.spot.theme}</div><div class="sp-spots-loading">暂无匹配景点</div>`; return; }
        box.innerHTML = `<div class="sp-spots-title">${p.spot.icon} 联动云游景点 · ${p.spot.theme}（${p.spot.province || '全国'}）</div>` +
          list.map(s => `<button class="sp-spot" data-spot="${s.id}">
            <span class="sp-spot-name">${esc(s.name)}</span>
            <span class="sp-spot-meta">📍${esc(s.province)} · ⭐${Number(s.rating || 0).toFixed(1)} · ${Number(s.price) > 0 ? '¥' + Number(s.price) : '免费'}</span>
          </button>`).join('');
        box.querySelectorAll('[data-spot]').forEach(b => b.onclick = () => openSpot(Number(b.dataset.spot)));
      }).catch(() => {
        if (reqId !== spotReqId) return;
        const box = $('#spSpotsBox');
        if (box) box.innerHTML = `<div class="sp-spots-title">${p.spot.icon} 联动云游景点 · ${p.spot.theme}</div><div class="sp-spots-loading">景点服务未连接</div>`;
      });

      // 启动飞行
      flyTarget = obj; flyFrom.copy(camera.position); flyT = 0; flying = true;
    }
    function flyBack() {
      stopSpeak();
      flyTarget = null; selectedObj = null; spotReqId++; syncChips();
      selRing.visible = false;
      flyFrom.copy(camera.position); flyT = 0; flying = true;
      retuneMusic(261.63);
      $('#spInfo').innerHTML = `<div class="sp-info-tip">👆 点击行星或下方按钮探索宇宙奥秘</div>`;
    }

    // 导航条 ↔ 3D 场景联动
    chips.forEach(c => c.onclick = () => {
      const obj = planetObjs[Number(c.dataset.idx)];
      if (obj) focusPlanet(obj);
    });

    $('#spReset').onclick = () => { flyBack(); azimuth = 0; polar = Math.PI / 3; dist = 200; velA = 0; velP = 0; updateCam(); };

    // ---- 控制面板: 速度 / 亮度 / 大小 ----
    let speedMul = 1, brightMul = 1, sizeMul = 1;
    $('#spSpeed').oninput = e => { speedMul = parseFloat(e.target.value); $('#spSpeedVal').textContent = speedMul.toFixed(1) + 'x'; };
    $('#spBright').oninput = e => { brightMul = parseFloat(e.target.value); $('#spBrightVal').textContent = brightMul.toFixed(1); sunLight.intensity = 3 * brightMul; glow.material.opacity = 0.6 * brightMul; };
    $('#spSize').oninput = e => {
      sizeMul = parseFloat(e.target.value); $('#spSizeVal').textContent = sizeMul.toFixed(1) + 'x';
      planetObjs.forEach(o => {
        o.mesh.scale.setScalar(sizeMul);
        if (o.mesh.userData.clouds) o.mesh.userData.clouds.scale.setScalar(sizeMul);
        if (o.mesh.userData.ring) o.mesh.userData.ring.scale.setScalar(sizeMul);
      });
    };

    // ---- 太空氛围音乐 (Web Audio API 实时生成, 随行星变调) ----
    let spaceCtx = null, spaceNodes = [], spaceTimer = null;
    let musicRoot = 261.63;
    const padOscs = [];      // { osc, ratio } 随行星根音变调
    function startSpaceMusic() {
      spaceCtx = new (window.AudioContext || window.webkitAudioContext)();
      const master = spaceCtx.createGain(); master.gain.value = 0.0; master.connect(spaceCtx.destination);
      master.gain.linearRampToValueAtTime(0.25, spaceCtx.currentTime + 2);
      // 低频 drone (深空, 跟随根音低八度)
      [0.5, 0.75, 1].forEach((ratio, i) => {
        const o = spaceCtx.createOscillator(); o.type = 'sine'; o.frequency.value = musicRoot * ratio;
        const g = spaceCtx.createGain(); g.gain.value = 0.15 - i * 0.04;
        const lp = spaceCtx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 200;
        const lfo = spaceCtx.createOscillator(); lfo.frequency.value = 0.05 + i * 0.02;
        const lfoGain = spaceCtx.createGain(); lfoGain.gain.value = 0.03;
        lfo.connect(lfoGain); lfoGain.connect(g.gain);
        o.connect(lp); lp.connect(g); g.connect(master);
        o.start(); lfo.start();
        spaceNodes.push(o, lfo);
        padOscs.push({ osc: o, ratio });
      });
      // 漂浮 pad 和弦 (大三和弦, 随根音变调)
      [1, 1.25, 1.5, 2].forEach((ratio, i) => {
        const o = spaceCtx.createOscillator(); o.type = 'triangle'; o.frequency.value = musicRoot * ratio;
        const g = spaceCtx.createGain(); g.gain.value = 0;
        const lp = spaceCtx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 800;
        const lfo = spaceCtx.createOscillator(); lfo.frequency.value = 0.08 + i * 0.03;
        const lfoGain = spaceCtx.createGain(); lfoGain.gain.value = 0.06;
        lfo.connect(lfoGain); lfoGain.connect(g.gain);
        o.connect(lp); lp.connect(g); g.connect(master);
        o.start(); lfo.start();
        spaceNodes.push(o, lfo);
        padOscs.push({ osc: o, ratio });
      });
      // 星光闪烁音
      const twinkle = () => {
        if (!spaceCtx) return;
        const o = spaceCtx.createOscillator(); o.type = 'sine';
        o.frequency.value = 1200 + Math.random() * 1800;
        const g = spaceCtx.createGain(); g.gain.value = 0;
        o.connect(g); g.connect(master);
        const t = spaceCtx.currentTime;
        g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(0.03, t + 0.02); g.gain.exponentialRampToValueAtTime(0.001, t + 0.8);
        o.start(t); o.stop(t + 0.8);
        spaceTimer = setTimeout(twinkle, 1500 + Math.random() * 2500);
      };
      spaceTimer = setTimeout(twinkle, 1000);
    }
    function stopSpaceMusic() {
      if (spaceCtx) { spaceCtx.close(); spaceCtx = null; }
      clearTimeout(spaceTimer); spaceNodes = []; padOscs.length = 0;
    }
    // 行星 ↔ 音乐联动: 切到行星时整段和声平滑变调
    function retuneMusic(root) {
      musicRoot = root;
      if (!spaceCtx) return;
      const t = spaceCtx.currentTime;
      padOscs.forEach(p => p.osc.frequency.cancelScheduledValues(t));
      padOscs.forEach(p => p.osc.frequency.linearRampToValueAtTime(root * p.ratio, t + 0.9));
    }
    let bgmOn = false;
    $('#spMusic').onclick = () => {
      bgmOn = !bgmOn;
      if (bgmOn) { startSpaceMusic(); $('#spMusic').textContent = '🔊 星海乐章'; $('#spMusic').classList.add('on'); }
      else { stopSpaceMusic(); $('#spMusic').textContent = '🎵 背景音乐'; $('#spMusic').classList.remove('on'); }
    };

    // ---- 动画循环 ----
    let raf;
    const clock = new THREE.Clock();
    const defaultCam = new THREE.Vector3(0, 80, 200);
    const animate = () => {
      raf = requestAnimationFrame(animate);
      const t = clock.getElapsedTime();
      sun.rotation.y = t * 0.1;
      stars.rotation.y = t * 0.004;
      sky.rotation.y = Math.PI * 0.6 + t * 0.0012;
      glow.material.opacity = 0.85 + Math.sin(t * 1.3) * 0.1;
      corona.material.opacity = 0.3 + Math.sin(t * 0.8 + 1) * 0.08;
      planetObjs.forEach(o => {
        o.pivot.rotation.y += (1 / o.data.speed) * 0.02 * speedMul;
        o.spin.rotation.y += 0.01 * speedMul;
        if (o.mesh.userData.clouds) o.mesh.userData.clouds.rotation.y += 0.013 * speedMul;
        if (o.mesh.userData.moonOrbit) o.mesh.userData.moonOrbit.rotation.y += 0.025 * speedMul;
      });
      belt.rotation.y += 0.0025 * speedMul;
      // 选中环跟随行星
      if (selectedObj) {
        const w = new THREE.Vector3(); selectedObj.mesh.getWorldPosition(w);
        selRing.position.copy(w);
        selRing.scale.setScalar(selectedObj.baseSize * sizeMul * 4.2);
        selRing.material.opacity = 0.55 + Math.sin(t * 3) * 0.3;
      }
      // 飞行探测插值
      if (flying) {
        flyT += 0.02;
        const e = 1 - Math.pow(1 - Math.min(flyT, 1), 3); // easeOutCubic
        if (flyTarget) {
          const tp = new THREE.Vector3(); flyTarget.mesh.getWorldPosition(tp);
          const dest = tp.clone().add(new THREE.Vector3(0, flyTarget.baseSize * 2.5, flyTarget.baseSize * 5));
          camera.position.lerpVectors(flyFrom, dest, e);
          camera.lookAt(tp);
          target.copy(tp);
        } else {
          camera.position.lerpVectors(flyFrom, defaultCam, e);
          camera.lookAt(0, 0, 0);
          target.set(0, 0, 0);
          dist = 200; azimuth = 0; polar = Math.PI / 3;
        }
        if (flyT >= 1) flying = false;
      } else {
        updateCam();
      }
      renderer.render(scene, camera);
    };
    animate();

    // ---- 清理函数 ----
    spaceCleanup = () => {
      cancelAnimationFrame(raf);
      stopSpaceMusic();
      controls.dispose();
      renderer.dispose();
      scene.traverse(obj => { if (obj.geometry) obj.geometry.dispose(); if (obj.material) { if (Array.isArray(obj.material)) obj.material.forEach(m => m.dispose()); else obj.material.dispose(); } });
      if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement);
    };
  }
  function shadeColor(hex, percent) {
    const n = parseInt(hex.slice(1), 16);
    let r = (n >> 16) + percent, g = ((n >> 8) & 0xff) + percent, b = (n & 0xff) + percent;
    r = Math.max(0, Math.min(255, r)); g = Math.max(0, Math.min(255, g)); b = Math.max(0, Math.min(255, b));
    return `rgb(${r},${g},${b})`;
  }

  function render() {
    stopSpeak();
    switch (state.tab) {
      case 'home': renderHome(); break;
      case 'planner': renderPlanner(); break;
      case 'explore': renderExplore(); break;
      case 'recommend': renderRecommend(); break;
      case 'tour': renderTour(); break;
      case 'map': renderMap(); break;
      case 'ai': renderAi(); break;
      case 'space': renderSpace(); break;
      case 'orders': renderOrders(); break;
      case 'me': renderMe(); break;
      default: renderHome();
    }
  }

  /* ---------------- 启动 ---------------- */
  window.openSpot = openSpot;   // 暴露到全局, 供地图 popup onclick 调用
  loadMe().then(render);
  if ('speechSynthesis' in window) { window.speechSynthesis.onvoiceschanged = () => {}; }
})();
