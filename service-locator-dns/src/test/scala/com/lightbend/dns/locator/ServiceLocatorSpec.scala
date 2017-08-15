/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.dns.locator

import java.net.InetAddress

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.io.AsyncDnsResolver.SrvResolved
import akka.io.Dns
import akka.testkit.TestProbe
import com.lightbend.dns.locator.ServiceLocator.{ Addresses, ServiceAddress }
import com.typesafe.config.ConfigFactory
import org.scalatest._
import ru.smslv.akka.dns.raw.SRVRecord

import scala.concurrent.duration._

object ServiceLocatorSpec {
  class TestServiceLocator(dnsProbe: TestProbe) extends ServiceLocator {
    override protected def dns: ActorRef =
      dnsProbe.ref
  }
}

class ServiceLocatorSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  import ServiceLocatorSpec._

  implicit val system = ActorSystem(
    "ServiceLocatorSpec",
    ConfigFactory.parseString(
      s"""
         |service-locator-dns {
         |  srv-translators = [
         |    { "^_test-srv-translator[.]_tcp[.](.+)" = "_test-srv-translator._http.$$1" },
         |    { "^.*$$" = "$$0" }
         |  ]
         |}
       """.stripMargin))

  "A DNS service locator" should {
    "resolve a service to 2 addresses" in {
      val dnsProbe = TestProbe()
      val serviceLocator = system.actorOf(Props(new TestServiceLocator(dnsProbe)))

      val requestor = TestProbe()

      requestor.send(serviceLocator, ServiceLocator.GetAddresses("_some-service._tcp.marathon.mesos"))

      dnsProbe.expectMsg(Dns.Resolve("_some-service._tcp.marathon.mesos"))
      val srv1 = SRVRecord("_some-service._tcp.marathon.mesos", 3600, 0, 0, 1000, "some-service-host1.marathon.mesos")
      val srv2 = SRVRecord("_some-service._tcp.marathon.mesos", 3600, 0, 0, 1001, "some-service-host2.marathon.mesos")
      dnsProbe.sender() ! SrvResolved("some-service", List(srv1, srv2))

      dnsProbe.expectMsg(Dns.Resolve("some-service-host1.marathon.mesos"))
      dnsProbe.sender() ! Dns.Resolved("some-service-host1.marathon.mesos", List(InetAddress.getByName("1.1.1.1")))
      dnsProbe.expectMsg(Dns.Resolve("some-service-host2.marathon.mesos"))
      dnsProbe.sender() ! Dns.Resolved("some-service-host2.marathon.mesos", List(InetAddress.getByName("1.1.1.2")))

      requestor.expectMsg(
        Addresses(
          Seq(
            ServiceAddress("tcp", "some-service-host1.marathon.mesos", "1.1.1.1", 1000),
            ServiceAddress("tcp", "some-service-host2.marathon.mesos", "1.1.1.2", 1001))))
    }

    "resolve a service to 1 address having requested just one" in {
      val dnsProbe = TestProbe()
      val serviceLocator = system.actorOf(Props(new TestServiceLocator(dnsProbe)))

      val requestor = TestProbe()

      requestor.send(serviceLocator, ServiceLocator.GetAddress("_some-service._tcp.marathon.mesos"))

      dnsProbe.expectMsg(Dns.Resolve("_some-service._tcp.marathon.mesos"))
      val srv1 = SRVRecord("_some-service._tcp.marathon.mesos", 3600, 0, 0, 1000, "some-service-host1.marathon.mesos")
      val srv2 = SRVRecord("_some-service._tcp.marathon.mesos", 3600, 0, 0, 1001, "some-service-host2.marathon.mesos")
      dnsProbe.sender() ! SrvResolved("some-service", List(srv1, srv2))

      val (expectedHostname, expectedHost, expectedPort) =
        dnsProbe.expectMsgPF() {
          case Dns.Resolve(hostname @ "some-service-host1.marathon.mesos") => (hostname, "1.1.1.1", 1000)
          case Dns.Resolve(hostname @ "some-service-host2.marathon.mesos") => (hostname, "1.1.1.2", 1001)
        }
      dnsProbe.sender() ! Dns.Resolved(expectedHostname, List(InetAddress.getByName(expectedHost)))
      dnsProbe.expectNoMsg(500.millis)

      requestor.expectMsg(Addresses(Seq(ServiceAddress("tcp", expectedHostname, expectedHost, expectedPort))))
    }

    "Fail to resolve a service due to no srv resolution" in {
      val dnsProbe = TestProbe()
      val serviceLocator = system.actorOf(Props(new TestServiceLocator(dnsProbe)))

      val requestor = TestProbe()

      requestor.send(serviceLocator, ServiceLocator.GetAddress("_some-service._tcp.marathon.mesos"))

      dnsProbe.expectMsg(Dns.Resolve("_some-service._tcp.marathon.mesos"))
      dnsProbe.sender() ! SrvResolved("some-service", List.empty)

      dnsProbe.expectNoMsg(500.millis)

      requestor.expectMsg(Addresses(Seq.empty))
    }

    "Fail to resolve a service due to no target resolution" in {
      val dnsProbe = TestProbe()
      val serviceLocator = system.actorOf(Props(new TestServiceLocator(dnsProbe)))

      val requestor = TestProbe()

      requestor.send(serviceLocator, ServiceLocator.GetAddress("_some-service._tcp.marathon.mesos"))

      dnsProbe.expectMsg(Dns.Resolve("_some-service._tcp.marathon.mesos"))
      val srv1 = SRVRecord("_some-service._tcp.marathon.mesos", 3600, 0, 0, 1000, "some-service-host1.marathon.mesos")
      dnsProbe.sender() ! SrvResolved("some-service", List(srv1))

      dnsProbe.expectMsg(Dns.Resolve("some-service-host1.marathon.mesos"))
      dnsProbe.sender() ! Dns.Resolved("some-service-host1.marathon.mesos", List.empty)
      dnsProbe.expectNoMsg(500.millis)

      requestor.expectMsg(Addresses(Seq.empty))
    }

    "Fail to resolve a service to 1 address given an error on the SRV resolution" in {
      val dnsProbe = TestProbe()
      val serviceLocator = system.actorOf(Props(new TestServiceLocator(dnsProbe)))

      val requestor = TestProbe()

      requestor.send(serviceLocator, ServiceLocator.GetAddress("_some-service._tcp.marathon.mesos"))

      dnsProbe.expectMsg(Dns.Resolve("_some-service._tcp.marathon.mesos"))
      dnsProbe.sender() ! "some stupid reply"
      dnsProbe.expectNoMsg(500.millis)

      requestor.expectMsg(Addresses(List.empty))
    }

    "Fail to resolve a service to 1 address given an error on the DNS resolution of a successful SRV lookup" in {
      val dnsProbe = TestProbe()
      val serviceLocator = system.actorOf(Props(new TestServiceLocator(dnsProbe)))

      val requestor = TestProbe()

      requestor.send(serviceLocator, ServiceLocator.GetAddress("_some-service._tcp.marathon.mesos"))

      dnsProbe.expectMsg(Dns.Resolve("_some-service._tcp.marathon.mesos"))
      val srv1 = SRVRecord("_some-service._tcp.marathon.mesos", 3600, 0, 0, 1000, "some-service-host1.marathon.mesos")
      dnsProbe.sender() ! SrvResolved("some-service", List(srv1))

      dnsProbe.expectMsg(Dns.Resolve("some-service-host1.marathon.mesos"))
      dnsProbe.sender() ! "some stupid reply"
      dnsProbe.expectNoMsg(500.millis)

      requestor.expectMsg(Addresses(List.empty))
    }

    "Translate SRV records after resolving" in {
      val dnsProbe = TestProbe()
      val serviceLocator = system.actorOf(Props(new TestServiceLocator(dnsProbe)))

      val requestor = TestProbe()

      requestor.send(serviceLocator, ServiceLocator.GetAddress("_test-srv-translator._tcp.marathon.mesos"))

      dnsProbe.expectMsg(Dns.Resolve("_test-srv-translator._tcp.marathon.mesos"))
      val srv = SRVRecord("_test-srv-translator._tcp.marathon.mesos", 3600, 0, 0, 1000, "some-service-host.marathon.mesos")
      dnsProbe.reply(SrvResolved("some-service", List(srv)))

      dnsProbe.expectMsgPF() {
        case Dns.Resolve("some-service-host.marathon.mesos") =>
      }

      dnsProbe.reply(Dns.Resolved("some-service-host.marathon.mesos", List(InetAddress.getByName("127.0.0.1"))))
      dnsProbe.expectNoMsg(500.millis)

      requestor.expectMsg(Addresses(Seq(ServiceAddress("http", "some-service-host.marathon.mesos", "127.0.0.1", 1000))))
    }
  }

  override protected def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}

