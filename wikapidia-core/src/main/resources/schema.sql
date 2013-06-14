
DROP TABLE IF EXISTS local_page;
CREATE TABLE local_page (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  title VARCHAR(256) NOT NULL,
  name_space SMALLINT NOT NULL,
  is_redirect BOOLEAN NOT NULL,
  is_disambig BOOLEAN NOT NULL
);

DROP TABLE IF EXISTS local_link;
CREATE TABLE local_link (
  lang_id SMALLINT NOT NULL,
  anchor_text TEXT NOT NULL,
  source_id INT NOT NULL,
  dest_id INT NOT NULL,
  location INT NOT NULL,
  is_parseable BOOLEAN NOT NULL,
  location_type SMALLINT NOT NULL
);

DROP TABLE IF EXISTS universal_page;
CREATE TABLE universal_page (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  title VARCHAR(256) NOT NULL,
  name_space SMALLINT NOT NULL,
  univ_id INT NOT NULL
);

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
  is_disambig BOOLEAN NOT NULL
);

DROP TABLE IF EXISTS category_members;
CREATE TABLE category_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  category_id INT NOT NULL,
  article_id INT NOT NULL
);

DROP TABLE if exists redirect;
CREATE TABLE redirect (
  resolved BOOLEAN NOT NULL,
  lang_id SMALLINT NOT NULL,
  redirected_title VARCHAR(256) NOT NULL,
  title VARCHAR(256) NOT NULL,
  name_space SMALLINT NOT NULL
);

DROP TABLE IF EXISTS table_modified;
CREATE TABLE table_modified (
  table_name VARCHAR(256),
  last_modified TIMESTAMP
);

COMMIT;