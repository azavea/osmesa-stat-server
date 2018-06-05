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
import blobstore.Store,
import blobstore.s3.S3Store
import com.amazonaws.services.s3.AmazonS3ClientBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class TileRouter(trans: Transactor[IO], tileConf: Config.Tiles) extends Http4sDsl[IO] {

  implicit val xa: Transactor[IO] = trans

  private val store: Store[IO] = S3Store[IO](AmazonS3ClientBuilder.standard().build())

  val bucket = tileConf.s3bucket
  val prefix = tileConf.s3prefix

  def routes: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "user" / userId / IntVar(z) / IntVar(x) / IntVar(y) =>
      Ok()

    case GET -> Root / "user" / hashtag / IntVar(z) / IntVar(x) / IntVar(y) =>
      Ok()
  }
}
