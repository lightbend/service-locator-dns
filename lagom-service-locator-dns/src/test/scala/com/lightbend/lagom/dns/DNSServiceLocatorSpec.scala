/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.lagom.dns

import java.net.URI

import akka.actor.{ ActorRef, ActorSystem }
import akka.testkit.TestProbe
import com.lightbend.lagom.javadsl.api.Descriptor
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import play.api.inject.guice.GuiceApplicationBuilder

import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import play.api.inject.bind
import com.lightbend.dns.locator.{ ServiceLocator => ServiceLocatorService }

class DNSServiceLocatorSpec extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit val system = ActorSystem("ServiceLocatorSpec", ConfigFactory.load())
  implicit val dispatcher = system.dispatcher

  "The DNS service locator" should {

    "be able to look up a known service" in {

      val dnsServiceLocator = TestProbe()

      // GuiceApplicationBuilder uses the enabled modules from the `reference.conf`
      val app = new GuiceApplicationBuilder()
        .disable(classOf[ServiceLocatorModule])
        .bindings(bind[ActorRef].qualifiedWith("ServiceLocatorService").to(dnsServiceLocator.ref))
        .build()

      val serviceLocator = app.injector.instanceOf[DnsServiceLocator]
      val service = serviceLocator.locate("some-service", Descriptor.Call.NONE).toScala.map(_.asScala)

      dnsServiceLocator.expectMsg(ServiceLocatorService.GetAddress("some-service"))
      dnsServiceLocator.sender() ! ServiceLocatorService.Addresses(List(ServiceLocatorService.ServiceAddress("http", "127.0.0.1", 9000)))

      service.futureValue shouldBe Some(new URI("http://127.0.0.1:9000"))
    }

    "be able to look up an unknown service" in {

      val dnsServiceLocator = TestProbe()

      // GuiceApplicationBuilder uses the enabled modules from the `reference.conf`
      val app = new GuiceApplicationBuilder()
        .disable(classOf[ServiceLocatorModule])
        .bindings(bind[ActorRef].qualifiedWith("ServiceLocatorService").to(dnsServiceLocator.ref))
        .build()

      val serviceLocator = app.injector.instanceOf[DnsServiceLocator]
      val service = serviceLocator.locate("some-service", Descriptor.Call.NONE).toScala.map(_.asScala)

      dnsServiceLocator.expectMsg(ServiceLocatorService.GetAddress("some-service"))
      dnsServiceLocator.sender() ! ServiceLocatorService.Addresses(List.empty)

      service.futureValue shouldBe None
    }
  }

  override protected def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}
