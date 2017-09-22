/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.lagom.scaladsl.dns

import java.net.URI

import akka.actor.{ ActorRef, ActorSystem }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import com.lightbend.dns.locator.{ ServiceLocator => ServiceLocatorService }
import com.lightbend.lagom.internal.client.{ CircuitBreakerConfig, CircuitBreakerMetricsProviderImpl, CircuitBreakers }
import com.lightbend.lagom.internal.spi.CircuitBreakerMetricsProvider
import com.lightbend.lagom.scaladsl.api.Service.named
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service }
import com.lightbend.lagom.scaladsl.dns
import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomApplicationContext }
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router

import scala.concurrent.ExecutionContext

object DnsServiceLocatorSpec {
  class DummyService extends Service {
    override def descriptor: Descriptor = {
      import Service._
      named("dummy")
    }
  }
}

class DnsServiceLocatorSpec extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("ServiceLocatorSpec", ConfigFactory.load())
  implicit val mat: Materializer = ActorMaterializer.create(system)

  val app = new LagomApplication(LagomApplicationContext.Test) with AhcWSComponents with DnsServiceLocatorComponents {
    override lazy val lagomServer = serverFor[DnsServiceLocatorSpec.DummyService](new dns.DnsServiceLocatorSpec.DummyService())
    override lazy val actorSystem: ActorSystem = system
    override lazy val materializer: Materializer = mat
    override lazy val executionContext: ExecutionContext = actorSystem.dispatcher
    override lazy val router: Router = Router.empty
    override lazy val circuitBreakerMetricsProvider: CircuitBreakerMetricsProvider = new CircuitBreakerMetricsProviderImpl(actorSystem)
    override lazy val circuitBreakerConfig: CircuitBreakerConfig = new CircuitBreakerConfig(configuration)
    override lazy val circuitBreakers: CircuitBreakers = new CircuitBreakers(actorSystem, circuitBreakerConfig, circuitBreakerMetricsProvider)
    val dnsServiceLocator = TestProbe()
    override lazy val serviceLocatorService: ActorRef = dnsServiceLocator.ref
  }

  "The DNS service locator" should {

    "be able to look up a known service" in {

      val service = app.serviceLocator.locate("some-service")

      app.dnsServiceLocator.expectMsg(ServiceLocatorService.GetAddress("some-service"))
      app.dnsServiceLocator.sender() ! ServiceLocatorService.Addresses(List(ServiceLocatorService.ServiceAddress("http", "localhost", "127.0.0.1", 9000)))

      service.futureValue shouldBe Some(new URI("http://127.0.0.1:9000"))
    }

    "be able to look up an unknown service" in {

      val service = app.serviceLocator.locate("some-service")

      app.dnsServiceLocator.expectMsg(ServiceLocatorService.GetAddress("some-service"))
      app.dnsServiceLocator.sender() ! ServiceLocatorService.Addresses(List.empty)

      service.futureValue shouldBe None
    }
  }

  override protected def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}
