#!/bin/sh
# 云游中国 启动脚本
# 1) 同时监听 PORT(默认 8080) 与另一个常用端口, 兼容云平台端口配置差异
# 2) 任何一步失败都只告警不退出, 避免容器被判定为启动失败而反复重启
PORT="${PORT:-8080}"
if [ "$PORT" = "8080" ]; then
  SECOND_PORT="${SECOND_PORT:-3000}"
else
  SECOND_PORT="8080"
fi

CONF="$CATALINA_HOME/conf/server.xml"

log() { echo "[entrypoint] $*"; }

log "PORT=$PORT SECOND_PORT=$SECOND_PORT"
log "JAVA_OPTS=$JAVA_OPTS"
log "DB_HOST=$DB_HOST DB_PORT=$DB_PORT DB_NAME=$DB_NAME DB_USER=$DB_USER"
log "server.xml=$CONF writable=$([ -w "$CONF" ] && echo yes || echo no)"

if [ -f "$CONF" ] && [ -w "$CONF" ]; then
  sed -i "s/port=\"8080\"/port=\"$PORT\"/" "$CONF" || log "WARN 设置主端口失败, 继续使用默认值"
  if [ "$SECOND_PORT" != "$PORT" ]; then
    sed -i "s#</Service>#<Connector port=\"$SECOND_PORT\" protocol=\"HTTP/1.1\" connectionTimeout=\"20000\" redirectPort=\"8443\"/>\n</Service>#" "$CONF" \
      || log "WARN 追加备用端口失败"
  fi
else
  log "WARN server.xml 不可写, 使用默认 8080"
fi

log "starting catalina..."
exec catalina.sh run
