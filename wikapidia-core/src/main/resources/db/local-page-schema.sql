DROP TABLE IF EXISTS local_page;
CREATE TABLE local_page (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  title VARCHAR(256) NOT NULL,
  page_type SMALLINT NOT NULL
);