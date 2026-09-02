# 配置中心（Nacos）使用说明

本项目采用 **Nacos Config Server** 作为统一配置中心，集中管理 `DEEPSEEK_API_KEY`、`MYSQL_PASSWORD` 等敏感配置，并支持动态刷新。

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `config-center` | 配置中心门户服务，通过 HTTP 接口读写 Nacos 配置 |
| `ai-model-gateway` | AI 模型网关，从 Nacos 拉取 `spring.ai.openai.*` 等配置 |
| `nacos-config/` | 需导入 Nacos 的配置文件模板 |

## 版本对应关系

- Spring Boot：`3.5.14`
- Spring Cloud：`2025.0.0`
- Spring Cloud Alibaba：`2025.0.0.0`
- Nacos Server：`2.x`

## 一、启动 Nacos Server

方式一：Docker（推荐）

```bash
docker run -d --name nacos \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=true \
  -p 8848:8848 \
  -p 9848:9848 \
  nacos/nacos-server:v2.3.2
```

方式二：下载二进制包解压启动

```bash
# 下载 nacos-server-2.3.2.zip 并解压后：
cd nacos/bin
# Linux/Mac
sh startup.sh -m standalone
# Windows
startup.cmd -m standalone
```

访问控制台：`http://127.0.0.1:8848/nacos`（默认账号密码 `nacos/nacos`）

> 建议在 Nacos 控制台「权限控制-用户」中修改默认密码。

## 二、导入配置到 Nacos

1. 登录 Nacos 控制台，进入「配置管理-配置列表」。
2. 点击「导入」按钮，选择本目录下的 `ai-model-gateway.yaml`。
3. 确认 DataId 为 `ai-model-gateway.yaml`，Group 为 `DEFAULT_GROUP`，格式为 YAML。
4. 在配置列表中打开该配置，将 `api-key` 和 `password` 替换为你的真实值（也可改用环境变量占位）。

也可以在「配置列表」中直接「新建配置」：

- DataId：`ai-model-gateway.yaml`
- Group：`DEFAULT_GROUP`
- 配置格式：`YAML`
- 配置内容：参考 `nacos-config/ai-model-gateway.yaml`

## 三、启动各服务

### 3.1 启动 config-center

```bash
cd config-center
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8002"
```

验证：`GET http://127.0.0.1:8002/api/config?dataId=ai-model-gateway.yaml`

### 3.2 启动 ai-model-gateway

```bash
cd ai-model-gateway
mvn spring-boot:run
```

启动成功后，日志中会看到 Nacos Config 加载成功。此时**无需再设置本机 `DEEPSEEK_API_KEY` 环境变量**，配置已由 Nacos 下发。

## 四、配置热刷新

当 Nacos 上的配置变更后，触发刷新（默认自动刷新开启）：

```bash
curl -X POST http://127.0.0.1:8080/actuator/refresh
```

`@RefreshScope` 标注的 `AiModelDynamicConfig` 会自动获取最新值。

## 五、通过 config-center 管理配置（可选）

发布 / 更新配置：

```bash
curl -X POST http://127.0.0.1:8002/api/config \
  -H "Content-Type: application/json" \
  -d '{"dataId":"ai-model-gateway.yaml","group":"DEFAULT_GROUP","content":"spring:\n  ai:\n    openai:\n      api-key: sk-xxx"}'
```

查询配置：

```bash
curl "http://127.0.0.1:8002/api/config?dataId=ai-model-gateway.yaml"
```

删除配置：

```bash
curl -X DELETE "http://127.0.0.1:8002/api/config?dataId=ai-model-gateway.yaml"
```

## 六、环境变量说明

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `NACOS_SERVER_ADDR` | Nacos Server 地址 | `127.0.0.1:8848` |
| `NACOS_USERNAME` | Nacos 用户名 | `nacos` |
| `NACOS_PASSWORD` | Nacos 密码 | `nacos` |
| `NACOS_NAMESPACE` | Nacos 命名空间 | 空（public） |
| `SPRING_PROFILES_ACTIVE` | 激活 profile | `dev` |
| `SERVER_PORT` | ai-model-gateway 端口 | `8080` |
| `CONFIG_CENTER_PORT` | config-center 端口 | `8002` |

> 这些变量描述的是连接信息，可安全放在各服务的本地配置中；真正的敏感配置（API Key、密码）存放在 Nacos。
