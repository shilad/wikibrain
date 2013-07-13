CREATE INDEX IF NOT EXISTS universal_link_idx_source_id ON universal_link(source_univ_id, algorithm_id);
CREATE INDEX IF NOT EXISTS universal_link_idx_dest_id ON universal_link(dest_univ_id, algorithm_id);
CREATE INDEX IF NOT EXISTS universal_link_idx_both_ids ON universal_link(source_univ_id, dest_univ_id, algorithm_id);
