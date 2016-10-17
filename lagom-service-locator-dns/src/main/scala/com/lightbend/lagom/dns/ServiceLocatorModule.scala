/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.lagom.dns

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
        Akka.bindingOf[ServiceLocatorService]("ServiceLocatorService")
      )
    else
      Seq.empty
}
