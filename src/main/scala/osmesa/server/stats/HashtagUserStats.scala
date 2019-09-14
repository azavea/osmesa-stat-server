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

case class HashtagUserStats(tag: String,
                            uid: Long,
                            name: Option[String],
                            measurements: Json,
                            counts: Json,
                            lastEdit: Option[Instant],
                            changesetCount: Option[Int],
                            editCount: Option[Int]
                           )

object HashtagUserStats extends Implicits {
  implicit val hashtagUserDecoder: Decoder[HashtagUserStats] = deriveDecoder
  implicit val hashtagUserEncoder: Encoder[HashtagUserStats] = deriveEncoder

  private val selectF =
    fr"""
      SELECT
        hashtag tag,
        user_id uid,
        name,
        coalesce(measurements, '{}'::jsonb) measurements,
        coalesce(counts, '{}'::jsonb) counts,
        last_edit,
        changeset_count,
        edit_count
      FROM
        hashtag_user_statistics
    """

  def byTagAndUid(
                   tag: String,
                   uid: Long
                 )(implicit xa: Transactor[IO]): IO[Either[OsmStatError, HashtagUserStats]] =
    (selectF ++ fr"WHERE hashtag = $tag AND user_id = $uid")
      .query[HashtagUserStats]
      .option
      .attempt
      .transact(xa)
      .map {
        case Right(hashtagOrNone) =>
          hashtagOrNone match {
            case Some(ht) => Right(ht)
            case None => Left(IdNotFoundError("hashtag_user_statistics", tag))
          }
        case Left(err) => Left(UnknownError(err.toString))
      }

  def getPage(tag: String, pageNum: Int, pageSize: Int = 25)(
    implicit xa: Transactor[IO]
  ): IO[Either[OsmStatError, ResultPage[HashtagUserStats]]] = {
    val offset = (pageNum - 1) * pageSize
    (selectF ++ fr"WHERE hashtag = $tag ORDER BY hashtag, user_id ASC LIMIT $pageSize OFFSET $offset")
      .query[HashtagUserStats]
      .to[List]
      .attempt
      .transact(xa)
      .map {
        case Right(results) => Right(ResultPage(results, pageNum))
        case Left(err) => Left(UnknownError(err.toString))
      }
  }
}
