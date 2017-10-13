import ReleaseTransformations._

val sharedSettings = Seq(
  organization := "com.lightbend",
  organizationName := "Lightbend, Inc.",
  startYear := Some(2016),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding", "UTF-8"),
  sonatypeProfileName := "com.lightbend",
  scmInfo := Some(ScmInfo(url("https://github.com/typesafehub/service-locator-dns"), "git@github.com:typesafehub/service-locator-dns.git")),
  scalaVersion := Version.scala211,
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseCrossBuild := false,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    releaseStepCommandAndRemaining("+test"),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(
    serviceLocatorDns,
    lagom13JavaServiceLocatorDns,
    lagom13ScalaServiceLocatorDns,
    lagom14JavaServiceLocatorDns,
    lagom14ScalaServiceLocatorDns
  )
  .settings(sharedSettings)
  .settings(
    publishArtifact := false
  )

lazy val serviceLocatorDns = project
  .in(file("service-locator-dns"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(sharedSettings)
  .settings(
    crossScalaVersions := Vector(Version.scala211, Version.scala212),
  )

lazy val lagom13JavaServiceLocatorDns = project
  .in(file("lagom13-java-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)
  .settings(sharedSettings)

lazy val lagom13ScalaServiceLocatorDns = project
  .in(file("lagom13-scala-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)
  .settings(sharedSettings)

lazy val lagom14JavaServiceLocatorDns = project
  .in(file("lagom14-java-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)
  .settings(sharedSettings)

lazy val lagom14ScalaServiceLocatorDns = project
  .in(file("lagom14-scala-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)
  .settings(sharedSettings)
