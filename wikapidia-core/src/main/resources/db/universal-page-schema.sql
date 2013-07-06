CREATE TABLE IF NOT EXISTS universal_page (
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  name_space SMALLINT NOT NULL,
  univ_id INT NOT NULL,
  algorithm_id INT NOT NULL
);

DROP INDEX IF EXISTS universal_page_idx_page_id;
DROP INDEX IF EXISTS universal_page_idx_page_type;