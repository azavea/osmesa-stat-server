package osmesa.server.stats

import java.time.Instant

import cats.effect._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.java8.time._
import osmesa.server._
import osmesa.server.model._

case class CountryStats(countryId: Long,
                        name: Option[String],
                        code: Option[String],
                        measurements: Json,
                        counts: Json,
                        lastEdit: Option[Instant],
                        updatedAt: Option[Instant],
                        changesetCount: Option[Int],
                        editCount: Option[Int],
                        userEdits: Json,
                        hashtagEdits: Json)

object CountryStats extends Implicits {
  implicit val countryStatsDecoder: Decoder[CountryStats] = deriveDecoder
  implicit val countryStatsEncoder: Encoder[CountryStats] = deriveEncoder

  private val selectF =
    fr"""
      SELECT
        country_id,
        country_name,
        country_code,
        coalesce(measurements, '{}'::jsonb) measurements,
        coalesce(counts, '{}'::jsonb) counts,
        last_edit,
        updated_at,
        changeset_count,
        edit_count,
        coalesce(user_edit_counts, '{}'::jsonb) user_edit_counts,
        coalesce(hashtag_edits, '{}'::jsonb) hashtag_edits
      FROM
        country_statistics
    """

  def byId(
    code: String
  )(implicit xa: Transactor[IO]): IO[Either[OsmStatError, CountryStats]] =
    (selectF ++ fr"WHERE country_code = $code")
      .query[CountryStats]
      .option
      .transact(xa)
      .map {
        case Some(country) => Right(country)
        case None          => Left(IdNotFoundError("country", code))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(
    implicit xa: Transactor[IO]
  ): IO[ResultPage[CountryStats]] = {
    val offset = (pageNum - 1) * pageSize
    (selectF ++ fr"ORDER BY id ASC LIMIT $pageSize OFFSET $offset")
      .query[CountryStats]
      .to[List]
      .map({
        ResultPage(_, pageNum)
      })
      .transact(xa)
  }
}
