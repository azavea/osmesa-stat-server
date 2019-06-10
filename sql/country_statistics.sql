CREATE MATERIALIZED VIEW country_statistics AS
  WITH changesets AS (
    SELECT
      *
    FROM changesets
    -- ignore users 0 and 1
    WHERE user_id > 1
  ),
  general AS (
    SELECT
      country_id,
      max(coalesce(closed_at, created_at)) last_edit,
      count(*) changeset_count,
      sum(edit_count) edit_count,
      max(updated_at) updated_at
    FROM changesets
    JOIN changesets_countries ON changesets.id = changesets_countries.changeset_id
    GROUP BY country_id
  ),
  processed_changesets AS (
    SELECT
      -- TODO include changesets_countries.edit_count as an alternative to changeset count
      id,
      user_id,
      country_id,
      measurements,
      counts
    FROM changesets
    JOIN changesets_countries ON changesets.id = changesets_countries.changeset_id
  ),
  hashtag_counts AS (
    SELECT
      -- TODO rank by edit count?
      RANK() OVER (PARTITION BY country_id ORDER BY count(*) DESC) AS rank,
      country_id,
      hashtag,
      -- TODO expose edit count instead?
      count(*) changesets
    FROM processed_changesets
    JOIN changesets_hashtags ON processed_changesets.id = changesets_hashtags.changeset_id
    JOIN hashtags ON changesets_hashtags.hashtag_id = hashtags.id
    GROUP BY country_id, hashtag
  ),
  hashtags AS (
    SELECT
      country_id,
      json_object_agg(hashtag, changesets) hashtags
    FROM hashtag_counts
    WHERE rank <= 10
    GROUP BY country_id
  ),
  user_counts AS (
    SELECT
      -- TODO rank by edit count?
      RANK() OVER (PARTITION BY country_id ORDER BY count(*) DESC) AS rank,
      country_id,
      user_id,
      -- TODO expose edit count instead?
      count(*) changesets
    FROM processed_changesets
    GROUP BY country_id, user_id
  ),
  users AS (
    SELECT
      country_id,
      json_object_agg(user_id, changesets) users
    FROM user_counts
    WHERE rank <= 10
    GROUP BY country_id
  ),
  measurements AS (
    SELECT
      id,
      country_id,
      key,
      value
    FROM processed_changesets
    CROSS JOIN LATERAL jsonb_each(measurements)
  ),
  aggregated_measurements_kv AS (
    SELECT
      country_id,
      key,
      sum(value::numeric) AS value
    FROM measurements
    GROUP BY country_id, key
  ),
  aggregated_measurements AS (
    SELECT
      country_id,
      json_object_agg(key, value) measurements
    FROM aggregated_measurements_kv
    GROUP BY country_id
  ),
  counts AS (
    SELECT
      id,
      country_id,
      key,
      value
    FROM processed_changesets
    CROSS JOIN LATERAL jsonb_each(counts)
  ),
  aggregated_counts_kv AS (
    SELECT
      country_id,
      key,
      sum(value::numeric) AS value
    FROM counts
    GROUP BY country_id, key
  ),
  aggregated_counts AS (
    SELECT
      country_id,
      json_object_agg(key, value) counts
    FROM aggregated_counts_kv
    GROUP BY country_id
  )
  SELECT
    general.country_id,
    countries.name country_name,
    countries.code country_code,
    -- NOTE these are per-changeset, not per-country, so stats are double-counted
    measurements,
    -- NOTE these are per-changeset, not per-country, so stats are double-counted
    counts,
    general.changeset_count,
    general.edit_count,
    general.last_edit,
    general.updated_at,
    users user_edit_counts,
    hashtags hashtag_edits
  FROM general
  JOIN countries ON country_id = countries.id
  LEFT OUTER JOIN users USING (country_id)
  LEFT OUTER JOIN hashtags USING (country_id)
  LEFT OUTER JOIN aggregated_measurements USING (country_id)
  LEFT OUTER JOIN aggregated_counts USING (country_id);

CREATE UNIQUE INDEX country_statistics_id ON country_statistics(country_code);
