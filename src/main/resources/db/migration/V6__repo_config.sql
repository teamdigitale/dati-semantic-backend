alter table REPOSITORY
    add CONFIG TEXT;

alter table REPOSITORY
    modify NAME varchar(256) not null;
