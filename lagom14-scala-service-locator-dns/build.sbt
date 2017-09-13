name := "lagom14-scala-service-locator-dns"

libraryDependencies ++= Seq(
  Library.lagom14ScalaClient,
  Library.akkaTestkit % "test",
  Library.scalaTest   % "test"
)

resolvers += Resolver.hajile