class ServiceLocatorStaticSpec extends WordSpec with Matchers {

  "matching" should {
    "mesos replace all" in {
      val nameTranslators = Seq("^.*$".r -> "_$0._tcp.marathon.mesos")
      val name = "some-service"
      val expected = Some("_some-service._tcp.marathon.mesos")
      ServiceLocator.matchTranslation(name, nameTranslators) should be(expected)
    }

    "k8s replace all" in {
      val nameTranslators = Seq("(.*)-(.*)-(.*)-(.*)".r -> "_$3._$4.$2.$1.svc.cluster.local")
      val name = "customers-cassandra-native-tcp"
      val expected = Some("_native._tcp.cassandra.customers.svc.cluster.local")
      ServiceLocator.matchTranslation(name, nameTranslators) should be(expected)
    }

    "k8s replace all of the second translator" in {
      val nameTranslators =
        Seq(
          "(.*)-(.*)-(.*)-(.*)".r -> "_$3._$4.$2.$1.svc.cluster.local",
          "(.*)-(.*)-(.*)".r -> "_$3._udp.$2.$1.svc.cluster.local")
      val name = "customers-cassandra-native"
      val expected = Some("_native._udp.cassandra.customers.svc.cluster.local")
      ServiceLocator.matchTranslation(name, nameTranslators) should be(expected)
    }

    "k8s not find a match in any of the translators" in {
      val nameTranslators =
        Seq(
          "(.*)-(.*)-(.*)-(.*)".r -> "_$3._$4.$2.$1.svc.cluster.local",
          "(.*)-(.*)-(.*)".r -> "_$3._udp.$2.$1.svc.cluster.local")
      val name = "cannot be matched"
      val expected = None
      ServiceLocator.matchTranslation(name, nameTranslators) should be(expected)
    }
  }

  "extracting a protocol" should {
    "work given a properly formatted string" in {
      ServiceLocator.protocolFromName("_native._udp.cassandra.customers.svc.cluster.local") should be("udp")
    }

    "still work given an improperly formatted string" in {
      ServiceLocator.protocolFromName("native.udp.cassandra.customers.svc.cluster.local") should be("dp")
    }
  }

}
