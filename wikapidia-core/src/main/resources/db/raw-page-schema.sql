DROP TABLE IF EXISTS raw_page;
CREATE TABLE raw_page (
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  revision_id INT NOT NULL,
  body CLOB NOT NULL,
  title VARCHAR(256) NOT NULL,
  lastEdit TIMESTAMP,
  name_space SMALLINT NOT NULL,
  is_redirect BOOLEAN NOT NULL,
  is_disambig BOOLEAN NOT NULL,
  redirect_title VARCHAR (256)
);
