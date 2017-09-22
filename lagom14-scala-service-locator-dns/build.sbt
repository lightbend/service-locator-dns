name := "lagom14-scala-service-locator-dns"

libraryDependencies ++= Seq(
  Library.lagom14ScalaClient,
  Library.akkaTestkit % "test",
  Library.lagom14ScalaServer % "test",
  Library.scalaTest   % "test"
)

resolvers += Resolver.hajile
