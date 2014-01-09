
/* Create geometries table */
CREATE TABLE geometries (
  ref_sys_name VARCHAR(63) NOT NULL,
  layer_name VARCHAR(63) NOT NULL,
  shape_type SMALLINT NOT NULL,
  geom_id INTEGER NOT NULL PRIMARY KEY);
SELECT AddGeometryColumn('public','geometries','geometry',-1,'GEOMETRY',2);

/* Add indices to geometries table */
CREATE INDEX rs_layer_type ON geometries (ref_sys_name, layer_name, shape_type);
CREATE INDEX geometry_index ON geometries USING GIST ( geometry );

/* Create spatiotag table */
CREATE TABLE spatiotags (
  local_id INTEGER NOT NULL,
  lang_id SMALLINT NOT NULL,
  geom_id INTEGER NOT NULL PRIMARY KEY);

/*Add indices to spatio tag table*/
CREATE INDEX geom_lookup ON spatiotags (local_id, lang_id);
