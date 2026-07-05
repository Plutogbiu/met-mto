# mto-admin

PC 管理后台，基于 Vue 3 + Vite + Element Plus。

## 当前页面

1. 登录。
2. 工单管理。
3. 工单详情。
4. 客户管理。
5. 客户详情。
6. 设备管理。
7. 人员管理。

后台附件管理不单独作为菜单出现。图片上传主要给后续移动端现场使用，后台只在工单详情中查看和审查图片资料。

## 本地运行

```bash
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

## 构建

```bash
npm run build
```

Vite 构建时可能出现来自依赖的 pure annotation warning 和 chunk size warning，当前不影响构建结果。
