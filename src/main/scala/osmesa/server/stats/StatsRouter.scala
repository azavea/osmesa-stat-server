package osmesa.server.stats

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


class StatsRouter(trans: Transactor[IO]) extends Http4sDsl[IO] {

  private def eitherResult[Result: Encoder](result: Either[OsmStatError, Result]) = {
    result match {
      case Right(succ) => Ok(succ.asJson, `Content-Type`(MediaType.`application/json`))
      case Left(err) => NotFound(err.toString)
    }
  }

  implicit val xa: Transactor[IO] = trans

  object OptionalPageQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("page")


  def routes: HttpService[IO] = HttpService[IO] {
    case GET -> Root =>
      Ok("hello world")

    case GET -> Root / "users" :? OptionalPageQueryParamMatcher(pageNum) =>
      Ok(UserStats.getPage(pageNum.getOrElse(0)).map(_.asJson))

    case GET -> Root / "users" / IntVar(userId) =>
      for {
        io <- UserStats.byId(userId)
        userRes <- eitherResult(io)
      } yield userRes

    // Too many results. The data will get where it needs to go (streamed, chunked response) but the client might well crash
    case GET -> Root / "changesets" :? OptionalPageQueryParamMatcher(pageNum) =>
      Ok(Changeset.getPage(pageNum.getOrElse(0)).map(_.asJson))

    case GET -> Root / "changesets" / LongVar(changesetId) =>
      for {
        io <- Changeset.byId(changesetId)
        changeset <- eitherResult(io)
      } yield changeset

    case GET -> Root / "hashtags" :? OptionalPageQueryParamMatcher(pageNum) =>
      Ok(Hashtag.getPage(pageNum.getOrElse(0)).map(_.asJson))

    case GET -> Root / "hashtags" / IntVar(hashtagId) =>
      for {
        io <- Hashtag.byId(hashtagId)
        hashtag <- eitherResult(io)
      } yield hashtag

    case GET -> Root / "countries" :? OptionalPageQueryParamMatcher(pageNum) =>
      Ok(Country.getPage(pageNum.getOrElse(0)).map(_.asJson))

    case GET -> Root / "countries" / IntVar(countryId) =>
      for {
        io <- Country.byId(countryId)
        country <- eitherResult(io)
      } yield country

    case GET -> Root / "changesets-countries" :? OptionalPageQueryParamMatcher(pageNum) =>
      Ok(ChangesetCountry.getPage(pageNum.getOrElse(0)).map(_.asJson))

    case GET -> Root / "changesets-countries" / IntVar(changesetId) / IntVar(countryId) =>
      for {
        io <- ChangesetCountry.byId(changesetId, countryId)
        changesetCountry <- eitherResult(io)
      } yield changesetCountry
  }
}
