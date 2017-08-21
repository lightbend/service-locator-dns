logLevel := Level.Warn

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("org.scalariform"   % "sbt-scalariform" % "1.8.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "1.5.1")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"    % "1.1")
