CREATE INDEX IF NOT EXISTS meta_idx_comp ON meta_info(component);
CREATE INDEX IF NOT EXISTS meta_idx_comp_lang ON meta_info(component, lang_id);
