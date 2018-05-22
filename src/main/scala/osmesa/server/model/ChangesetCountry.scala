package osmesa.server.model

import doobie._
import doobie.implicits._
import cats.effect._


case class ChangesetCountry(
  changesetId: Int,
  countryId: Int,
  editCount: Int
)


object ChangesetCountry {

  private val selectF = fr"""
      SELECT
        changeset_id, country_id, edit_count
      FROM
        changeset_countries
    """

  def byId(countryId: Int, changesetId: Int)(implicit xa: Transactor[IO]): fs2.Stream[ConnectionIO, ChangesetCountry] =
    (selectF ++ fr"WHERE changeset_id == $changesetId AND country_id == $countryId").query[ChangesetCountry].stream

}

