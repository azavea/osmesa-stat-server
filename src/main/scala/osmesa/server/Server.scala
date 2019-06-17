package osmesa.server

import cats.effect._
import fs2.StreamApp.ExitCode
import fs2._
import org.http4s._
import org.http4s.server.HttpMiddleware
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import osmesa.server.tile._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Server extends StreamApp[IO] {

  private val corsConfig = CORSConfig(
    anyOrigin = true,
    anyMethod = false,
    allowedMethods = Some(Set("GET")),
    allowCredentials = true,
    maxAge = 1.day.toSeconds
  )

  private val middleware: HttpMiddleware[IO] = { routes: HttpService[IO] =>
    CORS(routes, corsConfig)
  }

  def stream(args: List[String],
             requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    for {
      config <- Stream.eval(Config.load())
      transactor <- Stream.eval(Database.transactor(config.database))
      //_        <- Stream.eval(Database.initialize(transactor))
      default = middleware(new DefaultRouter(transactor).routes)
      tiles = middleware(new TileRouter(config.tiles).routes)
      exitCode <- BlazeBuilder[IO]
        .enableHttp2(true)
        .bindHttp(config.server.port, config.server.host)
        .mountService(default, "/")
        .mountService(tiles, "/tiles")
        .serve
    } yield exitCode
  }
}
