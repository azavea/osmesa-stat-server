CREATE MATERIALIZED VIEW hashtag_statistics AS
  WITH general AS (
    SELECT
      hashtag_id,
      max(coalesce(closed_at, created_at)) last_edit,
      count(*) changeset_count,
      sum(total_edits) edit_count,
      max(updated_at) updated_at
    FROM changesets
    JOIN changesets_hashtags ON changesets.id = changesets_hashtags.changeset_id
    GROUP BY hashtag_id
  ),
  processed_changesets AS (
    SELECT
      id,
      user_id,
      hashtag_id,
      measurements,
      counts
    FROM changesets
    JOIN changesets_hashtags ON changesets.id = changesets_hashtags.changeset_id
  ),
  user_counts AS (
    SELECT
      -- TODO rank by edit count?
      RANK() OVER (PARTITION BY hashtag_id ORDER BY count(*) DESC) AS rank,
      hashtag_id,
      user_id,
      -- TODO expose edit count instead?
      count(*) changesets
    FROM processed_changesets
    GROUP BY hashtag_id, user_id
  ),
  users AS (
    SELECT
      hashtag_id,
      json_object_agg(user_id, changesets) users
    FROM user_counts
    WHERE rank <= 10
    GROUP BY hashtag_id
  ),
  measurements AS (
    SELECT
      id,
      hashtag_id,
      key,
      value
    FROM processed_changesets
    CROSS JOIN LATERAL jsonb_each(measurements)
  ),
  aggregated_measurements_kv AS (
    SELECT
      hashtag_id,
      key,
      sum(value::numeric) AS value
    FROM measurements
    GROUP BY hashtag_id, key
  ),
  aggregated_measurements AS (
    SELECT
      hashtag_id,
      json_object_agg(key, value) measurements
    FROM aggregated_measurements_kv
    GROUP BY hashtag_id
  ),
  counts AS (
    SELECT
      id,
      hashtag_id,
      key,
      value
    FROM processed_changesets
    CROSS JOIN LATERAL jsonb_each(counts)
  ),
  aggregated_counts_kv AS (
    SELECT
      hashtag_id,
      key,
      sum(value::numeric) AS value
    FROM counts
    GROUP BY hashtag_id, key
  ),
  aggregated_counts AS (
    SELECT
      hashtag_id,
      json_object_agg(key, value) counts
    FROM aggregated_counts_kv
    GROUP BY hashtag_id
  )
  SELECT
    hashtags.hashtag tag,
    general.hashtag_id,
    'hashtag/' || general.hashtag_id || '/{z}/{x}/{y}.mvt' AS extent_uri,
    measurements,
    counts,
    general.changeset_count,
    general.edit_count,
    general.last_edit,
    general.updated_at,
    users
  FROM general
  JOIN hashtags ON hashtag_id = hashtags.id
  LEFT OUTER JOIN users USING (hashtag_id)
  LEFT OUTER JOIN aggregated_measurements USING (hashtag_id)
  LEFT OUTER JOIN aggregated_counts USING (hashtag_id);

CREATE UNIQUE INDEX hashtag_statistics_hashtag_id ON hashtag_statistics(hashtag_id);