package fr.dossierfacile.api.front.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
/*
    @Autowired
    private Environment env;


    //@Bean(name = "hackathonDataSource")
    public DataSource dataSource() {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("hackathon.datasource.driver-class-name"));
        dataSource.setUrl(env.getProperty("hackathon.datasource.jdbc-url"));
        dataSource.setUsername(env.getProperty("hackathon.datasource.username"));
        dataSource.setPassword(env.getProperty("hackathon.datasource.password"));

        return dataSource;
    }


/*
    @Bean(name = "customDataSource")
    @ConfigurationProperties(prefix = "hackathon.datasource")
    public DataSource customDataSource() {
        return DataSourceBuilder.create().build();
    }

 */
}