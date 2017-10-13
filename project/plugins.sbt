logLevel := Level.Warn

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("org.scalariform"  % "sbt-scalariform" % "1.8.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "3.0.2")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"         % "1.1.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"     % "1.0.6")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"    % "2.0")