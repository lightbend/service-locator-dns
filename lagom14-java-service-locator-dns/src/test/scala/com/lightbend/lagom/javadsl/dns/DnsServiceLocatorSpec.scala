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

import scala.concurrent.ExecutionContextExecutor

class DnsServiceLocatorSpec extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("ServiceLocatorSpec", ConfigFactory.load())
  implicit val dispatcher: ExecutionContextExecutor = system.dispatcher

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
      dnsServiceLocator.sender() ! ServiceLocatorService.Addresses(List(ServiceLocatorService.ServiceAddress("http", "localhost", "127.0.0.1", 9000)))

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
