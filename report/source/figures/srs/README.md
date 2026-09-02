# SRS 图片清单

| 文件 | 类型 | 内容 | 建议主责 |
| --- | --- | --- | --- |
| `01-system-context.pdf` | 非 UML | 四类用户、平台与外部服务边界 | 示意图 |
| `02-core-domain-class.pdf` | UML 类图 | 用户、店铺、商品、订单、配送、评价 | UML |
| `03-assets-preference-class.pdf` | UML 类图 | 会员、钱包、积分、优惠券、偏好 | UML |
| `04-fulfillment-activity.pdf` | UML 活动图 | 六泳道外卖履约闭环及异常 | UML |
| `05-order-state.pdf` | UML 状态机 | 订单状态及触发者 | UML |
| `06-delivery-state.pdf` | UML 状态机 | 配送任务状态及异常处理 | UML |
| `07-settlement-sequence.pdf` | UML 顺序图 | 订单、库存、资产与支付 | UML |
| `08-ai-assistant-sequence.pdf` | UML 顺序图 | AI 意图、工具调用、二次校验与确认 | UML |
| `09-feature-map.pdf` | 非 UML | 七大功能模块树 | 示意图 |
| `10-system-use-case.pdf` | UML 用例图 | 系统一级用例 | UML |
| `11-customer-use-case.pdf` | UML 用例图 | 顾客用例 | UML |
| `12-merchant-use-case.pdf` | UML 用例图 | 商家用例 | UML |
| `13-rider-use-case.pdf` | UML 用例图 | 骑手用例 | UML |
| `14-admin-use-case.pdf` | UML 用例图 | 管理员用例 | UML |
| `15-page-information-architecture.pdf` | 非 UML | 四端页面导航层级 | 示意图 |
| `16-key-page-wireframes.pdf` | 非 UML | AI 点餐、履约、商家工作台、骑手任务、管理看板原型组 | 示意图 |
| `17-external-services.pdf` | 非 UML | 核心服务、AI、地图、对象存储与降级路径 | 示意图 |

规则：图内术语必须与 SRS 正文一致；所有源文件与导出图同名保存；正文不手写图号，统一使用 `\caption` 和 `\label` 自动编号。
