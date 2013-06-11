CREATE INDEX local_page_idx_page_id ON local_page(univ_id);
CREATE INDEX local_page_idx_page_title ON local_page(univ_id, page_type);