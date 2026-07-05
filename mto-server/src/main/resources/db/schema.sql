create table if not exists customer_site (
    id bigint primary key auto_increment,
    name varchar(100) not null comment '客户现场名称',
    address varchar(255) null comment '地址',
    contact_name varchar(50) null comment '联系人',
    contact_phone varchar(30) null comment '联系电话',
    longitude decimal(10, 6) null comment '经度',
    latitude decimal(10, 6) null comment '纬度',
    location_address varchar(255) null comment '定位地址',
    location_remark varchar(255) null comment '定位备注',
    remark varchar(500) null comment '备注',
    status tinyint not null default 1 comment '状态：1启用，0停用',
    created_at datetime not null,
    updated_at datetime not null,
    key idx_customer_site_name (name),
    key idx_customer_site_status (status)
) comment '客户现场档案';

create table if not exists sys_user (
    id bigint primary key auto_increment,
    username varchar(50) not null comment '账号',
    password_hash varchar(128) not null comment '密码哈希',
    real_name varchar(50) null comment '姓名',
    phone varchar(30) null comment '手机号',
    role varchar(30) not null default 'admin' comment '角色',
    status tinyint not null default 1 comment '状态：1启用，0停用',
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_sys_user_username (username),
    key idx_sys_user_status (status)
) comment '用户';

create table if not exists sys_permission (
    id bigint primary key auto_increment,
    code varchar(80) not null comment '权限编码',
    name varchar(80) not null comment '权限名称',
    module varchar(50) not null comment '所属模块',
    status tinyint not null default 1 comment '状态：1启用，0停用',
    sort_order int not null default 0 comment '排序',
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_sys_permission_code (code),
    key idx_sys_permission_module (module),
    key idx_sys_permission_status (status)
) comment '权限定义';

create table if not exists sys_role_permission (
    id bigint primary key auto_increment,
    role varchar(30) not null comment '角色',
    permission_code varchar(80) not null comment '权限编码',
    created_at datetime not null,
    unique key uk_sys_role_permission (role, permission_code),
    key idx_sys_role_permission_role (role)
) comment '角色权限分配';

create table if not exists sys_user_permission (
    id bigint primary key auto_increment,
    user_id bigint not null comment '用户ID',
    permission_code varchar(80) not null comment '权限编码',
    effect varchar(10) not null default 'allow' comment '效果：allow允许，deny拒绝',
    created_at datetime not null,
    unique key uk_sys_user_permission (user_id, permission_code),
    key idx_sys_user_permission_user (user_id),
    key idx_sys_user_permission_effect (effect)
) comment '用户权限覆盖';

create table if not exists customer_device (
    id bigint primary key auto_increment,
    name varchar(100) not null comment '设备名称',
    model varchar(100) null comment '设备型号',
    serial_no varchar(100) null comment '设备编号/序列号',
    status tinyint not null default 1 comment '状态：1启用，0停用',
    created_at datetime not null,
    updated_at datetime not null,
    key idx_customer_device_name (name),
    key idx_customer_device_status (status)
) comment '设备档案';

create table if not exists work_order (
    id bigint primary key auto_increment,
    order_no varchar(40) not null comment '工单编号',
    title varchar(120) null comment '工单标题（系统生成）',
    type varchar(30) not null comment '工单类型：onsite现场工单，inspection日常巡检',
    customer_site_id bigint not null comment '客户现场ID',
    customer_site_name varchar(100) not null comment '客户现场名称快照',
    customer_address varchar(255) null comment '客户地址快照',
    device_id bigint null comment '设备ID',
    device_name varchar(100) null comment '设备名称快照',
    priority varchar(20) not null default 'normal' comment '优先级：normal普通，urgent紧急',
    status varchar(30) not null default 'pending' comment '状态：pending待处理，processing处理中，completed已完成，closed已关闭',
    content varchar(1000) null comment '工单内容',
    notice varchar(1000) null comment '注意事项',
    estimated_arrival_time datetime null comment '预计到达时间',
    estimated_complete_time datetime null comment '预估完成时间',
    completed_at datetime null comment '完成时间',
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_work_order_no (order_no),
    key idx_work_order_customer_site (customer_site_id),
    key idx_work_order_type (type),
    key idx_work_order_status (status),
    key idx_work_order_created_at (created_at)
) comment '工单';

