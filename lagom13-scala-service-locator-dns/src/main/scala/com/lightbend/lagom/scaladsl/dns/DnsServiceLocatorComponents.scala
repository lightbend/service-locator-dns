/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.lagom.scaladsl.dns

import akka.actor.ActorRef
import com.lightbend.dns.locator.{ ServiceLocator => ServiceLocatorService }
import com.lightbend.lagom.scaladsl.client.CircuitBreakerComponents

/**
 * Provides the DNS service locator.
 */
trait DnsServiceLocatorComponents extends CircuitBreakerComponents {
  def serviceLocatorService: ActorRef =
    actorSystem.actorOf(ServiceLocatorService.props, ServiceLocatorService.Name)

  lazy val serviceLocator: DnsServiceLocator =
    new DnsServiceLocator(serviceLocatorService, actorSystem, circuitBreakers, executionContext)
}
