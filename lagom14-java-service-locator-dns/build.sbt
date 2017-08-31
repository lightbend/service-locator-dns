name := "lagom14-java-service-locator-dns"

libraryDependencies ++= Seq(
  Library.lagom14JavaClient,
  Library.akkaTestkit % "test",
  Library.scalaTest   % "test"
)

resolvers += Resolver.hajile
