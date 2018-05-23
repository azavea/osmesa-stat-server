package osmesa.server.model

import doobie._
import doobie.implicits._
import cats.effect._
import io.circe._
import io.circe.generic.semiauto._


case class Hashtag(
  id: Int,
  hashtag: String
)


object Hashtag {

  implicit val hashtagDecoder: Decoder[Hashtag] = deriveDecoder
  implicit val hashtagEncoder: Encoder[Hashtag] = deriveEncoder

  private val selectF = fr"""
      SELECT
        id, hashtag
      FROM
        hashtags
    """

  def byId(id: Int)(implicit xa: Transactor[IO]): fs2.Stream[IO, Hashtag] =
    (selectF ++ fr"WHERE id == $id")
      .query[Hashtag]
      .stream
      .transact(xa)

}

