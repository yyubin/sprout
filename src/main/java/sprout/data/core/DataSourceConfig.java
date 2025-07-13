package sprout.data.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import sprout.beans.annotation.Bean;
import sprout.beans.annotation.Configuration;
import sprout.config.AppConfig;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = true)
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(AppConfig appConfig) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(appConfig.getStringProperty("sprout.database.url", "jdbc:mysql://localhost:3306/sprout"));
        hikariConfig.setUsername(appConfig.getStringProperty("sprout.database.username", "sprout"));
        hikariConfig.setPassword(appConfig.getStringProperty("sprout.database.password", "tygh8868!"));
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setConnectionTimeout(30000);
        return new HikariDataSource(hikariConfig);
    }

}
