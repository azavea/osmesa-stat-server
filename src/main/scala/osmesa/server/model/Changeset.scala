package osmesa.server.model

import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import cats.effect._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time._

import java.time.LocalDate


case class Changeset(
  id: Long,
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
  editor: Option[String],
  userId: Option[Int],
  createdAt: Option[LocalDate],
  closedAt: Option[LocalDate],
  augmentedDiffs: Option[Array[Int]],
  updatedAt: Option[LocalDate]
)


object Changeset {

  implicit val changesetDecoder: Decoder[Changeset] = deriveDecoder
  implicit val changesetEncoder: Encoder[Changeset] = deriveEncoder

  private val selectF = fr"""
      SELECT
        id, road_km_added, road_km_modified, waterway_km_added, waterway_km_modified,
        coastline_km_added, coastline_km_modified, roads_added, roads_modified,
        waterways_added, waterways_modified, coastlines_added, coastlines_modified,
        buildings_added, buildings_modified, pois_added, pois_modified, editor, user_id,
        created_at, closed_at, augmented_diffs, updated_at
      FROM
        changesets
    """

  def byId(id: Long)(implicit xa: Transactor[IO]): IO[Either[OsmStatError, Changeset]] =
    (selectF ++ fr"WHERE id = $id")
      .query[Changeset]
      .option
      .transact(xa)
      .map {
        case Some(changeset) => Right(changeset)
        case None => Left(IdNotFoundError("changeset", id))
      }

  def getPage(pageNum: Int)(implicit xa: Transactor[IO]): IO[ResultPage[Changeset]] = {
    val offset = pageNum * 10 + 1
    (selectF ++ fr"ORDER BY id ASC LIMIT 10 OFFSET $offset;")
      .query[Changeset]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }

}
