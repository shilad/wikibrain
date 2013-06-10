CREATE INDEX local_page_idx_page_id ON local_page(lang_id, page_id);
CREATE INDEX local_page_idx_page_title ON local_page(lang_id, title, ns);