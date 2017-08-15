name := "service-locator-dns"

resolvers += Resolver.hajile

libraryDependencies ++= Seq(
  Library.akkaDns,
  Library.akkaTestkit,
  Library.scalaTest % "test"
)
