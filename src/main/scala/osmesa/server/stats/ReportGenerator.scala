package osmesa.server.stats

import osmesa.server.model._

import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.jawn._
import io.circe.syntax._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import fs2._
import org.http4s.circe._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.HttpMiddleware
import org.http4s.server.middleware.{GZip, CORS, CORSConfig}
import org.http4s.headers.{Location, `Content-Type`}
import org.postgresql.util.PGobject
import org.postgresql.copy.CopyManager

import scala.concurrent.duration._


object ReportGenerator {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val userStatsDecoder: Decoder[UserStats] = deriveDecoder
  implicit val userStatsEncoder: Encoder[UserStats] = deriveEncoder

  private val copyCmd = """
    COPY (
      SELECT
        id, name, road_km_added, road_km_modified, road_km_deleted, waterway_km_added, waterway_km_modified, waterway_km_deleted,
        coastline_km_added, coastline_km_modified, coastline_km_deleted, roads_added, roads_modified, roads_deleted,
        waterways_added, waterways_modified, waterways_deleted, coastlines_added, coastlines_modified, coastlines_deleted,
        buildings_added, buildings_modified, buildings_deleted, pois_added, pois_modified, pois_deleted, last_edit, edit_count
      FROM
        user_statistics
    ) TO stdout ( encoding 'utf-8', format csv, header )
    """

  def apply()(implicit xa: Transactor[IO]): IO[String] = {
    val bao = new java.io.ByteArrayOutputStream
    val prog: ConnectionIO[Long] = PHC.pgGetCopyAPI(PFCM.copyOut(copyCmd, bao))
    prog.transact(xa).map{ num => new String(bao.toByteArray, java.nio.charset.StandardCharsets.UTF_8) }
  }

}
