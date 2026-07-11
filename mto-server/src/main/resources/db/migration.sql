-- 版本迭代补丁：给已有数据库执行的结构或数据修正。
-- 使用方式：按版本段手动执行对应 SQL。已经合并到 schema.sql 的字段，不需要在新库重复执行。

-- V20260623__add_work_order_record_checkin_location
-- 说明：历史库补充工单处理记录中的打卡经纬度和定位地址字段。
set @add_longitude_sql = if(
    (select count(*) from information_schema.columns where table_schema = database() and table_name = 'work_order_record' and column_name = 'longitude') = 0,
    'alter table work_order_record add column longitude decimal(10, 6) null comment ''打卡经度''',
    'select 1'
);
prepare add_longitude_stmt from @add_longitude_sql;
execute add_longitude_stmt;
deallocate prepare add_longitude_stmt;

set @add_latitude_sql = if(
    (select count(*) from information_schema.columns where table_schema = database() and table_name = 'work_order_record' and column_name = 'latitude') = 0,
    'alter table work_order_record add column latitude decimal(10, 6) null comment ''打卡纬度''',
    'select 1'
);
prepare add_latitude_stmt from @add_latitude_sql;
execute add_latitude_stmt;
deallocate prepare add_latitude_stmt;

set @add_location_address_sql = if(
    (select count(*) from information_schema.columns where table_schema = database() and table_name = 'work_order_record' and column_name = 'location_address') = 0,
    'alter table work_order_record add column location_address varchar(255) null comment ''打卡定位地址''',
    'select 1'
);
prepare add_location_address_stmt from @add_location_address_sql;
execute add_location_address_stmt;
deallocate prepare add_location_address_stmt;

-- V20260623__disable_legacy_permissions
-- 说明：停用旧版粗粒度权限，避免权限分配页面继续显示旧权限。
update sys_permission
set status = 0, updated_at = now()
where code in (
    'user:read',
    'user:manage',
    'customer:read',
    'customer:manage',
    'device:read',
    'device:manage',
    'work-order:read',
    'work-order:manage',
    'attachment:read',
    'attachment:manage',
    'map:read',
    'map:use'
);

-- v1.1版本迭代内容
-- V20260709__sync_work_order_record_operator_real_name
-- 说明：历史工单流转记录中 operator_name 曾保存账号名，这里按 operator_id 回填用户真实姓名。
update work_order_record r
inner join sys_user u on u.id = r.operator_id
set r.operator_name = u.real_name
where u.real_name is not null
  and u.real_name <> ''
  and (r.operator_name is null or r.operator_name <> u.real_name);

-- V20260709__add_work_order_maintenance_content
-- 说明：工单增加维保内容字段，现场工单和巡检工单都需要填写。
alter table work_order add column maintenance_content varchar(500) null comment '维保内容';

-- V20260709__add_work_order_delete_permission
-- 说明：新增工单物理删除权限，仅默认分配给管理员。
insert into sys_permission (code, name, module, status, sort_order, created_at, updated_at)
select 'work-order:delete', '删除工单', '工单管理', 1, 380, now(), now()
where not exists (select 1 from sys_permission where code = 'work-order:delete');

insert into sys_role_permission (role, permission_code, created_at)
select 'admin', 'work-order:delete', now()
where not exists (
    select 1 from sys_role_permission
    where role = 'admin' and permission_code = 'work-order:delete'
);

-- V20260711__add_app_version
-- 新增 App 版本发布记录，用于 APK 下载更新和 WGT 热更新。
create table if not exists app_version (
    id bigint primary key auto_increment,
    platform varchar(20) not null default 'android' comment '平台，android 或 ios',
    update_type varchar(20) not null default 'wgt' comment '更新类型，wgt 热更新包或 apk 安装包',
    version_name varchar(50) not null comment '版本名称',
    version_code int not null comment '版本号，用于客户端比较',
    min_version_code int not null default 0 comment '最低可用版本号，低于该版本时强制更新',
    base_version_code int null comment 'WGT 包对应的基础 APK 版本号',
    download_url varchar(500) not null comment '更新包下载地址',
    release_notes varchar(1000) null comment '更新说明',
    force_update tinyint not null default 0 comment '是否强制更新：1 是，0 否',
    status tinyint not null default 1 comment '状态：1 启用，0 停用',
    created_at datetime not null,
    updated_at datetime not null,
    key idx_app_version_platform_status (platform, status),
    key idx_app_version_version_code (version_code)
) comment 'App 版本发布记录';
