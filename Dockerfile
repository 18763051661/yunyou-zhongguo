# 云游中国 · 星际探索 — 全国景点智能导览与星际科普平台
# 运行环境: Tomcat 11 + JDK 17, WAR 部署为 ROOT(根上下文)
FROM tomcat:11.0-jdk17-temurin

ENV TZ=Asia/Shanghai \
    PORT=8080 \
    DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=scenic_db \
    DB_USER=scenic \
    DB_PASSWORD=scenic123 \
    JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -Dfile.encoding=UTF-8"

# 官方镜像自带示例应用, 清空后只保留本项目
RUN rm -rf /usr/local/tomcat/webapps/*

COPY target/untitled8.war /usr/local/tomcat/webapps/ROOT.war

# 启动脚本: 同时监听 PORT(默认 8080) 与另一个常用端口, 兼容云平台端口配置差异
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/docker-entrypoint.sh \
 && chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080 3000

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
