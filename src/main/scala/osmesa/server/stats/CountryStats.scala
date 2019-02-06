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

case class CountryStats(
  countryId: Long,
  name: Option[String],
  kmRoadsAdd: Option[Double],
  kmRoadsMod: Option[Double],
  kmWaterwaysAdd: Option[Double],
  kmWaterwaysMod: Option[Double],
  kmCoastlinesAdd: Option[Double],
  kmCoastlinesMod: Option[Double],
  roadsAdd: Option[Int],
  roadsMod: Option[Int],
  waterwaysAdd: Option[Int],
  waterwaysMod: Option[Int],
  coastlinesAdd: Option[Int],
  coastlinesMod: Option[Int],
  buildingsAdd: Option[Int],
  buildingsMod: Option[Int],
  poiAdd: Option[Int],
  poiMod: Option[Int],
  lastEdit: Option[java.sql.Timestamp],
  updatedAt: Option[java.sql.Timestamp],
  changesetCount: Option[Int],
  editCount: Option[Int],
  userEdits: Json,
  hashtagEdits: Json
)

object CountryStats {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val countryStatsDecoder: Decoder[CountryStats] = deriveDecoder
  implicit val countryStatsEncoder: Encoder[CountryStats] = deriveEncoder

  private val selectF = fr"""
      SELECT
        country_id, country_name, road_km_added, road_km_modified, waterway_km_added, waterway_km_modified,
        coastline_km_added, coastline_km_modified, roads_added, roads_modified, waterways_added, waterways_modified,
        coastlines_added, coastlines_modified, buildings_added, buildings_modified, pois_added, pois_modified,
        last_edit, updated_at, changeset_count, edit_count, user_edit_counts, hashtag_edits
      FROM
        country_statistics
    """

  def byId(code: String)(implicit xa: Transactor[IO]): IO[Either[OsmStatError, CountryStats]] =
    (selectF ++ fr"WHERE country_code = $code")
      .query[CountryStats]
      .option
      .transact(xa)
      .map {
        case Some(country) => Right(country)
        case None => Left(IdNotFoundError("country", code))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(implicit xa: Transactor[IO]): IO[ResultPage[CountryStats]] = {
    val offset = pageNum * pageSize + 1
    (selectF ++ fr"ORDER BY id ASC LIMIT $pageSize OFFSET $offset")
      .query[CountryStats]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }
}
