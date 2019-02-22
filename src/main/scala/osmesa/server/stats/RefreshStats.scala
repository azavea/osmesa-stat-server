package osmesa.server.stats

import osmesa.server.model._

import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.jawn._
import io.circe.syntax._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import fs2._
import org.http4s.circe._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.HttpMiddleware
import org.http4s.server.middleware.{GZip, CORS, CORSConfig}
import org.http4s.headers.{Location, `Content-Type`}
import org.postgresql.util.PGobject

import scala.concurrent.duration._

case class RefreshTime(
  view: Option[String],
  updatedAt: Option[java.sql.Timestamp]
)

case class RefreshStats(
  userStatsRefresh: Option[java.sql.Timestamp],
  countryStatsRefresh: Option[java.sql.Timestamp],
  hashtagStatsRefresh: Option[java.sql.Timestamp]
) {
  def +(that: RefreshStats) = {
    RefreshStats(
      (userStatsRefresh.toList ++ that.userStatsRefresh).headOption,
      (countryStatsRefresh.toList ++ that.countryStatsRefresh).headOption,
      (hashtagStatsRefresh.toList ++ that.hashtagStatsRefresh).headOption
    )
  }
}

object RefreshStats {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  implicit val refreshStatsDecoder: Decoder[RefreshStats] = deriveDecoder
  implicit val refreshStatsEncoder: Encoder[RefreshStats] = deriveEncoder

  private val selectF = fr"""
      SELECT
        mat_view, updated_at
      FROM
        refreshments
    """

  def apply(arg: RefreshTime): RefreshStats = arg.view.get match {
    case "user_statistics" => RefreshStats(arg.updatedAt, None, None)
    case "country_statistics" => RefreshStats(None, arg.updatedAt, None)
    case "hashtag_statistics" => RefreshStats(None, None, arg.updatedAt)
  }

  def getCurrentStatus()(implicit xa: Transactor[IO]): IO[RefreshStats] =
    (selectF)
      .query[RefreshTime]
      .to[List]
      .transact(xa)
      .map(_.map(RefreshStats.apply(_)).reduce(_+_))

}
