# 轻量外卖平台

课程综合实践项目，包含顾客、商家、骑手和管理员四个工作端。技术栈为 Vue 3、Spring Boot 3、MyBatis 和 MySQL 8。

## 快速启动

macOS 可直接双击 `启动演示.command`，或在项目根目录执行：

```bash
./scripts/ensure-demo-env.sh
docker compose -f docker-compose.demo.yml up -d --build
```

本地服务：

- 前端：`http://localhost:18081`
- 后端：`http://localhost:18080`
- MySQL：`localhost:13306`

演示账号密码均为 `Demo1234!`：

- 顾客：`demo_user`
- 商家：`demo_merchant`
- 骑手：`demo_rider`
- 待审核骑手：`demo_rider_candidate`
- 管理员：`demo_admin`

停止服务：

```bash
docker compose -f docker-compose.demo.yml down
```

更完整的启动、数据恢复和四端验收流程见 [演示回归与开发清单](./演示回归与开发清单.md)。

## 项目结构

```text
elmclient/                  Vue 3 前端
elm_bk/                     Spring Boot 后端与数据库全量脚本
scripts/                    架构检查、环境准备和部署脚本
report/                     SRS 与课程报告
rush-docs/                  需求、分工和协作资料
demo-seed.sql               基础演示数据
rider-demo-seed.sql         骑手与配送演示数据
demo-showcase-seed.sql      丰富商家、菜品和评价数据
docker-compose.demo.yml     本地一键编排
```

重要文档：

- [架构与改需求指南](./架构与改需求指南.md)：业务规则的权威入口和推荐阅读顺序。
- [演示回归与开发清单](./演示回归与开发清单.md)：每次改需求后的四端回归清单。
- [SRS 实现进度](./SRS实现进度.md)：需求与代码实现对照。
- [安全说明](./SECURITY.md)：密钥、数据库和演示功能边界。

## 开发与验证

前端：

```bash
cd elmclient
npm install
npm run serve
npm run build
```

后端需要 JDK 17：

```bash
cd elm_bk
./mvnw test
./mvnw spring-boot:run
```

提交前至少执行：

```bash
bash scripts/check-architecture.sh
cd elmclient && npm run build
cd ../elm_bk && ./mvnw test
```

## 开发约定

- 前端不决定价格、优惠、权限或订单状态能否跳转；后端必须根据 JWT 和数据库重新校验。
- 登录态统一通过 `elmclient/src/utils/auth.js` 读写，路由角色边界由 `meta.role` 和 `router/authGuard.js` 维护。
- Controller 处理 HTTP 输入，Service 组织业务，Policy/Domain Service 保存可独立测试的规则，Mapper 负责数据读写。
- 不在页面中伪造接口成功或模拟业务数据；演示数据统一放在 seed SQL 中。

## 配置与公网演示

后端主要环境变量为 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 和可选的 `DEEPSEEK_API_KEY`。前端高德地图 Key 通过 `elmclient/.env.local` 中的 `VUE_APP_AMAP_KEY` 配置。请勿将密钥或本地 `.env` 提交到 Git。

固定演示地址为 [elm-demo.pages.dev](https://elm-demo.pages.dev) 和 [elm-demo.whliugong.xyz](https://elm-demo.whliugong.xyz)。部署方式见 [固定公网部署说明](./固定公网部署说明.md)。

课程演示可设置 `APP_DEMO_ENABLED=true` 开放模拟支付、余额充值和免费会员。正式部署必须关闭该开关，并接入支付平台的服务端签名回调。