create table if not exists work_order_engineer (
    id bigint primary key auto_increment,
    work_order_id bigint not null comment '工单ID',
    user_id bigint not null comment '工程师用户ID',
    username varchar(50) not null comment '工程师账号快照',
    real_name varchar(50) null comment '工程师姓名快照',
    created_at datetime not null,
    unique key uk_work_order_engineer (work_order_id, user_id),
    key idx_work_order_engineer_order (work_order_id),
    key idx_work_order_engineer_user (user_id)
) comment '工单指派工程师';

create table if not exists file_attachment (
    id bigint primary key auto_increment,
    biz_type varchar(50) null comment '业务类型，例如 work_order',
    biz_id bigint null comment '业务ID',
    category varchar(50) null comment '附件分类',
    original_name varchar(255) not null comment '原始文件名',
    file_name varchar(255) not null comment '存储文件名',
    content_type varchar(100) null comment '文件类型',
    file_size bigint not null comment '文件大小',
    storage_path varchar(500) not null comment '本地相对存储路径',
    access_url varchar(500) not null comment '访问地址',
    status tinyint not null default 1 comment '状态：1正常，0删除',
    created_at datetime not null,
    updated_at datetime not null,
    key idx_file_attachment_biz (biz_type, biz_id),
    key idx_file_attachment_category (category),
    key idx_file_attachment_status (status),
    key idx_file_attachment_created_at (created_at)
) comment '文件附件';

create table if not exists work_order_record (
    id bigint primary key auto_increment,
    work_order_id bigint not null comment '工单ID',
    record_type varchar(30) not null comment '记录类型：create创建，update更新，status状态变更，process处理记录',
    status_before varchar(30) null comment '变更前状态',
    status_after varchar(30) null comment '变更后状态',
    content varchar(1000) null comment '记录内容',
    longitude decimal(10, 6) null comment '打卡经度',
    latitude decimal(10, 6) null comment '打卡纬度',
    location_address varchar(255) null comment '打卡定位地址',
    operator_id bigint null comment '操作人ID',
    operator_name varchar(50) null comment '操作人',
    created_at datetime not null,
    key idx_work_order_record_order (work_order_id),
    key idx_work_order_record_type (record_type),
    key idx_work_order_record_created_at (created_at)
) comment '工单处理记录';

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

insert into sys_user (username, password_hash, real_name, role, status, created_at, updated_at)
select 'admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'Admin', 'admin', 1, now(), now()
where not exists (select 1 from sys_user where username = 'admin');

