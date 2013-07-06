CREATE TABLE IF NOT EXISTS local_link (
  lang_id SMALLINT NOT NULL,
  anchor_text TEXT NOT NULL,
  source_id INT NOT NULL,
  dest_id INT NOT NULL,
  location INT NOT NULL,
  is_parseable BOOLEAN NOT NULL,
  location_type SMALLINT NOT NULL
);

DROP INDEX IF EXISTS local_link_idx_source_id;
DROP INDEX IF EXISTS local_link_idx_dest_id;
