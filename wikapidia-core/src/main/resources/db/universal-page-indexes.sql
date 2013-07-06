CREATE INDEX universal_page_idx_page_id ON universal_page(univ_id, algorithm_id);
CREATE INDEX universal_page_idx_page_type ON universal_page(univ_id, algorithm_id, name_space);
