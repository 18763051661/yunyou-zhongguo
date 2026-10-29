package cn.edu.qut.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Spring 根容器配置: 真实 MySQL 8 数据源(Druid 连接池)、MyBatis、事务。
 * 数据库: localhost:3306/scenic_db (utf8mb4)。
 * 首次启动(表不存在或景点数为 0)时自动执行 schema.sql 建表与 data.sql 全国景点种子数据;
 * 之后重启不覆盖用户注册、评论、收藏、打卡等真实数据。
 */
@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@MapperScan("cn.edu.qut.mapper")
@ComponentScan(basePackages = {"cn.edu.qut.service", "cn.edu.qut.support", "cn.edu.qut.dao", "cn.edu.qut.aop", "cn.edu.qut.pay"})
@PropertySource("classpath:alipay.properties")
public class AppConfig {

    // ========== 支付宝沙箱配置 (从 alipay.properties 读取) ==========
    @Value("${alipay.appId:}")
    public String alipayAppId;
    @Value("${alipay.gatewayUrl:https://openapi-sandbox.dl.alipaydev.com}")
    public String alipayGatewayUrl;
    @Value("${alipay.appPrivateKey:}")
    public String alipayAppPrivateKey;
    @Value("${alipay.alipayPublicKey:}")
    public String alipayPublicKey;
    @Value("${alipay.signType:RSA2}")
    public String alipaySignType;
    @Value("${alipay.charset:UTF-8}")
    public String alipayCharset;
    @Value("${alipay.format:JSON}")
    public String alipayFormat;
    @Value("${alipay.returnUrl:/api/alipay/return}")
    public String alipayReturnUrl;
    @Value("${alipay.notifyUrl:}")
    public String alipayNotifyUrl;

    @Bean(initMethod = "init", destroyMethod = "close")
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:3306/scenic_db?useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");
        ds.setUsername("scenic");
        ds.setPassword("scenic123");
        ds.setInitialSize(2);
        ds.setMinIdle(2);
        ds.setMaxActive(16);
        ds.setMaxWait(10000);
        ds.setValidationQuery("select 1");
        ds.setTestWhileIdle(true);
        ds.setTimeBetweenEvictionRunsMillis(60000);
        return ds;
    }

    /**
     * 幂等播种: 仅在空库时执行建表与全国景点数据脚本。
     */
    @Bean
    public InitializingBean databaseBootstrap(DataSource dataSource) {
        return () -> {
            try (Connection conn = dataSource.getConnection()) {
                // schema.sql 每次都执行 (全部用 CREATE TABLE IF NOT EXISTS, 幂等安全)
                executeScript(conn, "schema.sql");
                // data.sql 仅在 scenic_spot 为空时执行
                boolean empty = true;
                try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "scenic_spot", null)) {
                    if (rs.next()) {
                        try (PreparedStatement ps = conn.prepareStatement("select count(*) from scenic_spot");
                             ResultSet crs = ps.executeQuery()) {
                            crs.next();
                            empty = crs.getLong(1) == 0;
                        }
                    }
                }
                if (empty) {
                    executeScript(conn, "data.sql");
                }
            }
        };
    }

    private void executeScript(Connection conn, String classpath) {
        ScriptUtils.executeSqlScript(conn,
                new EncodedResource(new ClassPathResource(classpath), StandardCharsets.UTF_8),
                false, false, "--", ";", "/*", "*/");
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        // 下划线列名自动映射驼峰属性
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
