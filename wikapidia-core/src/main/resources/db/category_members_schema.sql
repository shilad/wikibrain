DROP TABLE IF EXISTS category_members;
CREATE TABLE local_page (
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  article_id INT NOT NULL,
  category_id INT NOT NULL
);