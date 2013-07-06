CREATE TABLE IF NOT EXISTS category_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  category_id INT NOT NULL,
  article_id INT NOT NULL
);

DROP INDEX IF EXISTS local_category_member_idx_article_id;
DROP INDEX IF EXISTS local_category_member_idx_category_id;
