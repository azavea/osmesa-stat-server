scalaVersion := "2.12.6"

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

val Http4sVer = "0.18.11"
val DoobieVer = "0.5.3"
val ScalaTestVer = "3.0.5"
libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-blaze-server" % Http4sVer,
  "org.http4s"    %% "http4s-dsl"          % Http4sVer,
  "org.tpolecat"  %% "doobie-core"         % DoobieVer,
  "org.tpolecat"  %% "doobie-postgres"     % DoobieVer,
  "org.tpolecat"  %% "doobie-scalatest"    % DoobieVer    % "test",  // ScalaTest support for typechecking statements.
  "org.scalatest" %%  "scalatest"          % ScalaTestVer % "test"
)

parallelExecution in Test := false

