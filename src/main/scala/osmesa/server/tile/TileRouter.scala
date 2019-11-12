package osmesa.server.tile

import blobstore.s3.S3Store
import blobstore.{Store, Path => BStorePath}
import cats.effect._
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import geotrellis.vector.Extent
import geotrellis.vectortile.VectorTile
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{`Content-Encoding`, `Content-Type`}
import osmesa.server.Config

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

class TileRouter(tileConf: Config.Tiles) extends Http4sDsl[IO] {

  private val s3client =
    AmazonS3ClientBuilder.standard().withRegion("us-east-1").build()
  private val store: Store[IO] = S3Store[IO](s3client)

  private val vtileContentType = `Content-Type`(
    ("application", "vnd.mapbox-vector-tile")
  )

  private val emptyVT = VectorTile(Map(), Extent(0, 0, 1, 1))

  def routes: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "user" / userId / IntVar(z) / IntVar(x) / y =>
      val getBytes = store
        .get(
          tilePath(s"${tileConf.s3prefix}/user/${userId}", z, x, y),
          tileConf.chunkSize
        )
        .compile
        .to[Array]
        .attempt

      getBytes
        .flatMap {
          case Right(bytes) =>
            Ok(bytes, `Content-Encoding`(ContentCoding.gzip))
          case Left(s3e: AmazonS3Exception)
              if s3e.getStatusCode == 403 || s3e.getStatusCode == 404 =>
            Ok(emptyVT.toBytes)
        }
        .map(_.withContentType(vtileContentType))

    case GET -> Root / "hashtag" / hashtag / IntVar(z) / IntVar(x) / y =>
      val getBytes = store
        .get(
          tilePath(s"${tileConf.s3prefix}/hashtag/${hashtag}", z, x, y),
          tileConf.chunkSize
        )
        .compile
        .to[Array]
        .attempt

      getBytes
        .flatMap {
          case Right(bytes) =>
            Ok(bytes, `Content-Encoding`(ContentCoding.gzip))
          case Left(s3e: AmazonS3Exception)
              if s3e.getStatusCode == 403 || s3e.getStatusCode == 404 =>
            Ok(emptyVT.toBytes)
        }
        .map(_.withContentType(vtileContentType))
  }

  def tilePath(pre: String, z: Int, x: Int, y: String) = {
    BStorePath(tileConf.s3bucket, s"${pre}/${z}/${x}/${y}", None, false, None)
  }
}
