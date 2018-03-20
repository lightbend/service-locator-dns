/*
 * Copyright 2016 Lightbend, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.dns.locator

import akka.actor.{ Actor, ExtendedActorSystem, Extension, ExtensionKey }

import scala.concurrent.duration.{ Duration, FiniteDuration, MILLISECONDS }
import com.typesafe.config.Config

import scala.util.matching.Regex
import scala.collection.JavaConversions._

object Settings extends ExtensionKey[Settings]

/**
 * Settings for the service locator.
 */
class Settings(system: ExtendedActorSystem) extends Extension {
  val nameTranslators: Seq[(Regex, String)] =
    serviceLocatorDns
      .getObjectList("name-translators")
      .toList
      .flatMap(_.toMap.map {
        case (k, v) => k.r -> v.unwrapped().toString
      })

  val srvTranslators: Seq[(Regex, String)] =
    serviceLocatorDns
      .getObjectList("srv-translators")
      .toList
      .flatMap(_.toMap.map {
        case (k, v) => k.r -> v.unwrapped().toString
      })

  val externalServices: Map[String, String] =
    serviceLocatorDns
      .getObjectList("external-services")
      .toList
      .flatMap(x => x.toMap.map {
        case (k, v) => k -> v.unwrapped().toString
      }).toMap

  val resolveTimeout1: FiniteDuration =
    duration(serviceLocatorDns, "resolve-timeout1")

  val resolveTimeout2: FiniteDuration =
    duration(serviceLocatorDns, "resolve-timeout2")

  private lazy val config = system.settings.config
  private lazy val serviceLocatorDns = config.getConfig("service-locator-dns")

  private def duration(config: Config, key: String): FiniteDuration =
    Duration(config.getDuration(key, MILLISECONDS), MILLISECONDS)
}

trait ActorSettings {
  this: Actor =>

  protected val settings: Settings =
    Settings(context.system)
}
