/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Lightbend, Inc.
 */

import com.typesafe.sbt.SbtScalariform._
import xerial.sbt.Sonatype.autoImport._
import sbt._
import sbt.Keys._

import scalariform.formatter.preferences._
import de.heikoseeberger.sbtheader.{ HeaderPattern, HeaderPlugin }

object Build extends AutoPlugin {

  override def requires =
    plugins.JvmPlugin && HeaderPlugin

  override def trigger =
    allRequirements

  override def projectSettings =
    scalariformSettings ++
      List(
        // Core settings
        organization := "com.lightbend",
        scalaVersion := "2.12.2",
        scalacOptions ++= List(
          "-unchecked",
          "-deprecation",
          "-feature",
          "-language:_",
          "-target:jvm-1.8",
          "-encoding", "UTF-8"),
        homepage := Some(url("http://conductr.lightbend.com/")),
        licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
        developers := List(
          Developer("conductr", "ConductR Library Contributors", "", url("https://github.com/typesafehub/conductr-lib/graphs/contributors"))),
        scmInfo := Some(ScmInfo(url("https://github.com/typesafehub/service-locator-dns"), "git@github.com:typesafehub/service-locator-dns.git")),
        // Scalariform settings
        ScalariformKeys.preferences := ScalariformKeys.preferences.value
          .setPreference(AlignSingleLineCaseStatements, true)
          .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
          .setPreference(DoubleIndentClassDeclaration, true),
        // Header settings
        HeaderPlugin.autoImport.headers := Map(
          "scala" -> (
            HeaderPattern.cStyleBlockComment,
            """|/*
            | * Copyright © 2016 Lightbend, Inc. All rights reserved.
            | * No information contained herein may be reproduced or transmitted in any form
            | * or by any means without the express written permission of Typesafe, Inc.
            | */
            |
            |""".stripMargin),
          "conf" -> (
            HeaderPattern.hashLineComment,
            """|# Copyright © 2016 Lightbend, Inc. All rights reserved.
            |# No information contained herein may be reproduced or transmitted in any form
            |# or by any means without the express written permission of Typesafe, Inc.
            |
            |""".stripMargin)),
        // Sonatype settings
        sonatypeProfileName := "com.lightbend")
}
