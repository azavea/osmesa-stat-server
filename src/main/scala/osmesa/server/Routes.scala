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


class Router(trans: Transactor[IO]) extends Http4sDsl[IO] {

  implicit val xa: Transactor[IO] = trans

  def routes: HttpService[IO] = HttpService[IO] {
    case GET -> Root =>
      Ok("testing - root")

    case GET -> Root / "user" / IntVar(userId) =>
      Ok(Stream("[") ++ User.byId(userId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))

    case GET -> Root / "changeset" / LongVar(changesetId) =>
      Ok(Stream("[") ++ Changeset.byId(changesetId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))

    case GET -> Root / "hashtag" / IntVar(hashtagId) =>
      Ok(Stream("[") ++ Hashtag.byId(hashtagId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))

    case GET -> Root / "country" / IntVar(countryId) =>
      Ok(Stream("[") ++ Country.byId(countryId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))

    case GET -> Root / "changeset-country" / IntVar(changesetId) / IntVar(countryId) =>
      Ok(Stream("[") ++ ChangesetCountry.byId(changesetId, countryId).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))
  }
}
