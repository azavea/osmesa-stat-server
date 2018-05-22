package osmesa.server.model

import doobie._
import doobie.implicits._
import cats.effect._


case class User(
  id: Int,
  name: String
)


object User {

  private val selectF = fr"""
      SELECT
        id, name
      FROM
        users
    """

  def byId(id: Int)(implicit xa: Transactor[IO]): ConnectionIO[Option[User]] =
    (selectF ++ fr"WHERE id == $id").query[User].option

}

