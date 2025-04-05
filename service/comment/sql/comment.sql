create table comment (
     comment_id bigint not null primary key,
     content varchar(3000) not null,
     article_id bigint not null,
     parent_comment_id bigint not null,
     writer_id bigint not null,
     deleted bool not null,
     created_at datetime not null
);

create index idx_article_id_parent_comment_id_comment_id on comment (
     article_id asc, parent_comment_id asc, comment_id asc
);