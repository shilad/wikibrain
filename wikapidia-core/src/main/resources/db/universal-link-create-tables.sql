CREATE TABLE IF NOT EXISTS universal_link (
  lang_id SMALLINT NOT NULL,
  local_source_id INT NOT NULL,
  local_dest_id INT NOT NULL,
  univ_source_id INT NOT NULL,
  univ_dest_id INT NOT NULL,
  algorithm_id INT NOT NULL
);

