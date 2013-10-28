CREATE TABLE IF NOT EXISTS meta_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  component VARCHAR(255) NOT NULL,
  lang_id SMALLINT,
  num_records INT NOT NULL DEFAULT 0,
  num_errors INT NOT NULL DEFAULT 0,
  last_updated DATE,
  other VARCHAR(65535) -- JSON formatted dictionary of extra info
);

