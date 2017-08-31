lazy val root = project
  .in(file("."))
  .aggregate(serviceLocatorDns, lagom13JavaServiceLocatorDns, lagom13ScalaServiceLocatorDns)

lazy val serviceLocatorDns = project
  .in(file("service-locator-dns"))
  .enablePlugins(AutomateHeaderPlugin)

lazy val lagom13JavaServiceLocatorDns = project
  .in(file("lagom13-java-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)

lazy val lagom14JavaServiceLocatorDns = project
  .in(file("lagom14-java-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)

lazy val lagom13ScalaServiceLocatorDns = project
  .in(file("lagom13-scala-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)

lazy val lagom14ScalaServiceLocatorDns = project
  .in(file("lagom14-scala-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)

name := "root"
publishArtifact := false