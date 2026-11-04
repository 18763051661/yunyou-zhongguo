# 云游中国 · 全国景点智能导览推荐系统
# 运行环境: Tomcat 11 + JDK 17, WAR 部署为 ROOT(根上下文)
FROM tomcat:11.0-jdk17-temurin

ENV TZ=Asia/Shanghai \
    DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=scenic_db \
    DB_USER=scenic \
    DB_PASSWORD=scenic123

# 官方镜像自带示例应用, 清空后只保留本项目
RUN rm -rf /usr/local/tomcat/webapps/*

COPY target/untitled8.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
