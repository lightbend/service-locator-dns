logLevel := Level.Warn

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.typesafe.sbt"  % "sbt-scalariform" % "1.3.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "1.5.1")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"    % "1.1")
