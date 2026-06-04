# AI 智能家居管理系统项目说明及安装说明

本文档可作为 GitHub `README.md` 使用，也可作为毕业设计提交材料中的“项目说明、安装配置说明、使用说明”。按照本文档完成环境准备、配置文件、数据库初始化和启动步骤后，可以运行前端、后端和硬件端，完成智能家居系统的演示。

## 1. 项目简介

AI 智能家居管理系统是一个前端、后端、硬件联动的智能家居毕业设计项目。系统支持用户登录注册、家庭管理、设备注册、设备控制、AI 助手自然语言控制、温湿度采集、天气查询、空调温度推荐、人体存在检测和离家自动化等功能。

项目由三部分组成：

| 模块 | 路径 | 说明 |
|---|---|---|
| 后端服务 | `/Users/cms/Documents/code/javaproject/smart-home-agent` | Spring Boot 后端，提供 REST API、AI 助手、MQTT 通信、MySQL/Redis 数据管理 |
| 前端网页 | `/Users/cms/Documents/code/javaproject/aihome-web` | Vue 3 + Vite 前端，提供登录、首页控制台、AI 助手、图表、个人中心 |
| 硬件固件 | `/Users/cms/Documents/code/javaproject/AI-HOME/aihome_hardware` | PlatformIO + Arduino ESP32 固件，包含执行节点和人体感知节点 |

## 2. 系统功能

### 2.1 用户与家庭管理

- 用户注册、登录、修改密码。
- 创建家庭、加入家庭、离开家庭。
- 支持一个用户管理多个家庭。
- 家庭信息包含城市、省份、行政区编码、备注等。

### 2.2 设备管理

- 注册设备：灯、门、空调、红外传感器、PIR、毫米波等。
- 设备绑定 `mqttTopic`，后端根据 Topic 精准下发或解析硬件消息。
- 支持查询家庭下设备列表、设备数量和删除设备。

### 2.3 智能控制

- 前端按钮控制设备。
- AI 助手支持自然语言控制，例如“打开客厅灯”“把空调调到 26 度”“今天天气怎么样”。
- 后端通过 Spring AI 将空调、灯、门、天气等能力注册为工具函数。

### 2.4 天气与位置能力

- 使用高德地图 API 获取用户 IP 所在城市、行政区编码。
- 使用和风天气 API 获取实时天气和室外温度。
- 空调控制时结合室外温度、室内 DHT11 温度和用户历史习惯，生成推荐温度。

### 2.5 硬件与传感器

- 板 A：执行节点，包含 LED、DHT11、SG90 舵机、按键。
- 板 B：人体感知节点，包含双路红外对射、PIR、LD2410 毫米波 OUT。
- 硬件通过 MQTT 与后端通信。

## 3. 技术栈

### 3.1 后端

- Java 21
- Spring Boot 3.3.4
- Spring AI 1.0.0-M1
- MyBatis-Plus 3.5.7
- MySQL 8.x
- Redis 6.x/7.x
- Eclipse Paho MQTT Client
- EMQX 公共 MQTT Broker：`broker.emqx.io:1883`

### 3.2 前端

- Node.js 18+，建议 20+
- Vue 3
- Vite
- Vue Router
- Axios
- ECharts
- Tailwind CSS

### 3.3 硬件

- ESP32 DevKit
- PlatformIO
- Arduino Framework
- PubSubClient
- ArduinoJson
- DHT sensor library
- ESP32Servo
- Bounce2

## 4. 运行环境说明

建议运行环境如下：

| 环境 | 推荐版本 | 说明 |
|---|---:|---|
| JDK | 21 | 后端项目 `pom.xml` 中配置为 Java 21 |
| Maven | 3.9+ | 可使用项目自带 `./mvnw` |
| MySQL | 8.0+ | 存储用户、家庭、设备、聊天记录、习惯数据 |
| Redis | 6.0+ | 存储 AI 短期对话窗口、人体存在状态、红外待定状态 |
| Node.js | 18+ | 前端运行环境，建议 Node 20 |
| npm | 9+ | 安装前端依赖 |
| PlatformIO | 最新稳定版 | 烧录 ESP32 固件 |
| MQTT 工具 | MQTTX 可选 | 没有硬件时可模拟 MQTT 消息 |
| API Key | 高德、和风天气、OpenAI 兼容模型 | AI 和外部 API 功能需要 |

## 5. 安装配置说明

### 5.1 克隆或准备代码

如果是在本机已有代码，目录如下：

