package osmesa.server.model

import java.time.Instant

import cats.effect._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.java8.time._
import osmesa.server._

case class Changeset(id: Long,
                     measurements: Json,
                     counts: Json,
                     editor: Option[String],
                     userId: Option[Int],
                     createdAt: Option[Instant],
                     closedAt: Option[Instant],
                     augmentedDiffs: Option[Array[Int]],
                     updatedAt: Option[Instant])

object Changeset extends Implicits {
  implicit val changesetDecoder: Decoder[Changeset] = deriveDecoder[Changeset]
  implicit val changesetEncoder: Encoder[Changeset] = deriveEncoder[Changeset]

  private val selectF = fr"""
      SELECT
        id,
        coalesce(measurements, '{}'::jsonb) measurements,
        coalesce(counts, '{}'::jsonb) counts,
        editor,
        user_id,
        created_at,
        closed_at,
        augmented_diffs,
        updated_at
      FROM
        changesets
    """

  def byId(
    id: Long
  )(implicit xa: Transactor[IO]): IO[Either[OsmStatError, Changeset]] =
    (selectF ++ fr"WHERE id = $id")
      .query[Changeset]
      .option
      .transact(xa)
      .map {
        case Some(changeset) => Right(changeset)
        case None            => Left(IdNotFoundError("changeset", id))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(
    implicit xa: Transactor[IO]
  ): IO[ResultPage[Changeset]] = {
    val offset = (pageNum - 1) * pageSize
    (selectF ++ fr"ORDER BY id ASC LIMIT $pageSize OFFSET $offset;")
      .query[Changeset]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }

}
