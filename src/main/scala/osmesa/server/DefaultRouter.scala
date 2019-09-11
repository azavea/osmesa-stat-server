package osmesa.server

import cats.effect._
import doobie.Transactor
import io.circe._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import osmesa.server.model._
import osmesa.server.stats.{CountryStats, HashtagStats, RefreshStats, UserStats}

class DefaultRouter(trans: Transactor[IO]) extends Http4sDsl[IO] {

  def routes: HttpService[IO] = HttpService[IO] {
    case GET -> Root =>
      Ok("hello world")

    case GET -> Root / "users" :? OptionalPageQueryParamMatcher(pageNum) =>
      Ok(UserStats.getPage(pageNum.getOrElse(1)).map(_.asJson))

    case GET -> Root / "users" / IntVar(userId) =>
      for {
        io <- UserStats.byId(userId)
        userRes <- eitherResult(io)
      } yield userRes

    // Too many results. The data will get where it needs to go (streamed, chunked response) but the client might well crash
    case GET -> Root / "changesets" :? OptionalPageQueryParamMatcher(pageNum) =>
      Ok(Changeset.getPage(pageNum.getOrElse(1)).map(_.asJson))

    case GET -> Root / "changesets" / LongVar(changesetId) =>
      for {
        io <- Changeset.byId(changesetId)
        changeset <- eitherResult(io)
      } yield changeset

    case GET -> Root / "campaigns" :? OptionalPageQueryParamMatcher(pageNum) =>
      for {
        io <- HashtagStats.getPage(pageNum.getOrElse(1))
        res <- eitherResult(io)
      } yield res

    case GET -> Root / "campaigns" / hashtag =>
      for {
        io <- HashtagStats.byTag(hashtag)
        result <- eitherResult(io)
      } yield result

    case GET -> Root / "countries" :? OptionalPageQueryParamMatcher(pageNum) =>
      Ok(Country.getPage(pageNum.getOrElse(1)).map(_.asJson))

    case GET -> Root / "countries" / IntVar(countryId) =>
      for {
        io <- Country.byId(countryId)
        country <- eitherResult(io)
      } yield country

    case GET -> Root / "country-stats" / countryId =>
      for {
        io <- CountryStats.byId(countryId)
        result <- eitherResult(io)
      } yield result

    case GET -> Root / "changesets-countries" :? OptionalPageQueryParamMatcher(
          pageNum
        ) =>
      Ok(ChangesetCountry.getPage(pageNum.getOrElse(1)).map(_.asJson))

    case GET -> Root / "changesets-countries" / IntVar(changesetId) / IntVar(
          countryId
        ) =>
      for {
        io <- ChangesetCountry.byId(changesetId, countryId)
        changesetCountry <- eitherResult(io)
      } yield changesetCountry

    case GET -> Root / "status" =>
      for {
        io <- RefreshStats.getCurrentStatus
        result <- eitherResult(Right(io))
      } yield result
  }

  implicit val xa: Transactor[IO] = trans

  private def eitherResult[Result: Encoder](
    result: Either[OsmStatError, Result]
  ) = {
    result match {
      case Right(succ) =>
        Ok(succ.asJson, `Content-Type`(MediaType.`application/json`))
      case Left(err) => NotFound(err.toString)
    }
  }

  object OptionalPageQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[Int]("page")
}
