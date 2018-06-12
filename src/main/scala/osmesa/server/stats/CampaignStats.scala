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
import org.postgresql.util.PGobject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


case class HashtagStats(
  tag: String,
  extentUri: Option[String],
  buildingsAdd: Option[Int],
  buildingsMod: Option[Int],
  roadsAdd: Option[Int],
  kmRoadsAdd: Option[Double],
  roadsMod: Option[Int],
  kmRoadsMod: Option[Double],
  waterwaysAdd: Option[Int],
  kmWaterwaysAdd: Option[Double],
  waterwaysMod: Option[Int],
  kmWaterwaysMod: Option[Double],
  poiAdd: Option[Int],
  poiMod: Option[Int],
  users: Json
)

// uid, name, edits

object HashtagStats {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val userStatsDecoder: Decoder[HashtagStats] = deriveDecoder
  implicit val userStatsEncoder: Encoder[HashtagStats] = deriveEncoder

  private val selectF = fr"""
      SELECT
        tag, extent_uri, buildings_added, buildings_modified,
        roads_added, road_km_added, roads_modified, road_km_modified, waterways_added,
        waterway_km_added, waterways_km_modified, pois_added, pois_modified, users
      FROM
        hashtag_statistics
    """

  def byTag(tag: String)(implicit xa: Transactor[IO]): IO[Either[OsmStatError, HashtagStats]] =
    (selectF ++ fr"WHERE tag = $tag")
      .query[HashtagStats]
      .option
      .transact(xa)
      .map {
        case Some(user) => Right(user)
        case None => Left(IdNotFoundError("hashtag", tag))
      }

  def getPage(pageNum: Int)(implicit xa: Transactor[IO]): IO[ResultPage[HashtagStats]] = {
    val offset = pageNum * 10 + 1
    (selectF ++ fr"ORDER BY id ASC LIMIT 10 OFFSET $offset")
      .query[HashtagStats]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }
}
