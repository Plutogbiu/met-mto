# 数据库 SQL 使用说明

当前项目不引入 Flyway，数据库脚本按人工执行方式维护。

## 文件职责

| 文件 | 用途 | 执行场景 |
| --- | --- | --- |
| `schema.sql` | 只放建表语句 | 新环境初始化数据库结构 |
| `data.sql` | 默认账号、默认权限、角色权限 | 新环境初始化基础数据 |
| `migration.sql` | 版本迭代字段补丁、历史数据修正 | 老环境升级时按版本段执行 |

## 新环境初始化

新建数据库后按顺序执行：

```sql
source schema.sql;
source data.sql;
```

默认管理员账号：

```text
账号：admin
密码：admin
```

## 版本升级

后续每次数据库结构或基础数据需要调整时，不直接修改已上线环境的数据表。

做法：

1. 如果是新表或当前最新完整表结构，更新 `schema.sql`。
2. 如果是默认账号、默认权限、角色权限，更新 `data.sql`。
3. 如果是已有数据库需要升级，追加一段到 `migration.sql`。

`migration.sql` 追加格式建议：

```sql
-- V20260708__add_xxx_field
-- 说明：这里写清楚本次升级目的。
alter table xxx add column xxx varchar(50) null comment 'xxx';
```

服务器升级时，只执行本次新增的版本段，避免重复执行旧补丁。

