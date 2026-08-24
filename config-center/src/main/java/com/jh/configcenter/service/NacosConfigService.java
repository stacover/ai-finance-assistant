package com.jh.configcenter.service;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.jh.configcenter.config.NacosConfigProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Nacos 配置读写服务。
 *
 * <p>封装 Nacos 原生 ConfigService，提供配置的发布、查询、删除与监听能力，
 * 供配置中心门户接口调用。</p>
 *
 * @author jinhang
 * @since 2026/8/12
 */
@Service
public class NacosConfigService {

  private static final Logger log = LoggerFactory.getLogger(NacosConfigService.class);

  /** 默认配置文件格式 */
  public static final String DEFAULT_GROUP = "DEFAULT_GROUP";

  private ConfigService configService;
  private final NacosConfigProperties properties;

  public NacosConfigService(NacosConfigProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  public void init() {
    Properties props = new Properties();
    props.setProperty("serverAddr", properties.getServerAddr());
    props.setProperty("username", properties.getUsername());
    props.setProperty("password", properties.getPassword());
    try {
      configService = NacosFactory.createConfigService(props);
      log.info("Nacos ConfigService 初始化成功，serverAddr={}", properties.getServerAddr());
    } catch (NacosException e) {
      throw new IllegalStateException("Nacos ConfigService 初始化失败", e);
    }
  }

  /** 读取配置。 */
  public String getConfig(String dataId, String group) {
    try {
      return configService.getConfig(dataId, group, 5000);
    } catch (NacosException e) {
      log.error("读取配置失败 dataId={}, group={}", dataId, group, e);
      throw new RuntimeException("读取配置失败: " + e.getErrMsg(), e);
    }
  }

  /** 发布或更新配置。 */
  public boolean publishConfig(String dataId, String group, String content) {
    try {
      boolean ok = configService.publishConfig(dataId, group, content);
      log.info("发布配置 {} / {} -> {}", group, dataId, ok);
      return ok;
    } catch (NacosException e) {
      log.error("发布配置失败 dataId={}, group={}", dataId, group, e);
      throw new RuntimeException("发布配置失败: " + e.getErrMsg(), e);
    }
  }

  /** 删除配置。 */
  public boolean removeConfig(String dataId, String group) {
    try {
      boolean ok = configService.removeConfig(dataId, group);
      log.info("删除配置 {} / {} -> {}", group, dataId, ok);
      return ok;
    } catch (NacosException e) {
      log.error("删除配置失败 dataId={}, group={}", dataId, group, e);
      throw new RuntimeException("删除配置失败: " + e.getErrMsg(), e);
    }
  }

  @PreDestroy
  public void destroy() {
    if (configService != null) {
      try {
        configService.shutDown();
      } catch (NacosException e) {
        log.warn("关闭 Nacos ConfigService 异常", e);
      }
    }
  }
}
