package osmesa.server.model

import doobie._
import doobie.implicits._
import cats.effect._
import io.circe._
import io.circe.generic.semiauto._


case class Country(
  id: Int,
  name: Option[String],
  code: String
)


object Country {

  implicit val countryDecoder: Decoder[Country] = deriveDecoder
  implicit val countryEncoder: Encoder[Country] = deriveEncoder

  private val selectF = fr"""
      SELECT
        id, name, code
      FROM
        countries
    """

  def byId(id: Int)(implicit xa: Transactor[IO]): fs2.Stream[IO, Country] =
    (selectF ++ fr"WHERE id = $id")
      .query[Country]
      .stream
      .transact(xa)

}

