name := "lagom-service-locator-dns"

libraryDependencies ++= Seq(
  Library.lagom,
  Library.akkaTestkit,
  Library.scalaTest % "test"
)

resolvers += Resolver.hajile
