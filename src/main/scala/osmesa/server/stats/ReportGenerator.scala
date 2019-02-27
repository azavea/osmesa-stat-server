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
        id, name, road_km_added, road_km_modified, waterway_km_added, waterway_km_modified, coastline_km_added, coastline_km_modified, roads_added, roads_modified, waterways_added, waterways_modified, coastlines_added, coastlines_modified, buildings_added, buildings_modified, pois_added, pois_modified, last_edit, edit_count
      FROM
        user_statistics
    ) TO stdout ( encoding 'utf-8', format csv, header )
    """

  def apply()(implicit xa: Transactor[IO]): IO[String] = {
    val bao = new java.io.ByteArrayOutputStream
    val prog: ConnectionIO[Long] = PHC.pgGetCopyAPI(PFCM.copyOut(copyCmd, bao))
    prog.transact(xa).map{ num => new String(bao.toByteArray, java.nio.charset.StandardCharsets.UTF_8) }
  }

  // def byId(id: Long)(implicit xa: Transactor[IO]): IO[Either[OsmStatError, UserStats]] =
  //   (selectF ++ fr"WHERE id = $id")
  //     .query[UserStats]
  //     .option
  //     .transact(xa)
  //     .map {
  //       case Some(user) => Right(user)
  //       case None => Left(IdNotFoundError("user", id))
  //     }

  // def getPage(pageNum: Int, pageSize: Int = 25)(implicit xa: Transactor[IO]): IO[ResultPage[UserStats]] = {
  //   val offset = pageNum * pageSize + 1
  //   (selectF ++ fr"ORDER BY id ASC LIMIT $pageSize OFFSET $offset")
  //     .query[UserStats]
  //     .to[List]
  //     .map({ ResultPage(_, pageNum) })
  //     .transact(xa)
  // }
}
