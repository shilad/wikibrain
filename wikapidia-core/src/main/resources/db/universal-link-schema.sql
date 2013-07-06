CREATE TABLE IF NOT EXISTS universal_link (
  lang_id SMALLINT NOT NULL,
  source_id INT NOT NULL,
  dest_id INT NOT NULL,
  source_univ_id INT NOT NULL,
  dest_univ_id INT NOT NULL,
  algorithm_id INT NOT NULL
);

DROP INDEX IF EXISTS universal_link_idx_source_id;
DROP INDEX IF EXISTS universal_link_idx_dest_id;
DROP INDEX IF EXISTS universal_link_idx_both_ids;
