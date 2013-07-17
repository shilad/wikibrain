CREATE INDEX IF NOT EXISTS local_link_idx_source_id ON local_link(lang_id, source_id);
CREATE INDEX IF NOT EXISTS local_link_idx_dest_id ON local_link(lang_id, dest_id);
