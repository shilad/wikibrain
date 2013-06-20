DROP TABLE if exists universal_link;
CREATE TABLE universal_link (
  lang_id SMALLINT NOT NULL,
  source_id INT NOT NULL,
  dest_id INT NOT NULL,
  location INT NOT NULL,
  source_univ_id INT NOT NULL,
  dest_univ_id INT NOT NULL,
  algorithm_id INT NOT NULL
);