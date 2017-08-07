/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.lagom.dns

import java.net.URI
import java.util.concurrent.ThreadLocalRandom

import scala.collection.concurrent.TrieMap

import com.lightbend.lagom.scaladsl.client.CircuitBreakingServiceLocator
import com.lightbend.lagom.scaladsl.api.{ Descriptor, ServiceLocator }
import com.lightbend.dns.locator.{ Settings, ServiceLocator => ServiceLocatorService }
import scala.concurrent.{ ExecutionContext, Future }

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import com.lightbend.dns.locator.ServiceLocator.ServiceAddress
import com.lightbend.lagom.internal.client.CircuitBreakers

/**
 * DnsServiceLocator implements Lagom's ServiceLocator by using the DNS Service Locator service, which is an actor.
 */
class DnsServiceLocator(serviceLocatorService: ActorRef,
    system: ActorSystem,
    circuitBreakers: CircuitBreakers)(implicit ec: ExecutionContext) extends CircuitBreakingServiceLocator(circuitBreakers) {

  private val roundRobinIndexFor = TrieMap.empty[String, Int]

  private val locatorSettings = Settings(system)

  private val routingPolicy = RoutingPolicy(locatorSettings.routingPolicy)

  override def locate(name: String, serviceCall: Descriptor.Call[_, _]): Future[Option[URI]] =
    serviceLocatorService
      .ask(ServiceLocatorService.GetAddress(name))(locatorSettings.resolveTimeout1 + locatorSettings.resolveTimeout1 + locatorSettings.resolveTimeout2)
      .mapTo[ServiceLocatorService.Addresses]
      .map {
        case ServiceLocatorService.Addresses(addresses) if addresses.isEmpty =>
          Option.empty[URI]

        case ServiceLocatorService.Addresses(addresses) if addresses.length == 1 =>
          pickFirst(addresses)

        case ServiceLocatorService.Addresses(addresses) => routingPolicy match {
          case First      => pickFirst(addresses)
          case Random     => pickRandom(addresses)
          case RoundRobin => pickRoundRobin(name, addresses)
        }
      }

  private def toUri(sa: ServiceAddress) =
    new URI(sa.protocol, null, sa.host, sa.port, null, null, null)

  private[dns] def pickFirst(addresses: Seq[ServiceAddress]): Option[URI] =
    addresses
      .headOption
      .map(toUri)

  private[dns] def pickRandom(addresses: Seq[ServiceAddress]): Option[URI] =
    Option(addresses.map(toUri)
      .sorted
      .apply(ThreadLocalRandom.current.nextInt(addresses.size - 1)))

  private[dns] def pickRoundRobin(name: String, addresses: Seq[ServiceAddress]): Option[URI] = {
    roundRobinIndexFor.putIfAbsent(name, 0)
    val sortedAddresses = addresses.map(toUri).sorted
    val currentIndex = roundRobinIndexFor(name)
    val nextIndex =
      if (sortedAddresses.size > currentIndex + 1) currentIndex + 1
      else 0
    roundRobinIndexFor.replace(name, nextIndex)
    Option(sortedAddresses.apply(currentIndex))
  }
}
