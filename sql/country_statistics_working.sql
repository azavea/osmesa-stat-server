CREATE MATERIALIZED VIEW country_statistics AS
  WITH country_counts AS (
    SELECT cc.changeset_id,
        countries.id AS country_id,
        countries.name AS country_name,
        cc.edit_count
      FROM (changesets_countries cc
        JOIN countries ON ((cc.country_id = countries.id)))
    ), agg_stats AS (
    SELECT cc.country_id,
        cc.country_name,
        array_agg(chg.id) AS changesets,
        sum(chg.road_km_added) AS road_km_added,
        sum(chg.road_km_modified) AS road_km_modified,
        sum(chg.waterway_km_added) AS waterway_km_added,
        sum(chg.waterway_km_modified) AS waterway_km_modified,
        sum(chg.coastline_km_added) AS coastline_km_added,
        sum(chg.coastline_km_modified) AS coastline_km_modified,
        sum(chg.roads_added) AS roads_added,
        sum(chg.roads_modified) AS roads_modified,
        sum(chg.waterways_added) AS waterways_added,
        sum(chg.waterways_modified) AS waterways_modified,
        sum(chg.coastlines_added) AS coastlines_added,
        sum(chg.coastlines_modified) AS coastlines_modified,
        sum(chg.buildings_added) AS buildings_added,
        sum(chg.buildings_modified) AS buildings_modified,
        sum(chg.pois_added) AS pois_added,
        sum(chg.pois_modified) AS pois_modified,
        max(coalesce(chg.closed_at, chg.created_at)) AS last_edit,
        max(COALESCE(chg.closed_at, chg.created_at, chg.updated_at)) AS updated_at,
        count(*) AS changeset_count,
        sum(cc.edit_count) as edit_count
      FROM (changesets chg
        JOIN country_counts cc ON ((cc.changeset_id = chg.id)))
      GROUP BY cc.country_id, cc.country_name
    )
  SELECT agg_stats.country_id,
     agg_stats.country_name,
     agg_stats.road_km_added,
     agg_stats.road_km_modified,
     agg_stats.waterway_km_added,
     agg_stats.waterway_km_modified,
     agg_stats.coastline_km_added,
     agg_stats.coastline_km_modified,
     agg_stats.roads_added,
     agg_stats.roads_modified,
     agg_stats.waterways_added,
     agg_stats.waterways_modified,
     agg_stats.coastlines_added,
     agg_stats.coastlines_modified,
     agg_stats.buildings_added,
     agg_stats.buildings_modified,
     agg_stats.pois_added,
     agg_stats.pois_modified,
     agg_stats.last_edit,
     agg_stats.updated_at,
     agg_stats.changeset_count,
     agg_stats.edit_count
    FROM agg_stats;

CREATE UNIQUE INDEX country_statistics_id ON country_statistics(country_id);
