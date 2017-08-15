lazy val root = project
  .in(file("."))
  .aggregate(serviceLocatorDns)

lazy val serviceLocatorDns = project
  .in(file("service-locator-dns"))
  .enablePlugins(AutomateHeaderPlugin)

name := "root"

publishArtifact := false
