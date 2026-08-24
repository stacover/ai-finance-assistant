package com.jh.configcenter.controller;

import com.jh.configcenter.dto.ConfigPublishRequest;
import com.jh.configcenter.service.NacosConfigService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置中心管理接口。
 *
 * <p>通过本接口可对 Nacos Config Server 上的配置进行查询、发布与删除，
 * 从而统一管理 DEEPSEEK_API_KEY、MYSQL_PASSWORD 等敏感配置。</p>
 *
 * @author jinhang
 * @since 2026/8/12
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

  private final NacosConfigService nacosConfigService;

  public ConfigController(NacosConfigService nacosConfigService) {
    this.nacosConfigService = nacosConfigService;
  }

  /** 查询配置。 */
  @GetMapping
  public ResponseEntity<Map<String, Object>> get(
      @RequestParam String dataId,
      @RequestParam(defaultValue = "DEFAULT_GROUP") String group) {
    String content = nacosConfigService.getConfig(dataId, group);
    return ResponseEntity.ok(
        Map.of("dataId", dataId, "group", group, "content", content == null ? "" : content));
  }

  /** 发布或更新配置。 */
  @PostMapping
  public ResponseEntity<Map<String, Object>> publish(@Valid @RequestBody ConfigPublishRequest req) {
    boolean ok = nacosConfigService.publishConfig(req.getDataId(), req.getGroup(), req.getContent());
    return ResponseEntity.ok(
        Map.of("success", ok, "dataId", req.getDataId(), "group", req.getGroup()));
  }

  /** 删除配置。 */
  @DeleteMapping
  public ResponseEntity<Map<String, Object>> remove(
      @RequestParam String dataId,
      @RequestParam(defaultValue = "DEFAULT_GROUP") String group) {
    boolean ok = nacosConfigService.removeConfig(dataId, group);
    return ResponseEntity.ok(Map.of("success", ok, "dataId", dataId, "group", group));
  }
}
