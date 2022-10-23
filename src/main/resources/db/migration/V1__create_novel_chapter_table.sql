CREATE TABLE chapter_info (
    id SERIAL not null,
    title varchar(255),
    link varchar(255) unique
);