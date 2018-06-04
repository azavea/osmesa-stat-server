package osmesa.server.model

import doobie._
import doobie.implicits._
import cats.effect._
import io.circe._
import io.circe.generic.semiauto._


case class ChangesetCountry(
  changesetId: Int,
  countryId: Int,
  editCount: Int
)


object ChangesetCountry {

  implicit val changesetCountryDecoder: Decoder[ChangesetCountry] = deriveDecoder
  implicit val changesetCountryEncoder: Encoder[ChangesetCountry] = deriveEncoder

  private val selectF = fr"""
      SELECT
        changeset_id, country_id, edit_count
      FROM
        changesets_countries
    """

  def byId(changesetId: Int, countryId: Int)(implicit xa: Transactor[IO]): IO[Either[OsmStatError, ChangesetCountry]] =
    (selectF ++ fr"WHERE changeset_id = $changesetId AND country_id == $countryId")
      .query[ChangesetCountry]
      .option
      .transact(xa)
      .map {
        case Some(changesetCountry) => Right(changesetCountry)
        case None => Left(IdNotFoundError("changesetCountry", (changesetId, countryId)))
      }

  def getPage(pageNum: Int)(implicit xa: Transactor[IO]): IO[ResultPage[ChangesetCountry]] = {
    val offset = pageNum * 10 + 1
    (selectF ++ fr"ORDER BY changesetId ASC, countryId ASC LIMIT 10 OFFSET $offset")
      .query[ChangesetCountry]
      .to[List]
      .map({ ResultPage(_, pageNum) })
      .transact(xa)
  }
}

