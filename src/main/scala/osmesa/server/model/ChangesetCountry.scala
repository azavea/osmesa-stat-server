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
        changeset_countries
    """

  def byId(changesetId: Int, countryId: Int)(implicit xa: Transactor[IO]): fs2.Stream[IO, ChangesetCountry] =
    (selectF ++ fr"WHERE changeset_id == $changesetId AND country_id == $countryId")
      .query[ChangesetCountry]
      .stream
      .transact(xa)

}

