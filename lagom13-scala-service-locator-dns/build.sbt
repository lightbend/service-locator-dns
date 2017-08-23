name := "lagom13-scala-service-locator-dns"

libraryDependencies ++= Seq(
  Library.lagom13ScalaClient,
  Library.akkaTestkit % "test",
  Library.scalaTest   % "test"
)

resolvers += Resolver.hajile
