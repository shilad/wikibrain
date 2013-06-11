DROP TABLE IF EXISTS local_page;
CREATE TABLE local_page (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  title VARCHAR(256) NOT NULL,
  page_type SMALLINT NOT NULL
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
  page_type SMALLINT NOT NULL,
  univ_id INT NOT NULL
);
COMMIT;