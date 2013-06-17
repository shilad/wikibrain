DROP TABLE IF EXISTS universal_page;
CREATE TABLE universal_page (
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  name_space SMALLINT NOT NULL,
  univ_id INT NOT NULL,
  algorithm_id INT NOT NULL
);