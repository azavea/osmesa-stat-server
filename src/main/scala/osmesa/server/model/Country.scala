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

  def byId(id: Int)(implicit xa: Transactor[IO]): IO[Either[OsmStatError, Country]] =
    (selectF ++ fr"WHERE id = $id")
      .query[Country]
      .option
      .transact(xa)
      .map {
        case Some(country) => Right(country)
        case None => Left(IdNotFoundError("country", id))
      }

  def getPage(pageNum: Int)(implicit xa: Transactor[IO]): IO[ResultPage[Country]] = {
    val offset = pageNum * 10 + 1
    (selectF ++ fr"ORDER BY id ASC LIMIT 10 OFFSET $offset")
      .query[Country]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }

}