```bash
/Users/cms/Documents/code/javaproject/
├── smart-home-agent              # 后端
├── aihome-web                    # 前端
└── AI-HOME/aihome_hardware       # 硬件
```

如果上传到 GitHub，建议仓库结构保持一致：

```bash
AI-HOME/
├── backend/smart-home-agent
├── frontend/aihome-web
└── hardware/aihome_hardware
```

## 6. 后端安装使用说明

### 6.1 进入后端目录

```bash
cd /Users/cms/Documents/code/javaproject/smart-home-agent
```

### 6.2 准备 MySQL 数据库

创建数据库：

```sql
CREATE DATABASE aihome DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

建议初始化以下表。字段根据项目实体和 Mapper 整理：

```sql
USE aihome;

CREATE TABLE `user` (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_name VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL
);

CREATE TABLE family (
  id INT PRIMARY KEY AUTO_INCREMENT,
  family_name VARCHAR(100) NOT NULL,
  family_code VARCHAR(16) NOT NULL UNIQUE,
  province VARCHAR(64),
  city VARCHAR(64),
  adcode VARCHAR(32),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE family_member (
  id INT PRIMARY KEY AUTO_INCREMENT,
  family_id INT NOT NULL,
  user_id INT NOT NULL,
  remark VARCHAR(100),
  UNIQUE KEY uk_family_user (family_id, user_id)
);

CREATE TABLE device (
  id INT PRIMARY KEY AUTO_INCREMENT,
  device_name VARCHAR(100) NOT NULL,
  family_id INT NOT NULL,
  device_type VARCHAR(64) NOT NULL,
  mqtt_topic VARCHAR(128) NOT NULL
);

CREATE TABLE chat_log (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  role VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE habit_data_log (
  id INT PRIMARY KEY AUTO_INCREMENT,
  family_id INT NOT NULL,
  user_id INT NOT NULL,
  outdoor_temp DOUBLE,
  indoor_temp DOUBLE,
  target_temp DOUBLE,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 6.3 准备演示账号

本项目当前没有区分管理员、普通用户等角色，登录用户即系统使用者。可通过注册接口创建账号，也可以直接插入一个演示账号。

推荐演示账号：

| 用户类型 | 用户名 | 密码 | 说明 |
|---|---|---|---|
| 普通演示用户 | `demo` | `123456` | 用于登录前端、创建家庭、注册设备、测试 AI 助手 |

初始化 SQL：

```sql
INSERT INTO `user` (user_name, password)
VALUES ('demo', '123456')
ON DUPLICATE KEY UPDATE password = VALUES(password);

INSERT INTO family (family_name, family_code, province, city, adcode)
VALUES ('演示家庭', 'ABC123', '上海市', '上海市', '310000')
ON DUPLICATE KEY UPDATE family_name = VALUES(family_name);

INSERT INTO family_member (family_id, user_id, remark)
SELECT f.id, u.id, '我的家'
FROM family f, `user` u
WHERE f.family_code = 'ABC123' AND u.user_name = 'demo'
ON DUPLICATE KEY UPDATE remark = VALUES(remark);
```

设备可在前端“个人中心/设备管理”中注册，也可以预置：

```sql
INSERT INTO device (device_name, family_id, device_type, mqtt_topic)
SELECT '客厅灯', f.id, 'LED', 'led-sub'
FROM family f WHERE f.family_code = 'ABC123';

INSERT INTO device (device_name, family_id, device_type, mqtt_topic)
SELECT '客厅门', f.id, 'door', 'door-sub'
FROM family f WHERE f.family_code = 'ABC123';

INSERT INTO device (device_name, family_id, device_type, mqtt_topic)
SELECT '客厅空调', f.id, 'airConditioner', 'airconditioner-sub'
FROM family f WHERE f.family_code = 'ABC123';

INSERT INTO device (device_name, family_id, device_type, mqtt_topic)
SELECT '门内红外', f.id, 'ir-in', 'cms-ir-in'
FROM family f WHERE f.family_code = 'ABC123';

INSERT INTO device (device_name, family_id, device_type, mqtt_topic)
SELECT '门外红外', f.id, 'ir-out', 'cms-ir-out'
FROM family f WHERE f.family_code = 'ABC123';

INSERT INTO device (device_name, family_id, device_type, mqtt_topic)
SELECT '卧室PIR', f.id, 'pir', 'cms-pir-bedroom'
FROM family f WHERE f.family_code = 'ABC123';
```

### 6.4 启动 Redis

macOS 可使用 Homebrew：

```bash
brew services start redis
```

或直接运行：

```bash
redis-server
```

默认配置使用：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### 6.5 配置后端 `application.yaml`

后端配置文件路径：

```bash
src/main/resources/application.yaml
```

请至少补齐 MySQL、AI、高德、和风天气配置。示例：

```yaml
server:
  port: 8080

spring:
  application:
    name: smart-home-agent

  datasource:
    url: jdbc:mysql://127.0.0.1:3306/aihome?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      password:

  ai:
    openai:
      api-key: 你的OpenAI或兼容模型API_KEY
      chat:
        options:
          model: gpt-4o-mini

map:
  gaodemap:
    key: 你的高德地图Web服务Key

weather:
  qweather:
    key: 你的和风天气Key

presence:
  ir-max-gap-ms: 10000
  ir-pending-ttl-seconds: 15
  pir-quiet-timeout: 8m
  mm-wave-enabled: false
  away-turn-off-door: false
  away-skip-ac: false
  scan-away-fixed-delay-ms: 30000
```

说明：

- 高德 Key 用于 `LocationService`，调用 IP 定位和行政区查询。
- 和风天气 Key 用于 `WeatherService`，先查询城市 `locationId`，再查询实时天气。
- AI Key 用于 `ChatController` 的 Spring AI 对话和工具调用。
- 如果使用 OpenAI 兼容服务，需要根据服务商文档额外配置 `base-url`。

### 6.6 启动后端

```bash
./mvnw spring-boot:run
```

或：

```bash
mvn spring-boot:run
```

启动成功后，后端默认地址：

```text
http://127.0.0.1:8080
```

可用接口快速验证：

```bash
curl -X POST http://127.0.0.1:8080/aihome/user/login \
  -H "Content-Type: application/json" \
  -d '{"userName":"demo","password":"123456"}'
```

预期返回：

```json
{
  "code": 200,
  "message": "suc",
  "data": {
    "id": 1,
    "userName": "demo",
    "password": "123456"
  }
}
```

## 7. 前端安装使用说明

### 7.1 进入前端目录

```bash
cd /Users/cms/Documents/code/javaproject/aihome-web
```

### 7.2 安装依赖

```bash
npm install
```

### 7.3 启动前端

```bash
npm run dev
```

默认访问地址一般为：

```text
http://127.0.0.1:5173
```

如果端口被占用，Vite 会自动切换到 5174、5175 等端口。

### 7.4 前端连接后端说明

开发环境下，前端 `src/api/request.js` 的 `baseURL` 为空，请求会走 Vite 代理。

代理配置在 `vite.config.js`：

```javascript
const API_TARGET = process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080'

server: {
  proxy: {
    '/aihome': { target: API_TARGET, changeOrigin: true },
    '/api': { target: API_TARGET, changeOrigin: true },
    '/ai': { target: API_TARGET, changeOrigin: true },
  },
}
```

如果后端不在本机 8080，可这样启动前端：

```bash
VITE_PROXY_TARGET=http://后端IP:8080 npm run dev
```

生产构建：

```bash
npm run build
```

## 8. 硬件安装使用说明

硬件目录：

```bash
cd /Users/cms/Documents/code/javaproject/AI-HOME/aihome_hardware
```

### 8.1 硬件分工

| 工程 | 说明 |
|---|---|
| `board_a_actuator` | 板 A，执行节点：LED、DHT11、SG90 舵机、按键 A/B |
| `board_b_presence` | 板 B，感知节点：双路红外对射、PIR、LD2410 |
| `demo1` | 单板演示工程 |

### 8.2 板 A 接线

| 功能 | GPIO | 说明 |
|---|---:|---|
| LED | 18 | 高电平点亮 |
| DHT11 DATA | 4 | 采集室内温湿度 |
| SG90 舵机 | 23 | 0° 关门，90° 开门 |
| 按键 A | 32 | 切换 LED |
| 按键 B | 33 | 切换门 |

注意：舵机建议使用独立 5V 电源，并与 ESP32 共地。

### 8.3 板 B 接线

| 功能 | GPIO | 说明 |
|---|---:|---|
| 红外对射 1 | 25 | 门内或门外，取决于数据库设备类型 |
| 红外对射 2 | 26 | 与红外 1 组成进出方向判断 |
| PIR | 27 | 人体移动检测 |
| LD2410 OUT | 14 | 毫米波人体存在检测 |

### 8.4 修改硬件配置

烧录前必须修改两个工程 `src/main.cpp` 顶部的 Wi-Fi 和 MQTT 配置：

```cpp
static const char *WIFI_SSID = "你的WiFi名称";
static const char *WIFI_PASSWORD = "你的WiFi密码";

static const char *MQTT_SERVER = "broker.emqx.io";
static const uint16_t MQTT_PORT = 1883;
```

同时确认硬件中的 MQTT Topic 与数据库 `device.mqtt_topic` 一致。

例如：

| 设备 | 固件 Topic | 数据库 `mqtt_topic` |
|---|---|---|
| 客厅灯 | `led-sub` 或 `cms-sub` | `led-sub` |
| 门锁/舵机 | `door-sub` | `door-sub` |
| 门内红外 | `cms-ir-in` | `cms-ir-in` |
| 门外红外 | `cms-ir-out` | `cms-ir-out` |
| PIR | `cms-pir-bedroom` | `cms-pir-bedroom` |
| 毫米波 | `cms-mmwave-living` | `cms-mmwave-living` |

### 8.5 烧录板 A

```bash
cd /Users/cms/Documents/code/javaproject/AI-HOME/aihome_hardware/board_a_actuator
pio run -t upload
pio device monitor -b 115200
```

### 8.6 烧录板 B

```bash
cd /Users/cms/Documents/code/javaproject/AI-HOME/aihome_hardware/board_b_presence
pio run -t upload
pio device monitor -b 115200
```

### 8.7 没有硬件时的 MQTT 模拟

可使用 MQTTX 连接：

```text
Broker: broker.emqx.io
Port: 1883
```

模拟温湿度：

```text
Topic: cms-pub
Payload:
{"temperature":26.5,"humidity":58.2}
```

模拟设备状态：

```text
Topic: cms-device-status
Payload:
{"ledStatus":true,"buzzerStatus":false,"monitorStatus":true}
```

模拟 PIR：

```text
Topic: cms-pir-bedroom
Payload:
{"motion":true}
```

模拟红外进门：

```text
先发布 Topic: cms-ir-out Payload: {"triggered":true}
再发布 Topic: cms-ir-in  Payload: {"triggered":true}
```

模拟红外出门：

```text
先发布 Topic: cms-ir-in  Payload: {"triggered":true}
再发布 Topic: cms-ir-out Payload: {"triggered":true}
```

## 9. 系统启动顺序

建议按以下顺序启动：

1. 启动 MySQL。
2. 启动 Redis。
3. 检查后端 `application.yaml` 中数据库、Redis、AI、高德、和风天气 Key。
4. 启动后端：

```bash
cd /Users/cms/Documents/code/javaproject/smart-home-agent
./mvnw spring-boot:run
```

5. 启动前端：

```bash
cd /Users/cms/Documents/code/javaproject/aihome-web
npm run dev
```

6. 烧录并启动 ESP32 硬件，或使用 MQTTX 模拟硬件消息。
7. 浏览器访问前端地址，使用演示账号登录：

```text
用户名：demo
密码：123456
```

## 10. 基本使用流程

### 10.1 登录系统

打开前端页面，使用：

```text
用户名：demo
密码：123456
```

登录后进入首页。

### 10.2 创建或选择家庭

如果已经执行初始化 SQL，会看到“演示家庭”。也可以在个人中心创建新家庭，填写城市、省份、备注等信息。

### 10.3 注册设备

在设备管理区域注册设备，确保设备名、设备类型、MQTT Topic 与硬件一致。

推荐演示设备：

| 设备名 | 类型 | MQTT Topic |
|---|---|---|
| 客厅灯 | `LED` | `led-sub` |
| 客厅门 | `door` | `door-sub` |
| 客厅空调 | `airConditioner` | `airconditioner-sub` |
| 门内红外 | `ir-in` | `cms-ir-in` |
| 门外红外 | `ir-out` | `cms-ir-out` |
| 卧室PIR | `pir` | `cms-pir-bedroom` |

### 10.4 控制设备

可通过首页按钮控制设备，也可通过 Postman 调用：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/home/control \
  -H "Content-Type: application/json" \
  -d '{"familyId":1,"devicename":"客厅灯","deviceType":"LED","action":"ON"}'
```

### 10.5 使用 AI 助手

前端进入 AI 助手页面，输入：

```text
打开客厅灯
```

或：

```text
把客厅空调调到26度
```

或：

```text
今天上海天气怎么样
```

AI 会根据当前用户、家庭、城市、真实设备列表和历史对话生成回复，并在需要时调用工具函数控制设备。

## 11. 常用接口说明

| 功能 | 方法 | 地址 |
|---|---|---|
| 登录 | POST | `/aihome/user/login` |
| 注册 | POST | `/aihome/user/signup` |
| 获取家庭列表 | GET | `/aihome/user/families?userId=1` |
| 创建家庭 | POST | `/aihome/user/family/create` |
| 加入家庭 | POST | `/aihome/user/family/join` |
| 注册设备 | POST | `/aihome/device/register` |
| 查询设备列表 | GET | `/aihome/device/list?familyId=1` |
| 获取首页状态 | GET | `/api/v1/home/status` |
| 控制设备 | POST | `/api/v1/home/control` |
| 控制空调 | POST | `/aihome/air-conditioning/control` |
| AI 聊天 | POST | `/ai/chat` |
| AI 流式聊天 | GET | `/ai/chat/stream` |
| 查询在宅人数 | GET | `/aihome/presence/estimated-count?familyId=1` |
| 查询传感器触发 | GET | `/aihome/presence/sensor-triggers?familyId=1` |
| 查询历史数据 | GET | `/aihome/predict/history?familyId=1` |

## 12. 外部 API 配置说明

### 12.1 高德地图 API

后端使用 `LocationService`：

- `https://restapi.amap.com/v3/ip`
- `https://restapi.amap.com/v3/config/district`

作用：

- 根据真实 IP 获取用户所在城市。
- 根据城市名获取省份、城市、行政区编码。
- 为 AI 对话和家庭位置展示提供地理上下文。

配置：

```yaml
map:
  gaodemap:
    key: 你的高德地图Key
```

### 12.2 和风天气 API

后端使用 `WeatherService`：

- `/geo/v2/city/lookup`
- `/v7/weather/now`

作用：

- 先根据城市名查询 `locationId`。
- 再查询实时天气 JSON。
- 空调控制时解析 `now.temp` 作为室外温度。

配置：

```yaml
weather:
  qweather:
    key: 你的和风天气Key
```

## 13. 常见问题

### 13.1 前端提示网络错误

检查后端是否启动：

```bash
curl http://127.0.0.1:8080/api/v1/home/status
```

如果后端端口不是 8080，需要设置：

```bash
VITE_PROXY_TARGET=http://127.0.0.1:你的端口 npm run dev
```

### 13.2 后端启动失败，提示数据库连接错误

检查：

- MySQL 是否启动。
- 数据库 `aihome` 是否已创建。
- `application.yaml` 中用户名和密码是否正确。
- 表是否已初始化。

### 13.3 AI 聊天不可用

检查：

- `spring.ai.openai.api-key` 是否配置。
- 网络是否能访问模型服务。
- 如果使用兼容模型服务，是否配置正确的 `base-url` 和模型名。

### 13.4 天气查询失败

检查：

- 和风天气 Key 是否正确。
- 和风天气 API 域名是否与账号类型匹配。
- 城市名是否能被 `/geo/v2/city/lookup` 查询到。

### 13.5 硬件没有响应

检查：

- ESP32 是否连接 Wi-Fi。
- ESP32 串口是否显示 MQTT connected。
- 数据库中的 `device.mqtt_topic` 是否和固件中的 Topic 完全一致。
- 后端日志是否显示 MQTT 已连接。
- 公共 Broker `broker.emqx.io` 是否可连接。

## 14. 毕设演示建议

推荐演示顺序：

1. 登录前端，展示首页。
2. 展示家庭和设备列表。
3. 在 Postman 调用登录、注册设备、控制设备接口。
4. 在 AI 助手输入“打开客厅灯”，展示 AI 回复和硬件响应。
5. 展示 DHT11 温湿度上报到首页。
6. 用红外/PIR 模拟人体存在，展示在宅人数或传感器触发状态。
7. 展示高德 API 和和风天气 API 在空调推荐中的作用。
8. 展示历史图表和个人中心。

## 15. 项目亮点

- 实现了前端、后端、硬件、AI、MQTT 的完整闭环。
- AI 助手不是简单聊天，而是可以调用后端工具函数控制真实设备。
- 设备使用 MQTT Topic 绑定，支持多家庭、多设备扩展。
- 引入高德定位和和风天气，让 AI 控制具备环境上下文。
- 使用 Redis 实现红外进出状态机、在宅人数估计和离家自动化。
- 支持无硬件情况下用 MQTTX 模拟传感器上报，便于答辩演示和调试。
