/* 统一请求层: 解包 {code,msg,data}, 401 广播事件, 失败统一抛错
 * BASE 自动识别 context path (兼容根路径 / 与 Tomcat /xx_war_exploded)
 */
(function () {
  // 取当前路径去掉末尾 / 和文件名, 得到 context path (可能为空字符串 "")
  const _ctx = (() => {
    const p = window.location.pathname;
    const i = p.lastIndexOf('/');
    return (i <= 0 ? '' : p.slice(0, i)).replace(/\/$/, '');
  })();

  async function request(method, url, body) {
    // url 可能是 "/api/stats" 这种以 / 开头的绝对路径, 去掉前面的 / 拼 ctx
    const safe = url.startsWith('/') ? url.slice(1) : url;
    const fullUrl = _ctx + '/' + safe;
    let resp;
    try {
      resp = await fetch(fullUrl, {
        method,
        headers: body !== undefined ? { 'Content-Type': 'application/json;charset=UTF-8' } : undefined,
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
    } catch (networkErr) {
      throw new Error('网络连接失败，请确认服务已启动');
    }
    let json;
    try {
      json = await resp.json();
    } catch (e) {
      throw new Error('服务响应格式异常');
    }
    if (json.code === 401) {
      window.dispatchEvent(new CustomEvent('app:unauthorized', { detail: json.msg }));
      throw new Error(json.msg || '请先登录');
    }
    if (json.code !== 0) {
      throw new Error(json.msg || '请求失败');
    }
    return json.data;
  }

  window.API = {
    get: (url) => request('GET', url),
    post: (url, body) => request('POST', url, body ?? {}),
  };
})();
