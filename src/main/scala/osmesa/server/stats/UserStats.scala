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
                     extentUri: Option[String],
                     measurements: Json,
                     counts: Json,
                     lastEdit: Option[Instant],
                     changesetCount: Option[Int],
                     editCount: Option[Int],
                     editors: Json,
                     editTimes: Json,
                     countries: Json,
                     hashtags: Json)

object UserStats extends Implicits {
  implicit val userStatsDecoder: Decoder[UserStats] = deriveDecoder
  implicit val userStatsEncoder: Encoder[UserStats] = deriveEncoder

  private val selectF =
    fr"""
      SELECT
        id,
        name,
        extent_uri,
        coalesce(measurements, '{}'::jsonb) measurements,
        coalesce(counts, '{}'::jsonb) counts,
        last_edit,
        changeset_count,
        edit_count,
        coalesce(editors, '{}'::jsonb) editors,
        coalesce(edit_times, '{}'::jsonb) edit_times,
        coalesce(countries, '{}'::jsonb) countries,
        coalesce(hashtags, '{}'::jsonb) hashtags
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
