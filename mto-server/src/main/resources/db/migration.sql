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
