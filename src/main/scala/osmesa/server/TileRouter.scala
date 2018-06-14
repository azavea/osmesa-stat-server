package osmesa.server

import osmesa.server.model._

import cats.effect._
import doobie.Transactor
import io.circe._
import io.circe.syntax._
import fs2._
import fs2.StreamApp.ExitCode
import org.http4s.circe._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.HttpMiddleware
import org.http4s.server.middleware.{GZip, CORS, CORSConfig}
import org.http4s.headers.{Location, `Content-Type`}
import blobstore.{Path => BStorePath}
import blobstore.Store
import blobstore.s3.S3Store
import geotrellis.vector.Extent
import geotrellis.vectortile.VectorTile
import com.amazonaws.services.s3.AmazonS3ClientBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util._


class TileRouter(tileConf: Config.Tiles) extends Http4sDsl[IO] {

  private val s3client = AmazonS3ClientBuilder.standard().withRegion("us-east-1").build()
  private val store: Store[IO] = S3Store[IO](s3client)

  private val vtileContentType = `Content-Type`(("application", "vnd.mapbox-vector-tile"))

  private val emptyVT = VectorTile(Map(), Extent(0, 0, 1, 1))

  def tilePath(pre: String, z: Int, x: Int, y: Int) = {
    BStorePath(tileConf.s3bucket, s"${pre}/${z}/${x}/${y}${tileConf.s3suffix.getOrElse("")}", None, false, None)
  }

  def routes: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "user" / userId / IntVar(z) / IntVar(x) / IntVar(y) =>
      val bytes = store
        .get(tilePath(s"${tileConf.s3prefix}/user/${userId}", z, x, y), tileConf.chunkSize)
        .compile
        .to[Array]
      val response = (try Ok(bytes) catch { case e: Exception => Ok(emptyVT.toBytes) })
      response.map { _.withContentType(vtileContentType) }

    case GET -> Root / "hashtag" / hashtag / IntVar(z) / IntVar(x) / IntVar(y) =>
      val bytes = store
        .get(tilePath(s"${tileConf.s3prefix}/hashtag/${hashtag}", z, x, y), tileConf.chunkSize)
        .compile
        .to[Array]
      val response = (try Ok(bytes) catch { case e: Exception => Ok(emptyVT.toBytes) })
      response.map { _.withContentType(vtileContentType) }
  }
}
