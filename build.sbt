lazy val root = project
  .in(file("."))
  .aggregate(serviceLocatorDns, lagomServiceLocatorDns)

lazy val serviceLocatorDns = project
  .in(file("service-locator-dns"))
  .enablePlugins(AutomateHeaderPlugin)

lazy val lagomServiceLocatorDns = project
  .in(file("lagom-service-locator-dns"))
  .dependsOn(serviceLocatorDns % "compile")
  .enablePlugins(AutomateHeaderPlugin)

name := "root"
publishArtifact := false