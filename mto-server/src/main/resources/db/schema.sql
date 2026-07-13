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
    maintenance_content varchar(500) null comment '维保内容',
    notice varchar(1000) null comment '注意事项',
    estimated_arrival_time datetime null comment '预计到达时间',
    estimated_complete_time datetime null comment '预估完成时间',
    completed_at datetime null comment '完成时间',
    created_at datetime not null comment '创建时间',
    updated_at datetime not null comment '更新时间',
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