insert ignore into sys_permission (code, name, module, status, sort_order, created_at, updated_at) values
('user:list', '人员列表', '人员管理', 1, 10, now(), now()),
('user:detail', '人员详情', '人员管理', 1, 20, now(), now()),
('user:create', '新增人员', '人员管理', 1, 30, now(), now()),
('user:edit', '编辑人员', '人员管理', 1, 40, now(), now()),
('user:status', '启停人员', '人员管理', 1, 50, now(), now()),
('user:password', '重置密码', '人员管理', 1, 60, now(), now()),
('user:permission', '分配权限', '人员管理', 1, 70, now(), now()),
('customer:list', '客户列表', '客户管理', 1, 110, now(), now()),
('customer:detail', '客户详情', '客户管理', 1, 120, now(), now()),
('customer:create', '新增客户', '客户管理', 1, 130, now(), now()),
('customer:edit', '编辑客户', '客户管理', 1, 140, now(), now()),
('customer:status', '启停客户', '客户管理', 1, 150, now(), now()),
('device:list', '设备列表', '设备管理', 1, 210, now(), now()),
('device:detail', '设备详情', '设备管理', 1, 220, now(), now()),
('device:create', '新增设备', '设备管理', 1, 230, now(), now()),
('device:edit', '编辑设备', '设备管理', 1, 240, now(), now()),
('device:status', '启停设备', '设备管理', 1, 250, now(), now()),
('work-order:list', '工单列表', '工单管理', 1, 310, now(), now()),
('work-order:detail', '工单详情', '工单管理', 1, 320, now(), now()),
('work-order:create', '新增工单', '工单管理', 1, 330, now(), now()),
('work-order:edit', '工单编辑', '工单管理', 1, 340, now(), now()),
('work-order:status', '状态变更', '工单管理', 1, 350, now(), now()),
('work-order:complete', '完成工单', '工单管理', 1, 360, now(), now()),
('work-order:process', '处理记录', '工单管理', 1, 370, now(), now()),
('attachment:list', '附件列表', '附件管理', 1, 410, now(), now()),
('attachment:detail', '附件详情', '附件管理', 1, 420, now(), now()),
('attachment:upload', '上传附件', '附件管理', 1, 430, now(), now()),
('attachment:delete', '删除附件', '附件管理', 1, 440, now(), now());

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

insert ignore into sys_role_permission (role, permission_code, created_at) values
('admin', 'user:list', now()),
('admin', 'user:detail', now()),
('admin', 'user:create', now()),
('admin', 'user:edit', now()),
('admin', 'user:status', now()),
('admin', 'user:password', now()),
('admin', 'user:permission', now()),
('admin', 'customer:list', now()),
('admin', 'customer:detail', now()),
('admin', 'customer:create', now()),
('admin', 'customer:edit', now()),
('admin', 'customer:status', now()),
('admin', 'device:list', now()),
('admin', 'device:detail', now()),
('admin', 'device:create', now()),
('admin', 'device:edit', now()),
('admin', 'device:status', now()),
('admin', 'work-order:list', now()),
('admin', 'work-order:detail', now()),
('admin', 'work-order:create', now()),
('admin', 'work-order:edit', now()),
('admin', 'work-order:status', now()),
('admin', 'work-order:complete', now()),
('admin', 'work-order:process', now()),
('admin', 'attachment:list', now()),
('admin', 'attachment:detail', now()),
('admin', 'attachment:upload', now()),
('admin', 'attachment:delete', now()),
('online_ops', 'user:list', now()),
('online_ops', 'user:detail', now()),
('online_ops', 'customer:list', now()),
('online_ops', 'customer:detail', now()),
('online_ops', 'customer:create', now()),
('online_ops', 'customer:edit', now()),
('online_ops', 'customer:status', now()),
('online_ops', 'device:list', now()),
('online_ops', 'device:detail', now()),
('online_ops', 'device:create', now()),
('online_ops', 'device:edit', now()),
('online_ops', 'device:status', now()),
('online_ops', 'work-order:list', now()),
('online_ops', 'work-order:detail', now()),
('online_ops', 'work-order:create', now()),
('online_ops', 'work-order:edit', now()),
('online_ops', 'work-order:status', now()),
('online_ops', 'work-order:complete', now()),
('online_ops', 'work-order:process', now()),
('online_ops', 'attachment:list', now()),
('online_ops', 'attachment:detail', now()),
('online_ops', 'attachment:upload', now()),
('online_ops', 'attachment:delete', now()),
('field_engineer', 'work-order:list', now()),
('field_engineer', 'work-order:detail', now()),
('field_engineer', 'work-order:complete', now()),
('field_engineer', 'work-order:process', now()),
('field_engineer', 'attachment:list', now()),
('field_engineer', 'attachment:detail', now()),
('field_engineer', 'attachment:upload', now());
