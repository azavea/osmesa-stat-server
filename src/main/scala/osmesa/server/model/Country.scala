package osmesa.server.model

import cats.effect._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.generic.semiauto._
import osmesa.server._

case class Country(id: Int, name: Option[String], code: String)

object Country extends Implicits {

  implicit val countryDecoder: Decoder[Country] = deriveDecoder
  implicit val countryEncoder: Encoder[Country] = deriveEncoder

  private val selectF = fr"""
      SELECT
        id, name, code
      FROM
        countries
    """

  def byId(
    id: Int
  )(implicit xa: Transactor[IO]): IO[Either[OsmStatError, Country]] =
    (selectF ++ fr"WHERE id = $id")
      .query[Country]
      .option
      .transact(xa)
      .map {
        case Some(country) => Right(country)
        case None          => Left(IdNotFoundError("country", id))
      }

  def getPage(pageNum: Int, pageSize: Int = 25)(
    implicit xa: Transactor[IO]
  ): IO[ResultPage[Country]] = {
    val offset = (pageNum - 1) * pageSize
    (selectF ++ fr"ORDER BY id ASC LIMIT $pageSize OFFSET $offset")
      .query[Country]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }

}
