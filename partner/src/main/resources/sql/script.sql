create table client
(
    id       mediumtext    null,
    account  varchar(20)   not null comment '账号',
    password varchar(30)   not null comment '密码',
    role     int default 0 null comment '角色: 0-普通用户, 1-管理员',
    name     varchar(50)   null comment '用户昵称'
)
    comment '客户表';

create table curriculum
(
    id           bigint auto_increment
        primary key,
    student_id   varchar(64)                        not null comment '学号',
    year         varchar(20)                        not null comment '学年 (xnm)',
    semester     varchar(20)                        not null comment '学期 (xqm)',
    course_name  varchar(255)                       null comment '课程名称 (kcmc)',
    teacher      varchar(100)                       null comment '教师 (xm)',
    location     varchar(100)                       null comment '教室 (cdmc)',
    week_range   varchar(100)                       null comment '周次 (zcd)',
    day_of_week  varchar(20)                        null comment '星期几 (xqjmc)',
    day_code     varchar(10)                        null comment '星期代码 (xqj)',
    session_info varchar(50)                        null comment '节次 (jc)',
    credit       varchar(10)                        null comment '学分 (xf)',
    create_time  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '学生课表数据';

create index idx_student_term
    on curriculum (student_id, year, semester);

create table data_cache_meta
(
    id              bigint auto_increment
        primary key,
    student_id      varchar(30)                   not null,
    data_type       varchar(30)                   not null comment 'scores / timetable / exams',
    term_key        varchar(50)                   not null comment '格式：2024-2025_3',
    last_query_time datetime                      not null,
    status          varchar(20) default 'success' not null,
    constraint uk_student_type_term
        unique (student_id, data_type, term_key)
);

create table schedule_system
(
    id          bigint auto_increment
        primary key,
    student_id  varchar(50)  not null comment '学号',
    semester    varchar(20)  not null comment '学期，如 2024-2025-1',
    course_name varchar(100) not null comment '对应 kcmc',
    teacher     varchar(50)  null comment '对应 xm',
    location    varchar(100) null comment '对应 cdmc',
    day_of_week int          not null comment '星期几 1-7, 对应 xqj',
    start_node  int          not null comment '开始节次, 解析 jcs 得到',
    end_node    int          not null comment '结束节次',
    week_list   json         not null comment '核心：存储该课有效的周次数组，如 [1,2,3,4]',
    raw_zcd     varchar(50)  null comment '原始周次字符串，方便排查',
    constraint uk_course
        unique (student_id, semester, day_of_week, start_node)
);

create table schedule_user
(
    id                bigint auto_increment
        primary key,
    student_id        varchar(50)                      not null,
    semester          varchar(20)                      not null,
    operation_type    enum ('ADD', 'DELETE', 'MODIFY') not null,
    target_day        int                              null,
    target_start_node int                              null,
    custom_name       varchar(100)                     null,
    custom_location   varchar(100)                     null,
    custom_weeks      json                             null comment '用户自定义的周次',
    is_active         tinyint(1) default 1             null
);

create table score
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    student_id  varchar(20)   not null comment '学号（xh）',
    year        varchar(20)   not null comment '学年（xnmmc），如：2023-2024',
    semester    varchar(10)   not null comment '学期（xqmmc），如：1、2、短学期等',
    course_name varchar(100)  not null comment '课程名称（kcmc）',
    credit      decimal(3, 1) null comment '学分（xf）',
    point       decimal(3, 2) null comment '绩点（jd）',
    grade       decimal(5, 2) null comment '成绩（cj），可能是百分制或等级',
    gpa         decimal(5, 2) null comment '学分绩点（xfjd）= 学分 × 绩点',
    create_time datetime      not null,
    update_time datetime      not null
)
    comment '学生成绩表';

create index idx_student_id
    on score (student_id);

create index idx_student_year_semester
    on score (student_id, year, semester);

create table system_notice
(
    id           bigint auto_increment
        primary key,
    title        varchar(100)                       not null comment '公告标题',
    content      text                               null comment '公告内容',
    publisher_id bigint                             null comment '发布管理员ID',
    create_time  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '系统公告表';


