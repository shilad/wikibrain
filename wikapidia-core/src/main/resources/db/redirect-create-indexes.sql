CREATE INDEX IF NOT EXISTS redirect_idx_src_id ON redirect(lang_id, src_page_id);
CREATE INDEX IF NOT EXISTS redirect_idx_dest_id ON redirect(lang_id, dest_page_id);
