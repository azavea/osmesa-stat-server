scalaVersion := "2.11.12"

description := "OSMesa Statistics Server"

organization := "azavea"

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Ypartial-unification",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:existentials",
  "-feature"
)

externalResolvers := Seq(
 "locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
 "locationtech-snapshots" at "https://repo.locationtech.org/content/groups/snapshots"
)

val Http4sVer = "0.18.11"
val DoobieVer = "0.5.3"
val CirceVer = "0.10.0-M1"
val ScalaTestVer = "3.0.5"
val GeotrellisVer = "2.0.0-RC1"
val fs2BlobstoreVer = "0.1.6"
libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-blaze-server" % Http4sVer,
  "org.http4s"    %% "http4s-dsl"          % Http4sVer,
  "org.http4s"    %% "http4s-circe"        % Http4sVer,
  "io.circe"      %% "circe-core"          % CirceVer,
  "io.circe"      %% "circe-generic"       % CirceVer,
  "io.circe"      %% "circe-parser"        % CirceVer,
  "io.circe"      %% "circe-java8"         % CirceVer,
  "org.tpolecat"  %% "doobie-core"         % DoobieVer,
  "org.tpolecat"  %% "doobie-postgres"     % DoobieVer,
  "org.tpolecat"  %% "doobie-hikari"       % DoobieVer,
  "org.tpolecat"  %% "doobie-scalatest"    % DoobieVer % "test",
  "org.flywaydb"  %  "flyway-core"         % "4.2.0",
  "com.github.pureconfig" %% "pureconfig"  % "0.9.1",
  "org.locationtech.geotrellis" %% "geotrellis-raster" % GeotrellisVer,
  "org.locationtech.geotrellis" %% "geotrellis-spark" % GeotrellisVer,
  "org.locationtech.geotrellis" %% "geotrellis-vectortile" % GeotrellisVer,
  "com.lendup.fs2-blobstore" %% "core" % fs2BlobstoreVer,
  "com.lendup.fs2-blobstore" %% "sftp" % fs2BlobstoreVer,
  "com.lendup.fs2-blobstore" %% "s3"   % fs2BlobstoreVer,
  "org.scalatest" %%  "scalatest"          % ScalaTestVer % "test"
)

parallelExecution in Test := false

assemblyJarName in assembly := "osm-stat-server.jar"
