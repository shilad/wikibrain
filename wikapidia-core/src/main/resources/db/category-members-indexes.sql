CREATE INDEX local_category_member_idx_article_id ON category_members(article_id, lang_id);
CREATE INDEX local_category_member_idx_category_id ON category_members(category_id, lang_id);
