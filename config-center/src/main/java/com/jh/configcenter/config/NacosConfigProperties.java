package com.jh.configcenter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Nacos Config Server 连接参数。
 *
 * <p>用于通过 Nacos 原生 ConfigService API 读写配置项。</p>
 *
 * @author jinhang
 * @since 2026/8/12
 */
@Component
@ConfigurationProperties(prefix = "spring.cloud.nacos")
public class NacosConfigProperties {

  /** Nacos Server 地址，如 127.0.0.1:8848 */
  private String serverAddr = "127.0.0.1:8848";

  private String username = "nacos";

  private String password = "nacos";

  public String getServerAddr() {
    return serverAddr;
  }

  public void setServerAddr(String serverAddr) {
    this.serverAddr = serverAddr;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
