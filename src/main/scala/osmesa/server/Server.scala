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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.net.URI


object Server extends StreamApp[IO] with Http4sDsl[IO] {

  private val corsConfig = CORSConfig(
    anyOrigin = true,
    anyMethod = false,
    allowedMethods = Some(Set("GET")),
    allowCredentials = true,
    maxAge = 1.day.toSeconds
  )

  def middleware: HttpMiddleware[IO] = { (routes: HttpService[IO]) =>
    GZip(routes)
  }.compose { (routes: HttpService[IO]) =>
    CORS(routes)
  }

  object UriQueryParamMatcher extends QueryParamDecoderMatcher[URI]("uri")

  implicit val uriQueryParamDecoder: QueryParamDecoder[URI] =
    QueryParamDecoder[String].map(URI.create)

  def routes(implicit xa: Transactor[IO]): HttpService[IO] = HttpService[IO] {
    case GET -> Root =>
      Ok("testing - root")
    case GET -> Root / "user" / IntVar(userId) =>
      Ok(Stream("[") ++ User.byId(userId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))
    case GET -> Root / "changeset" / LongVar(changesetId) =>
      Ok(Stream("[") ++ Changeset.byId(changesetId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))
    case GET -> Root / "country" / IntVar(countryId) =>
      Ok(Stream("[") ++ Country.byId(countryId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))
    case GET -> Root / "changeset-country" / IntVar(changesetId) / IntVar(countryId) =>
      Ok(Stream("[") ++ ChangesetCountry.byId(changesetId, countryId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))
  }

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    for {
      config <- Stream.eval(Config.load())
      transactor <- Stream.eval(Database.transactor(config.database))
      service = middleware(routes(transactor))
      exitCode   <- BlazeBuilder[IO]
        .enableHttp2(true)
        .bindHttp(8080, "0.0.0.0")
        .mountService(service)
        .serve
    } yield exitCode
  }
}

