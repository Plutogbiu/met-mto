# mto-server

后端 API 服务，基于 Spring Boot 3 + MyBatis Plus + MySQL。

## 当前能力

1. 登录鉴权：JWT + Redis，Token 30 分钟过期。
2. 统一返回结果：`ApiResult`。
3. 分页结果：`PageResult`。
4. 全局异常：`exception` 包和 `ErrorCode` 异常枚举。
5. 跨域配置。
6. MyBatis Plus 分页插件和控制台 SQL 日志。
7. 客户管理。
8. 人员管理。
9. 设备管理。
10. 工单管理。
11. 工单处理记录。
12. 文件上传和附件分类。
13. 腾讯地图配置和地点搜索。

## 当前存储

开发环境使用本地文件存储：

```text
D:/workspace/met-mto/uploads
```

后续上服务器后可按需要切换 MinIO，目前不预留 MinIO 配置。

## 数据库

开发环境：

```text
database: mto
username: root
password: root
```

初始化和变更 SQL 维护在：

```text
src/main/resources/db/schema.sql
```

## 主要表

1. `sys_user`
2. `customer_site`
3. `customer_device`
4. `work_order`
5. `work_order_engineer`
6. `work_order_record`
7. `file_attachment`

## 启动

```bash
mvn spring-boot:run
```

如果本机没有 Maven，需要先安装 Maven 或使用 IDE 自带 Maven 启动。
