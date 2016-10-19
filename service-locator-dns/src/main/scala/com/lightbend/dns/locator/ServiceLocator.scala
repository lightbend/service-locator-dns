/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.dns.locator

import java.util.concurrent.ThreadLocalRandom

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.io.AsyncDnsResolver.SrvResolved
import akka.io.{ Dns, IO }
import akka.pattern.{ AskTimeoutException, ask, pipe }
import ru.smslv.akka.dns.raw.SRVRecord

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.matching.Regex

object ServiceLocator {

  def props: Props =
    Props(new ServiceLocator)

  /**
   * Get one address given a service name. Names will be translated given a list of translator
   * regexs provided as config. Only the first translation matched will be used. It is therefore
   * important that the matchers are ordered carefully.
   *
   * An [[Addresses]] object will be replied with addresses sorted by priority and weight, as
   * per RFC 2782. Only one of the addresses within the highest priority and randomized across weight
   * will be returned.
   */
  final case class GetAddress(name: String)

  /**
   * Get one or more addresses given a service name. Names are translated as per [[GetAddress]].
   *
   * An [[Addresses]] object will be replied with addresses sorted by priority and weight, as
   * per RFC 2782.
   */
  final case class GetAddresses(name: String)

  /**
   * A sequence of ServiceAddress objects are the reply, which may of course be empty.
   */
  sealed abstract case class Addresses(addresses: Seq[ServiceAddress])
  object Addresses {
    private val empty: Addresses = new Addresses(Nil) {}

    def apply(addresses: Seq[ServiceAddress]): Addresses =
      if (addresses.nonEmpty) new Addresses(addresses) {}
      else empty
  }

  /**
   * Used within replies.
   */
  final case class ServiceAddress(protocol: String, host: String, port: Int)

  private[locator] final case class RequestContext(replyTo: ActorRef, resolveOne: Boolean, srv: Seq[SRVRecord])

  private[locator] final case class ReplyContext(resolutions: Seq[(Dns.Resolved, SRVRecord)], rc: RequestContext)

  @tailrec
  private[locator] def matchName(name: String, nameTranslators: Seq[(Regex, String)]): Option[String] =
    nameTranslators match {
      case (r, s) +: tail =>
        val matcher = r.pattern.matcher(name)
        if (matcher.matches())
          Some(matcher.replaceAll(s))
        else
          matchName(name, tail)
      case _ => None
    }

  private[locator] def protocolFromName(name: String): String =
    name.iterator.dropWhile(_ != '.').drop(1).takeWhile(_ != '.').drop(1).mkString
}

/**
 * A service locator that can get all addresses for a service using DNS SRV lookups.
 * When considering DNS SRV we ignore priority and weight, sd they appear pretty useless
 * for distributing across service instances as the information is often static in nature.
 * If this turns out not to be the case though then we could certainly consider them.
 * We also avoid caching requests at the level of this actor as the underlying DNS
 * resolver will cache heavily for us. Again though, caching could be introduced at this
 * actor's level if we find that it is required.
 */
class ServiceLocator extends Actor with ActorSettings with ActorLogging {

  import ServiceLocator._

  private val _dns = IO(Dns)(context.system)
  protected def dns: ActorRef = _dns

  override def receive: Receive = {
    case GetAddress(name) =>
      resolve(name, resolveOne = true)

    case GetAddresses(name) =>
      resolve(name, resolveOne = false)

    case rc: RequestContext =>
      // When we return just one address then we randomize which of the candidates to return
      val (srvFrom, srvSize) =
        if (rc.resolveOne && rc.srv.nonEmpty)
          (ThreadLocalRandom.current.nextInt(rc.srv.size), 1)
        else
          (0, rc.srv.size)
      import context.dispatcher
      val resolutions =
        rc.srv
          .slice(srvFrom, srvFrom + srvSize)
          .map { srv =>
            dns
              .ask(Dns.Resolve(srv.target))(settings.resolveTimeout)
              .mapTo[Dns.Resolved]
              .map(_ -> srv)
          }
      Future
        .sequence(resolutions)
        .map(ReplyContext(_, rc))
        .pipeTo(self)

    case ReplyContext(resolutions, rc) =>
      val addresses =
        resolutions
          .flatMap {
            case (resolved, srv) =>
              val protocol = protocolFromName(srv.name)
              val port = srv.port
              resolved.ipv4.map(host => ServiceAddress(protocol, host.getHostAddress, port)) ++
                resolved.ipv6.map(host => ServiceAddress(protocol, host.getHostAddress, port))
          }
      rc.replyTo ! Addresses(addresses)

    case iobe: IndexOutOfBoundsException =>
      log.error("Could not substitute the service name with the name translator {}", iobe.getMessage)
      sender() ! Addresses(Nil)

    case cce: ClassCastException =>
      log.debug("Bad resolution - is this a properly formed SRV lookup of the form '_service._proto.some.domain'? {}", cce.getMessage)
      sender() ! Addresses(Nil)

    case ate: AskTimeoutException =>
      log.debug("Timed out on resolving an SRV record: {}", ate.getMessage)
      sender() ! Addresses(Nil)
  }

  private def resolve(name: String, resolveOne: Boolean): Unit = {
    log.debug("Resolving: {}", name)
    val matchedName = matchName(name, settings.nameTranslators)
    matchedName.foreach { mn =>
      val replyTo = sender()
      import context.dispatcher
      dns
        .ask(Dns.Resolve(mn))(settings.resolveTimeout)
        .mapTo[SrvResolved]
        .map(resolved => RequestContext(replyTo, resolveOne, resolved.srv))
        .pipeTo(self)
    }
    if (matchedName.isEmpty)
      sender() ! Addresses(Nil)
  }
}
