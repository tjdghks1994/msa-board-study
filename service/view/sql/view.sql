create database article_view;

-- Redis 데이터를 backup 하는 용도의 테이블
create table article_view_count (
    article_id bigint not null primary key,
    view_count bigint not null
);