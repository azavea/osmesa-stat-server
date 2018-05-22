package osmesa.server.model

import doobie._
import doobie.implicits._
import cats.effect._


case class Country(
  id: Int,
  name: Option[String],
  code: String
)


object Country {

  private val selectF = fr"""
      SELECT
        id, name, code
      FROM
        countries
    """

  def byId(id: Int)(implicit xa: Transactor[IO]): ConnectionIO[Option[Country]] =
    (selectF ++ fr"WHERE id == $id").query[Country].option

}

