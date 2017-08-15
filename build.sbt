lazy val akkaDns =
  RootProject(uri("git://github.com/davecromberge/akka-dns.git#master"))

lazy val root = project
  .in(file("."))
  .aggregate(serviceLocatorDns)

lazy val serviceLocatorDns = project
  .dependsOn(akkaDns)
  .in(file("service-locator-dns"))
  .enablePlugins(AutomateHeaderPlugin)

name := "root"

publishArtifact := false
