CREATE MATERIALIZED VIEW country_statistics AS
  WITH country_counts AS (
    -- Collect country-related changesets with edit counts and associate with hashtags
    SELECT cc.changeset_id,
        countries.id,
        countries.code,
        countries.name AS country_name,
        cc.edit_count,
        hts.hashtag_id
      FROM ((changesets_countries cc
        JOIN countries ON ((cc.country_id = countries.id)))
        FULL OUTER JOIN changesets_hashtags hts ON (hts.changeset_id = cc.changeset_id))
    ), user_edits AS (
      -- Associate user ids with changesets/edit count
      SELECT c_chg.country_id,
          c_chg.edit_count,
          c.user_id
        FROM (changesets_countries c_chg
          JOIN changesets c ON (c.id = c_chg.changeset_id))
    ), country_edits AS (
      -- Aggregate edit counts per user (ignore UID 0)
      SELECT country_id,
          user_id,
          sum(edit_count) AS edits
        FROM user_edits
        WHERE user_id <> 0
        GROUP BY country_id, user_id
    ), grouped_user_edits AS (
      -- Rank user edit totals per country
      SELECT *,
          ROW_NUMBER() OVER (PARTITION BY country_id ORDER BY edits DESC) as rank
        FROM country_edits
    ), json_country_edits AS (
      -- Collapse top ten user edit totals into JSON object
      SELECT country_id,
          json_agg(json_build_object('user', user_id, 'count', edits)) AS edits
        FROM grouped_user_edits
        WHERE rank <= 10
        GROUP BY country_id
    ), ht_edits AS (
      -- Associate hashtags with aggregate edit counts per country
      SELECT cc.id as country_id,
          hts.hashtag,
          sum(edit_count) as edit_count
        FROM (country_counts cc
          JOIN hashtags hts ON cc.hashtag_id = hts.id)
        GROUP BY cc.id, hts.hashtag
    ), grouped_hts AS (
      -- Rank edit counts per country
      SELECT *,
          ROW_NUMBER() OVER (PARTITION BY country_id ORDER BY edit_count DESC) as rank
        FROM ht_edits
    ), ht_json AS (
      -- Collapse top ten most active hashtags per country into JSON object
      SELECT country_id,
          json_agg(json_build_object('hashtag', hashtag, 'count', edit_count)) as hashtag_edits
        FROM grouped_hts
        WHERE rank <= 10
        GROUP BY country_id
    ), agg_stats AS (
      -- Aggregate statistics per country
      SELECT cc.id as country_id,
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
          sum(cc.edit_count) AS edit_count
        FROM (changesets chg
          JOIN country_counts cc ON ((cc.changeset_id = chg.id)))
        GROUP BY cc.id
    )
  SELECT agg.country_id,
      countries.name AS country_name,
      countries.code AS country_code,
      agg.road_km_added,
      agg.road_km_modified,
      agg.waterway_km_added,
      agg.waterway_km_modified,
      agg.coastline_km_added,
      agg.coastline_km_modified,
      agg.roads_added,
      agg.roads_modified,
      agg.waterways_added,
      agg.waterways_modified,
      agg.coastlines_added,
      agg.coastlines_modified,
      agg.buildings_added,
      agg.buildings_modified,
      agg.pois_added,
      agg.pois_modified,
      agg.last_edit,
      agg.updated_at,
      agg.changeset_count,
      agg.edit_count,
      jce.edits AS user_edit_counts,
      hts.hashtag_edits
    FROM (agg_stats agg
      FULL OUTER JOIN json_country_edits jce ON (agg.country_id = jce.country_id)
      FULL OUTER JOIN ht_json hts ON (agg.country_id = hts.country_id)
      JOIN countries ON agg.country_id = countries.id);

CREATE UNIQUE INDEX country_statistics_id ON country_statistics(country_code);
