CREATE INDEX local_page_idx_page_id ON local_page(lang_id, page_id);
CREATE INDEX local_page_idx_page_title ON local_page(lang_id, title, page_type);

CREATE INDEX local_link_idx_source_id ON local_link(lang_id, source_id);
CREATE INDEX local_link_idx_dest_id ON local_link(lang_id, dest_id);