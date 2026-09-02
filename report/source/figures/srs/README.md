# SRS 图片清单

本清单与《UML 大战需求分析》附录 1 的章节结构对齐。文件名、图类型和所在小节已经固定；绘图成员只需提交同名可编辑源文件及 PDF，正文不需要手工改图号。

| 文件 | 类型 | 所在小节 | 必须表达的内容 | 建议主责 |
| --- | --- | --- | --- | --- |
| `01-system-context.pdf` | 非 UML 系统上下文图 | 2.3 范围 | 四类用户、核心平台、外部服务及系统边界 | 示意图 |
| `02-core-domain-class.pdf` | UML 类图 | 3.3 交易与履约业务概念 | 用户、店铺、商品、购物车、订单、支付、配送、评价及关系 | UML |
| `03-assets-preference-class.pdf` | UML 类图 | 3.4 营销资产与智能交互业务概念 | 会员、钱包、积分、优惠券、流水、偏好、AI 会话及关系 | UML |
| `04-fulfillment-activity.pdf` | UML 活动图 | 4.2 外卖订单履约流程 | 六泳道履约闭环以及库存不足、支付失败、拒单、取消、配送异常 | UML |
| `05-order-state.pdf` | UML 状态机图 | 4.2 外卖订单履约流程 | 订单状态、触发事件、执行者和守卫条件 | UML |
| `06-delivery-state.pdf` | UML 状态机图 | 4.2 外卖订单履约流程 | 配送任务状态、异常处理、改派和终止 | UML |
| `07-settlement-sequence.pdf` | UML 顺序图 | 4.2 外卖订单履约流程 | 订单、库存、会员、优惠券、积分、钱包与支付的调用顺序 | UML |
| `08-ai-assistant-sequence.pdf` | UML 顺序图 | 4.3 AI 客服与智能点餐流程 | 意图识别、受控工具调用、业务二次校验、用户确认和降级 | UML |
| `09-actor-analysis.pdf` | UML 用例图（执行者关系） | 5.1 执行者分析 | 游客、已登录用户、四类角色和外部服务的泛化/协作关系 | UML |
| `10-system-use-case.pdf` | UML 用例图 | 5.2 总用例 | 全体执行者与系统一级用例，不塞入详细 CRUD | UML |
| `11-customer-use-case.pdf` | UML 用例图 | 5.4 顾客用例 | 顾客浏览、购物车、订单、履约、评价、资产和智能点餐 | UML |
| `12-merchant-use-case.pdf` | UML 用例图 | 5.5 商家用例 | 商家申请、店铺商品、库存、订单、评价回复和经营看板 | UML |
| `13-rider-use-case.pdf` | UML 用例图 | 5.6 骑手用例 | 骑手申请、上下线、接单、到店、取餐、配送、异常和历史 | UML |
| `14-admin-use-case.pdf` | UML 用例图 | 5.7 管理员用例 | 准入审核、账号与评价治理、配送异常处理和平台看板 | UML |
| `15-page-information-architecture.pdf` | 非 UML 信息架构图 | 6.5 界面 | 顾客、商家、骑手和管理端的页面导航层级 | 示意图 |
| `16-key-page-wireframes.pdf` | 非 UML 低保真原型组 | 6.5 界面 | AI 点餐、履约详情、商家工作台、骑手任务、管理员审核/看板 | 示意图 |
| `17-system-deployment.pdf` | UML 部署图 | 6.1 系统架构要求 | 浏览器、Vue 前端、Spring Boot 后端、MySQL 与外部服务部署/通信关系 | UML |

## 交付规则

1. 可编辑源文件与导出 PDF 使用相同主文件名，例如 `05-order-state.puml` 与 `05-order-state.pdf`。
2. UML 图使用标准符号；非 UML 图可以使用 draw.io、Figma 或其他原型工具，但要保持简洁一致。
3. 图内角色、状态、业务对象和用例名称必须与 SRS 正文完全一致。
4. PDF 放入本目录后，正文中的占位框会自动替换为正式图片；不要修改 `\caption`、`\label` 或手写图号。
5. 在 `report/source/` 运行 `make pdf`，确认所有图可读、无裁切且正文引用正确。
