name := "lagom-service-locator-dns"

resolvers += Resolver.hajile

libraryDependencies ++= Seq(
  Library.lagom,
  Library.lagomClient,
  Library.akkaTestkit,
  Library.scalaTest % "test"
)
