DROP TABLE if exists redirect;
CREATE TABLE redirect (
  lang_id SMALLINT NOT NULL,
  src_page_id INT NOT NULL,
  dest_page_id INT NOT NULL
);