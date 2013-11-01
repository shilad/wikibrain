CREATE TABLE IF NOT EXISTS category_members (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  category_id INT NOT NULL,
  article_id INT NOT NULL
);
