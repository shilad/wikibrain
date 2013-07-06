CREATE TABLE IF NOT EXISTS redirect (
  lang_id SMALLINT NOT NULL,
  src_page_id INT NOT NULL,
  dest_page_id INT NOT NULL
);
DROP INDEX IF EXISTS redirect_idx_src_id;
DROP INDEX IF EXISTS redirect_idx_dest_id;
