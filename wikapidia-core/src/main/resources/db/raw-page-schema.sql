DROP TABLE IF EXISTS raw_page;
CREATE TABLE raw_page (
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  text CLOB NOT NULL
);
