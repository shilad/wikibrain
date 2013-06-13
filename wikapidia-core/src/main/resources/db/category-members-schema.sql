DROP TABLE IF EXISTS category_members;
CREATE TABLE category_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  article_id INT NOT NULL,
  category_id INT NOT NULL
);