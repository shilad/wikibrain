CREATE INDEX IF NOT EXISTS UNIVERSAL_SKELETAL_LINK_IDX_SOURCE_ID ON UNIVERSAL_SKELETAL_LINK(SOURCE_ID, ALGORITHM_ID);
CREATE INDEX IF NOT EXISTS UNIVERSAL_SKELETAL_LINK_IDX_DEST_ID ON UNIVERSAL_SKELETAL_LINK(DEST_ID, ALGORITHM_ID);
CREATE INDEX IF NOT EXISTS UNIVERSAL_SKELETAL_LINK_IDX_BOTH_IDS ON UNIVERSAL_SKELETAL_LINK(SOURCE_ID, DEST_ID, ALGORITHM_ID)