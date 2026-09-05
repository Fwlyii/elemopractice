# 外卖平台前端

Vue 3 项目，开发环境使用 Node.js 20 LTS。所有 HTTP 请求经过 `src/utils/request.js`，登录态只通过 `src/utils/auth.js` 读写。

## 安装依赖

```
npm install
```

## 本地开发

```
npm run serve
```

## 生产构建

```
npm run build
```

构建产物位于 `dist/`，项目默认不生成生产 source map。
