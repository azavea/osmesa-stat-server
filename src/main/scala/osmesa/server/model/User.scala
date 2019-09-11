package osmesa.server.model

import cats.effect._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.generic.semiauto._
import osmesa.server._

case class User(id: Int, name: Option[String])

object User extends Implicits {

  implicit val userDecoder: Decoder[User] = deriveDecoder
  implicit val userEncoder: Encoder[User] = deriveEncoder

  private val selectF = fr"""
      SELECT
        id, name
      FROM
        users
    """

  def byId(
    id: Int
  )(implicit xa: Transactor[IO]): IO[Either[OsmStatError, User]] =
    (selectF ++ fr"WHERE id = $id")
      .query[User]
      .option
      .transact(xa)
      .map {
        case Some(user) => Right(user)
        case None       => Left(IdNotFoundError("user", id))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(
    implicit xa: Transactor[IO]
  ): IO[ResultPage[User]] = {
    val offset = (pageNum - 1) * pageSize
    (selectF ++ fr"ORDER BY id ASC LIMIT $pageSize OFFSET $offset")
      .query[User]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }
}
