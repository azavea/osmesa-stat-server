package osmesa.server.stats

import java.time.Instant

import cats.effect._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.java8.time._
import osmesa.server._
import osmesa.server.model.{IdNotFoundError, OsmStatError, ResultPage}

case class UserStats(uid: Long,
                     name: Option[String],
                     measurements: Json,
                     counts: Json,
                     lastEdit: Option[Instant],
                     updatedAt: Option[Instant],
                     changesetCount: Option[Int],
                     editCount: Option[Int],
                     editorChangesets: Json,
                     editorEdits: Json,
                     dayChangesets: Json,
                     dayEdits: Json,
                     countryChangesets: Json,
                     countryEdits: Json,
                     hashtagChangesets: Json,
                     hashtagEdits: Json)

object UserStats extends Implicits {
  implicit val userStatsDecoder: Decoder[UserStats] = deriveDecoder
  implicit val userStatsEncoder: Encoder[UserStats] = deriveEncoder

  private val selectF =
    fr"""
      SELECT
        id,
        name,
        coalesce(measurements, '{}'::jsonb) measurements,
        coalesce(counts, '{}'::jsonb) counts,
        last_edit,
        updated_at,
        changeset_count,
        edit_count,
        coalesce(editor_changesets, '{}'::jsonb) editor_changesets,
        coalesce(editor_edits, '{}'::jsonb) editor_edits,
        coalesce(day_changesets, '{}'::jsonb) day_changesets,
        coalesce(day_edits, '{}'::jsonb) day_edits,
        coalesce(country_changesets, '{}'::jsonb) country_changesets,
        coalesce(country_edits, '{}'::jsonb) country_edits,
        coalesce(hashtag_changesets, '{}'::jsonb) hashtag_changesets,
        coalesce(hashtag_edits, '{}'::jsonb) hashtag_edits
      FROM
        user_statistics
    """

  def byId(
    id: Long
  )(implicit xa: Transactor[IO]): IO[Either[OsmStatError, UserStats]] =
    (selectF ++ fr"WHERE id = $id")
      .query[UserStats]
      .option
      .transact(xa)
      .map {
        case Some(user) => Right(user)
        case None       => Left(IdNotFoundError("user", id))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(
    implicit xa: Transactor[IO]
  ): IO[ResultPage[UserStats]] = {
    val offset = (pageNum - 1) * pageSize
    (selectF ++ fr"ORDER BY id ASC LIMIT $pageSize OFFSET $offset")
      .query[UserStats]
      .to[List]
      .map({
        ResultPage(_, pageNum)
      })
      .transact(xa)
  }
}
