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
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service }
import com.lightbend.lagom.scaladsl.client.CircuitBreakersPanel
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
    override lazy val lagomServer = serverFor[DnsServiceLocatorSpec.DummyService](new DnsServiceLocatorSpec.DummyService())
    override lazy val actorSystem: ActorSystem = system
    override lazy val materializer: Materializer = mat
    override lazy val executionContext: ExecutionContext = actorSystem.dispatcher
    override lazy val router: Router = Router.empty
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
