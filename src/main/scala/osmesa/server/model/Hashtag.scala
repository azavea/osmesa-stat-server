package osmesa.server.model

import cats.effect._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.generic.semiauto._
import osmesa.server._

case class Hashtag(id: Int, hashtag: String)

object Hashtag extends Implicits {

  implicit val hashtagDecoder: Decoder[Hashtag] = deriveDecoder
  implicit val hashtagEncoder: Encoder[Hashtag] = deriveEncoder

  private val selectF =
    fr"""
      SELECT
        id, hashtag
      FROM
        hashtags
    """

  def byId(
    id: Int
  )(implicit xa: Transactor[IO]): IO[Either[OsmStatError, Hashtag]] =
    (selectF ++ fr"WHERE id = $id")
      .query[Hashtag]
      .option
      .transact(xa)
      .map {
        case Some(country) => Right(country)
        case None          => Left(IdNotFoundError("hashtag", id))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(
    implicit xa: Transactor[IO]
  ): IO[ResultPage[Hashtag]] = {
    val offset = (pageNum - 1) * pageSize
    (selectF ++ fr"ORDER BY id LIMIT $pageSize OFFSET $offset")
      .query[Hashtag]
      .to[List]
      .map({
        ResultPage(_, pageNum)
      })
      .transact(xa)
  }
}
