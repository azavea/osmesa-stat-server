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
  roadKmAdded: Double,
  roadKmModified: Double,
  waterwayKmAdded: Double,
  waterwayKmModified: Double,
  roadsAdded: Int,
  roadsModified: Int,
  waterwaysAdded: Int,
  waterwaysModified: Int,
  buildingsAdded: Int,
  buildingsModified: Int,
  poisAdded: Int,
  poisModified: Int,
  editor: String,
  userId: Int,
  createdAt: LocalDate,
  closedAt: LocalDate,
  augmentedDiffs: Array[Int],
  updatedAt: LocalDate
)


object Changeset {

  implicit val changesetDecoder: Decoder[Changeset] = deriveDecoder
  implicit val changesetEncoder: Encoder[Changeset] = deriveEncoder

  private val selectF = fr"""
      SELECT
        id, road_km_added, road_km_modified, waterway_km_added, waterway_km_modified,
        roads_added, roads_modified, waterways_added, waterways_modified, buildings_added,
        buildings_modified, pois_added, pois_modified, editor, user_id, created_at,
        closed_at, augmented_diffs, updated_at
      FROM
        changesets
    """

  def byId(id: Long)(implicit xa: Transactor[IO]): fs2.Stream[IO, Changeset] =
    (selectF ++ fr"WHERE id == $id")
      .query[Changeset]
      .stream
      .transact(xa)

}

