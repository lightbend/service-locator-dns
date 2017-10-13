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

package com.lightbend.lagom.javadsl.dns

import com.lightbend.lagom.javadsl.api.ServiceLocator
import play.api.{ Configuration, Environment, Mode }
import play.api.inject.{ Binding, Module }
import javax.inject.Singleton

import play.api.libs.concurrent.Akka
import com.lightbend.dns.locator.{ ServiceLocator => ServiceLocatorService }

/**
 * This module binds the ServiceLocator interface from Lagom to the `DnsServiceLocator`
 * The `DnsServiceLocator` is only bound if the application has been started in `Prod` mode.
 * In `Dev` mode the embedded service locator of Lagom is used.
 */
class ServiceLocatorModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    if (environment.mode == Mode.Prod)
      Seq(
        bind[ServiceLocator].to[DnsServiceLocator].in[Singleton],
        Akka.bindingOf[ServiceLocatorService]("ServiceLocatorService"))
    else
      Seq.empty
}
