#!/bin/sh
# 启动脚本: 让 Tomcat 同时监听两个端口
# 云平台(Sealos/K8s)的「容器端口」与 PORT 环境变量不一致时, 单端口会 404/502,
# 这里固定额外监听另一个常用端口, 两种配置都能访问。
set -e

PORT="${PORT:-8080}"
if [ "$PORT" = "8080" ]; then
  SECOND_PORT="${SECOND_PORT:-3000}"
else
  SECOND_PORT="8080"
fi

CONF="$CATALINA_HOME/conf/server.xml"

# 主端口: 把 server.xml 默认的 8080 换成 PORT
sed -i "s/port=\"8080\"/port=\"$PORT\"/" "$CONF"

# 备用端口: 在 </Service> 前追加一个 Connector
if [ "$SECOND_PORT" != "$PORT" ]; then
  sed -i "s#</Service>#<Connector port=\"$SECOND_PORT\" protocol=\"HTTP/1.1\" connectionTimeout=\"20000\" redirectPort=\"8443\"/>\n</Service>#" "$CONF"
fi

echo "[entrypoint] Tomcat listening on $PORT and $SECOND_PORT"

exec catalina.sh run
