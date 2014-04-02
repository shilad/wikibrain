
/* Create geometries table */
CREATE TABLE geometries (
  item_id int not null,
  ref_sys_name VARCHAR(63) NOT NULL,
  layer_name VARCHAR(63) NOT NULL);
SELECT AddGeometryColumn('public','geometries','geometry',-1,'GEOMETRY',2);

/* Add indices to geometries table */
CREATE INDEX rs_layer_type ON geometries (item_id, layer_name, ref_sys_name);
CREATE INDEX geometry_index ON geometries USING GIST ( geometry );


/*
/* Create spatiotag table */
CREATE TABLE spatiotags (
  local_id INTEGER NOT NULL PRIMARY KEY,
  lang_id SMALLINT NOT NULL PRIMARY KEY,
  geom_id INTEGER NOT NULL);

/*Add indices to spatio tag table*/
CREATE INDEX geom_lookup ON spatiotags (local_id, lang_id);
*/
