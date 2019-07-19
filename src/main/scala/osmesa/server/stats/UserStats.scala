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


case class UserStats(
  uid: Long,
  name: Option[String],
  extentUri: Option[String],
  kmRoadsAdd: Option[Double],
  kmRoadsMod: Option[Double],
  kmRoadsDel: Option[Double],
  kmWaterwaysAdd: Option[Double],
  kmWaterwaysMod: Option[Double],
  kmWaterwaysDel: Option[Double],
  kmCoastlinesAdd: Option[Double],
  kmCoastlinesMod: Option[Double],
  kmCoastlinesDel: Option[Double],
  roadsAdd: Option[Int],
  roadsMod: Option[Int],
  roadsDel: Option[Int],
  waterwaysAdd: Option[Int],
  waterwaysMod: Option[Int],
  waterwaysDel: Option[Int],
  coastlinesAdd: Option[Int],
  coastlinesMod: Option[Int],
  coastlinesDel: Option[Int],
  buildingsAdd: Option[Int],
  buildingsMod: Option[Int],
  buildingsDel: Option[Int],
  poiAdd: Option[Int],
  poiMod: Option[Int],
  poiDel: Option[Int],
  lastEdit: Option[java.sql.Timestamp],
  changesetCount: Option[Int],
  editCount: Option[Int],
  editors: Json,
  editTimes: Json,
  countryList: Json,
  hashtags: Json
)

object UserStats {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val userStatsDecoder: Decoder[UserStats] = deriveDecoder
  implicit val userStatsEncoder: Encoder[UserStats] = deriveEncoder

  private val selectF = fr"""
      SELECT
        id,
        name,
        extent_uri,
        road_km_added,
        road_km_modified,
        road_km_deleted,
        waterway_km_added,
        waterway_km_modified,
        waterway_km_deleted,
        coastline_km_added,
        coastline_km_modified,
        coastline_km_deleted,
        roads_added,
        roads_modified,
        roads_deleted,
        waterways_added,
        waterways_modified,
        waterways_deleted,
        coastlines_added,
        coastlines_modified,
        coastlines_deleted,
        buildings_added,
        buildings_modified,
        buildings_deleted,
        pois_added,
        pois_modified,
        pois_deleted,
        last_edit,
        changeset_count,
        edit_count,
        editors,
        edit_times,
        country_list,
        hashtags
      FROM
        user_statistics
    """

  def byId(id: Long)(implicit xa: Transactor[IO]): IO[Either[OsmStatError, UserStats]] =
    (selectF ++ fr"WHERE id = $id")
      .query[UserStats]
      .option
      .transact(xa)
      .map {
        case Some(user) => Right(user)
        case None => Left(IdNotFoundError("user", id))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(implicit xa: Transactor[IO]): IO[ResultPage[UserStats]] = {
    val offset = pageNum * pageSize + 1
    (selectF ++ fr"ORDER BY id ASC LIMIT $pageSize OFFSET $offset")
      .query[UserStats]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }
}
