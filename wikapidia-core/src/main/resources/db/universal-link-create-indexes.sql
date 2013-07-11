CREATE INDEX IF NOT EXISTS universal_link_idx_source_id ON universal_link(source_id, algorithm_id);
CREATE INDEX IF NOT EXISTS universal_link_idx_dest_id ON universal_link(dest_id, algorithm_id);
CREATE INDEX IF NOT EXISTS universal_link_idx_both_ids ON universal_link(source_id, dest_id, algorithm_id);
