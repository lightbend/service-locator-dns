/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Lightbend, Inc.
 */

import sbt.Resolver._
import sbt._

object Version {
  // OSS
  val akka       = "2.4.20"
  val akkaDns    = "2.4.2"
  val lagom13    = "1.3.7"
  val lagom14    = "1.4.0-M1"
  val scalaTest  = "3.0.1"
}

object Library {
  // OSS
  val akkaDns            = "ru.smslv.akka"       %% "akka-dns"              % Version.akkaDns
  val akkaTestkit        = "com.typesafe.akka"   %% "akka-testkit"          % Version.akka
  val lagom13JavaClient  = "com.lightbend.lagom" %% "lagom-javadsl-client"  % Version.lagom13
  val lagom14JavaClient  = "com.lightbend.lagom" %% "lagom-javadsl-client"  % Version.lagom14
  val lagom13ScalaClient = "com.lightbend.lagom" %% "lagom-scaladsl-client" % Version.lagom13
  val lagom13ScalaServer = "com.lightbend.lagom" %% "lagom-scaladsl-server" % Version.lagom13
  val lagom14ScalaClient = "com.lightbend.lagom" %% "lagom-scaladsl-client" % Version.lagom14
  val lagom14ScalaServer = "com.lightbend.lagom" %% "lagom-scaladsl-server" % Version.lagom14
  val scalaTest          = "org.scalatest"       %% "scalatest"             % Version.scalaTest
}

object Resolver {
  val hajile = bintrayRepo("hajile", "maven")
}
