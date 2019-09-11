package osmesa.server.stats

import java.time.Instant

import cats.effect._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.java8.time._
import osmesa.server.Implicits

case class RefreshTime(view: Option[String], updatedAt: Option[Instant])

case class RefreshStats(userStatsRefresh: Option[Instant],
                        countryStatsRefresh: Option[Instant],
                        hashtagStatsRefresh: Option[Instant]) {
  def +(that: RefreshStats): RefreshStats = {
    RefreshStats(
      (userStatsRefresh.toList ++ that.userStatsRefresh).headOption,
      (countryStatsRefresh.toList ++ that.countryStatsRefresh).headOption,
      (hashtagStatsRefresh.toList ++ that.hashtagStatsRefresh).headOption
    )
  }
}

object RefreshStats extends Implicits {
  implicit val refreshStatsDecoder: Decoder[RefreshStats] = deriveDecoder
  implicit val refreshStatsEncoder: Encoder[RefreshStats] = deriveEncoder

  private val selectF = fr"""
      SELECT
        mat_view, updated_at
      FROM
        refreshments
    """

  def getCurrentStatus()(implicit xa: Transactor[IO]): IO[RefreshStats] =
    selectF
      .query[RefreshTime]
      .to[List]
      .transact(xa)
      .map(_.map(RefreshStats.apply).reduce(_ + _))

  def apply(arg: RefreshTime): RefreshStats = arg.view.get match {
    case "user_statistics"    => RefreshStats(arg.updatedAt, None, None)
    case "country_statistics" => RefreshStats(None, arg.updatedAt, None)
    case "hashtag_statistics" => RefreshStats(None, None, arg.updatedAt)
  }

}
