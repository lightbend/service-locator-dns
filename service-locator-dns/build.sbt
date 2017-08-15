name := "service-locator-dns"

resolvers += Resolver.hajile

libraryDependencies ++= Seq(
  Library.akkaTestkit,
  Library.scalaTest % "test"
)
