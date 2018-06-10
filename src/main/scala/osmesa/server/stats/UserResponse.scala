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
import io.circe.generic.semiauto._
import fs2._
import fs2.StreamApp.ExitCode
import org.http4s.circe._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.HttpMiddleware
import org.http4s.server.middleware.{GZip, CORS, CORSConfig}
import org.http4s.headers.{Location, `Content-Type`}
import org.postgresql.util.PGobject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class UserResponse(
  id: Long,
  name: Option[String],
  extentUri: Option[String],
  changesets: Option[List[Long]],
  buildingsAdd: Option[Int],
  buildingsMod: Option[Int],
  roadsAdd: Option[Int],
  kmRoadsAdd: Option[Double],
  roadsMod: Option[Int],
  kmRoadsMod: Option[Double],
  waterwaysAdd: Option[Int],
  kmWaterwaysAdd: Option[Double],
  poiAdd: Option[Int],
  changesetCount: Option[Int],
  editCount: Option[Int],
  editors: Json,
  editTimes: Json,
  countryList: Json,
  hashtags: Json
)

object UserResponse {
  implicit val userResponseDecoder: Decoder[UserResponse] = deriveDecoder
  implicit val userResponseEncoder: Encoder[UserResponse] = deriveEncoder

  implicit val JsonMeta: Meta[Json] =
    Meta.other[PGobject]("json").xmap[Json](
      a => parse(a.getValue).leftMap[Json](e => throw e).merge,
      a => {
        val o = new PGobject
        o.setType("json")
        o.setValue(a.noSpaces)
        o
      }
    )

  private val selectF = fr"""
      SELECT
        id, name, extent_uri, changesets, buildings_added, buildings_modified,
        roads_added, road_km_added, roads_modified, road_km_modified, waterways_added,
        waterway_km_added, pois_added, changeset_count, edit_count, editors,
        edit_times, country_list, hashtags
      FROM
        user_statistics
    """

  def byId(id: Long)(implicit xa: Transactor[IO]): IO[Either[OsmStatError, UserResponse]] =
    (selectF ++ fr"WHERE id = $id")
      .query[UserResponse]
      .option
      .transact(xa)
      .map {
        case Some(user) => Right(user)
        case None => Left(IdNotFoundError("user", id))
      }

  def getPage(pageNum: Int)(implicit xa: Transactor[IO]): IO[ResultPage[UserResponse]] = {
    val offset = pageNum * 10 + 1
    (selectF ++ fr"ORDER BY id ASC LIMIT 10 OFFSET $offset")
      .query[UserResponse]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }
}
