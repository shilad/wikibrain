DROP TABLE IF EXISTS raw_page;
CREATE TABLE raw_page (
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  revision_id INT NOT NULL,
  body CLOB NOT NULL,
  title VARCHAR(256) NOT NULL,
  lastEdit TIMESTAMP,
  page_type SMALLINT NOT NULL
);
