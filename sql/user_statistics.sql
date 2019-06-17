CREATE MATERIALIZED VIEW user_statistics AS
  WITH general AS (
    SELECT
      user_id,
      array_agg(id) changesets,
      max(coalesce(closed_at, created_at)) last_edit,
      count(*) changeset_count,
      sum(total_edits) edit_count,
      max(updated_at) updated_at
    FROM changesets
    GROUP BY user_id
  ),
  country_counts AS (
    SELECT
      user_id,
      code,
      count(*) changesets
    FROM changesets
    JOIN changesets_countries ON changesets.id = changesets_countries.changeset_id
    JOIN countries ON changesets_countries.country_id = countries.id
    GROUP BY user_id, code
  ),
  countries AS (
    SELECT
      user_id,
      jsonb_object_agg(code, changesets) countries
    FROM country_counts
    GROUP BY user_id
  ),
  edit_time_counts AS (
    SELECT
      user_id,
      date_trunc('day', coalesce(closed_at, created_at))::date AS day,
      count(*) changesets
    FROM changesets
    GROUP BY user_id, day
  ),
  edit_times AS (
    SELECT
      user_id,
      jsonb_object_agg(day, changesets) edit_times
    FROM edit_time_counts
    GROUP BY user_id
  ),
  editor_counts AS (
    SELECT
      user_id,
      editor,
      count(*) changesets
    FROM changesets
    WHERE editor IS NOT NULL
    GROUP BY user_id, editor
  ),
  editors AS (
    SELECT
      user_id,
      jsonb_object_agg(editor, changesets) editors
    FROM editor_counts
    GROUP BY user_id
  ),
  hashtag_counts AS (
    SELECT
      user_id,
      hashtag,
      count(*) changesets
    FROM changesets
    JOIN changesets_hashtags ON changesets.id = changesets_hashtags.changeset_id
    JOIN hashtags ON changesets_hashtags.hashtag_id = hashtags.id
    GROUP BY user_id, hashtag
  ),
  hashtags AS (
    SELECT
      user_id,
      jsonb_object_agg(hashtag, changesets) hashtags
    FROM hashtag_counts
    GROUP BY user_id
  ),
  measurements AS (
    SELECT
      id,
      user_id,
      key,
      value
    FROM changesets
    CROSS JOIN LATERAL jsonb_each(measurements)
  ),
  aggregated_measurements_kv AS (
    SELECT
      user_id,
      key,
      sum(value::numeric) AS value
    FROM measurements
    GROUP BY user_id, key
  ),
  aggregated_measurements AS (
    SELECT
      user_id,
      jsonb_object_agg(key, value) measurements
    FROM aggregated_measurements_kv
    GROUP BY user_id
  ),
  counts AS (
    SELECT
      id,
      user_id,
      key,
      value
    FROM changesets
    CROSS JOIN LATERAL jsonb_each(counts)
  ),
  aggregated_counts_kv AS (
    SELECT
      user_id,
      key,
      sum(value::numeric) AS value
    FROM counts
    GROUP BY user_id, key
  ),
  aggregated_counts AS (
    SELECT
      user_id,
      jsonb_object_agg(key, value) counts
    FROM aggregated_counts_kv
    GROUP BY user_id
  )
  SELECT
    user_id AS id,
    users.name,
    'user/' || users.id || '/{z}/{x}/{y}.mvt' AS extent_uri,
    -- TODO this is unbounded; drop it?
    changesets,
    measurements,
    counts,
    last_edit,
    changeset_count,
    edit_count,
    -- TODO this is unbounded; top N?
    editors,
    edit_times,
    -- TODO top N?
    countries,
    -- TODO top N?
    hashtags,
    updated_at
  FROM general
  LEFT OUTER JOIN countries USING (user_id)
  LEFT OUTER JOIN editors USING (user_id)
  LEFT OUTER JOIN edit_times USING (user_id)
  LEFT OUTER JOIN hashtags USING (user_id)
  LEFT OUTER JOIN aggregated_measurements USING (user_id)
  LEFT OUTER JOIN aggregated_counts USING (user_id)
  JOIN users ON user_id = users.id;

CREATE UNIQUE INDEX user_statistics_id ON user_statistics(id);
