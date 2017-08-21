/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Lightbend, Inc.
 */

import sbt.Resolver._
import sbt._

object Version {
  // OSS
  val akka = "2.4.12"
  val lagom = "1.3.6"
  val scalaTest = "3.0.0"
  val akkaDns = "2.4.2"
}

object Library {
  // OSS
  val akkaDns = "ru.smslv.akka" %% "akka-dns" % Version.akkaDns
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % Version.akka
  val lagom = "com.lightbend.lagom" %% "lagom-scaladsl-api" % Version.lagom
  val lagomClient = "com.lightbend.lagom" %% "lagom-scaladsl-client" % Version.lagom
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
}

object Resolver {
  val hajile = bintrayRepo("hajile", "maven")
}
