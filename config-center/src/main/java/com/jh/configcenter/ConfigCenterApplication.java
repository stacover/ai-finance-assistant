package com.jh.configcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 配置中心服务启动类。
 *
 * <p>负责统一管理项目内的配置项（如 DEEPSEEK_API_KEY、MYSQL_PASSWORD 等），
 * 通过 Nacos Config Server 存储并对外提供配置查询、刷新等能力。</p>
 *
 * @author jinhang
 * @since 2026/8/12
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ConfigCenterApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigCenterApplication.class, args);
  }
}
