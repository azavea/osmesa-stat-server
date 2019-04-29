CREATE MATERIALIZED VIEW hashtag_statistics AS
  WITH hashtag_join AS (
    SELECT chg.id,
        chg.road_km_added,
        chg.road_km_modified,
        chg.road_km_deleted,
        chg.waterway_km_added,
        chg.waterway_km_modified,
        chg.waterway_km_deleted,
        chg.coastline_km_added,
        chg.coastline_km_modified,
        chg.coastline_km_deleted,
        chg.roads_added,
        chg.roads_modified,
        chg.roads_deleted,
        chg.waterways_added,
        chg.waterways_modified,
        chg.waterways_deleted,
        chg.coastlines_added,
        chg.coastlines_modified,
        chg.coastlines_deleted,
        chg.buildings_added,
        chg.buildings_modified,
        chg.buildings_deleted,
        chg.pois_added,
        chg.pois_modified,
        chg.pois_deleted,
        chg.editor,
        chg.user_id,
        chg.created_at,
        chg.closed_at,
        chg.augmented_diffs,
        chg.updated_at,
        ch.hashtag_id
      FROM (changesets chg
        JOIN changesets_hashtags ch ON ((ch.changeset_id = chg.id)))
    ), tag_usr_counts AS (
    SELECT hj.hashtag_id,
        array_agg(DISTINCT users.name) AS names,
        users.id AS uid,
        count(*) AS edit_count
      FROM (users
        JOIN hashtag_join hj ON ((hj.user_id = users.id)))
      WHERE users.id <> 0
      GROUP BY hj.hashtag_id, users.id
    ), hashtag_usr_counts AS (
    SELECT hj.hashtag_id,
        users.uid AS uid,
        array_agg(DISTINCT users.names) AS names,
        sum(hj.road_km_added) as road_km_added,
        sum(hj.road_km_modified) as road_km_modified,
        sum(hj.road_km_deleted) as road_km_deleted,
        sum(hj.waterway_km_added) as waterway_km_added,
        sum(hj.waterway_km_modified) as waterway_km_modified,
        sum(hj.waterway_km_deleted) as waterway_km_deleted,
        sum(hj.coastline_km_added) as coastline_km_added,
        sum(hj.coastline_km_modified) as coastline_km_modified,
        sum(hj.coastline_km_deleted) as coastline_km_deleted,
        sum(hj.roads_added) as roads_added,
        sum(hj.roads_modified) as roads_modified,
        sum(hj.roads_deleted) as roads_deleted,
        sum(hj.waterways_added) as waterways_added,
        sum(hj.waterways_modified) as waterways_modified,
        sum(hj.waterways_deleted) as waterways_deleted,
        sum(hj.coastlines_added) as coastlines_added,
        sum(hj.coastlines_modified) as coastlines_modified,
        sum(hj.coastlines_deleted) as coastlines_deleted,
        sum(hj.buildings_added) as buildings_added,
        sum(hj.buildings_modified) as buildings_modified,
        sum(hj.buildings_deleted) as buildings_deleted,
        sum(hj.pois_added) as pois_added,
        sum(hj.pois_modified) as pois_modified,
        sum(hj.pois_deleted) as pois_deleted,
        count(*) AS edit_count
      FROM (tag_usr_counts users
        JOIN hashtag_join hj ON ((hj.user_id = users.uid AND hj.hashtag_id = users.hashtag_id)))
      GROUP BY hj.hashtag_id, users.uid
    ), usr_json_agg AS (
    SELECT usr_counts.hashtag_id,
        json_agg(json_build_object('name', usr_counts.names[1],
                                   'uid', usr_counts.uid,
                                   'km_roads_add', usr_counts.road_km_added,
                                   'km_roads_mod', usr_counts.road_km_modified,
                                   'km_roads_del', usr_counts.road_km_deleted,
                                   'km_waterways_add', usr_counts.waterway_km_added,
                                   'km_waterways_mod', usr_counts.waterway_km_modified,
                                   'km_waterways_del', usr_counts.waterway_km_deleted,
                                   'km_coastlines_add', usr_counts.coastline_km_added,
                                   'km_coastlines_mod', usr_counts.coastline_km_modified,
                                   'km_coastlines_del', usr_counts.coastline_km_deleted,
                                   'roads_add', usr_counts.roads_added,
                                   'roads_mod', usr_counts.roads_modified,
                                   'roads_del', usr_counts.roads_deleted,
                                   'waterways_add', usr_counts.waterways_added,
                                   'waterways_mod', usr_counts.waterways_modified,
                                   'waterways_del', usr_counts.waterways_deleted,
                                   'coastlines_add', usr_counts.coastlines_added,
                                   'coastlines_mod', usr_counts.coastlines_modified,
                                   'coastlines_del', usr_counts.coastlines_deleted,
                                   'buildings_add', usr_counts.buildings_added,
                                   'buildings_mod', usr_counts.buildings_modified,
                                   'buildings_del', usr_counts.buildings_deleted,
                                   'poi_add', usr_counts.pois_added,
                                   'poi_mod', usr_counts.pois_modified,
                                   'poi_del', usr_counts.pois_deleted,
                                   'edits', usr_counts.edit_count)) AS users
      FROM hashtag_usr_counts usr_counts
      GROUP BY usr_counts.hashtag_id
    ), without_json AS (
    SELECT ht.hashtag AS tag,
        ht.id AS hashtag_id,
        (('hashtag/'::text || ht.hashtag) || '/{z}/{x}/{y}.mvt'::text) AS extent_uri,
        sum(hashtag_join.buildings_added) AS buildings_added,
        sum(hashtag_join.buildings_modified + hashtag_join.buildings_deleted) AS buildings_modified,
        sum(hashtag_join.roads_added) AS roads_added,
        sum(hashtag_join.road_km_added) AS road_km_added,
        sum(hashtag_join.roads_modified + hashtag_join.roads_deleted) AS roads_modified,
        sum(hashtag_join.road_km_modified + hashtag_join.road_km_deleted) AS road_km_modified,
        sum(hashtag_join.waterways_added) AS waterways_added,
        sum(hashtag_join.waterway_km_added) AS waterway_km_added,
        sum(hashtag_join.waterways_modified + hashtag_join.waterways_deleted) AS waterways_modified,
        sum(hashtag_join.waterway_km_modified + hashtag_join.waterway_km_deleted) AS waterway_km_modified,
        sum(hashtag_join.coastlines_added) AS coastlines_added,
        sum(hashtag_join.coastline_km_added) AS coastline_km_added,
        sum(hashtag_join.coastlines_modified + hashtag_join.coastlines_deleted) AS coastlines_modified,
        sum(hashtag_join.coastline_km_modified + hashtag_join.coastline_km_deleted) AS coastline_km_modified,
        sum(hashtag_join.pois_added) AS pois_added,
        sum(hashtag_join.pois_modified + hashtag_join.pois_deleted) AS pois_modified
      FROM (hashtags ht
        JOIN hashtag_join ON ((ht.id = hashtag_join.hashtag_id)))
      GROUP BY ht.id, ht.hashtag
    )
  SELECT without_json.tag,
     without_json.hashtag_id,
     without_json.extent_uri,
     without_json.buildings_added,
     without_json.buildings_modified,
     without_json.roads_added,
     without_json.road_km_added,
     without_json.roads_modified,
     without_json.road_km_modified,
     without_json.waterways_added,
     without_json.waterway_km_added,
     without_json.waterways_modified,
     without_json.waterway_km_modified,
     without_json.coastlines_added,
     without_json.coastline_km_added,
     without_json.coastlines_modified,
     without_json.coastline_km_modified,
     without_json.pois_added,
     without_json.pois_modified,
     usr_json_agg.users
    FROM (without_json
      JOIN usr_json_agg ON ((without_json.hashtag_id = usr_json_agg.hashtag_id)));

CREATE UNIQUE INDEX hashtag_statistics_hashtag_id ON hashtag_statistics(hashtag_id);
