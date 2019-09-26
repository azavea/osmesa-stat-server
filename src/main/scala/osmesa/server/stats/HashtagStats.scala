package osmesa.server.stats

import java.time.Instant

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.java8.time._
import osmesa.server._
import osmesa.server.model._

case class HashtagStats(tag: String,
                        measurements: Json,
                        counts: Json,
                        changesetCount: Option[Int],
                        editCount: Option[Int],
                        lastEdit: Option[Instant],
                        updatedAt: Option[Instant],
                        userChangesets: Json,
                        userEdits: Json)

object HashtagStats extends Implicits {
  implicit val hashtagDecoder: Decoder[HashtagStats] = deriveDecoder
  implicit val hashtagEncoder: Encoder[HashtagStats] = deriveEncoder

  private val selectF =
    fr"""
      SELECT
        tag,
        coalesce(measurements, '{}'::jsonb) measurements,
        coalesce(counts, '{}'::jsonb) counts,
        changeset_count,
        edit_count,
        last_edit,
        updated_at,
        coalesce(user_changesets, '{}'::jsonb) user_changesets,
        coalesce(user_edits, '{}'::jsonb) user_edits
      FROM
        hashtag_statistics
    """

  def byTag(
    tag: String
  )(implicit xa: Transactor[IO]): IO[Either[OsmStatError, HashtagStats]] =
    (selectF ++ fr"WHERE tag = $tag")
      .query[HashtagStats]
      .option
      .attempt
      .transact(xa)
      .map {
        case Right(hashtagOrNone) =>
          hashtagOrNone match {
            case Some(ht) => Right(ht)
            case None     => Left(IdNotFoundError("hashtag_statistics", tag))
          }
        case Left(err) => Left(UnknownError(err.toString))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(
    implicit xa: Transactor[IO]
  ): IO[Either[OsmStatError, ResultPage[HashtagStats]]] = {
    val offset = (pageNum - 1) * pageSize
    (selectF ++ fr"ORDER BY tag ASC LIMIT $pageSize OFFSET $offset")
      .query[HashtagStats]
      .to[List]
      .attempt
      .transact(xa)
      .map {
        case Right(results) => Right(ResultPage(results, pageNum))
        case Left(err)      => Left(UnknownError(err.toString))
      }
  }
}
