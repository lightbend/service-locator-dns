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
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import scala.util.matching.Regex

object ServiceLocator {

  val Name = "DnsServiceLocator"

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
  final case class ServiceAddress(protocol: String, hostname: String, host: String, port: Int)

  private[locator] final case class RequestContext(replyTo: ActorRef, resolveOne: Boolean, srv: Seq[SRVRecord])

  private[locator] final case class ReplyContext(resolutions: Seq[(Dns.Resolved, SRVRecord)], rc: RequestContext)

  @tailrec
  private[locator] def matchTranslation(name: String, translators: Seq[(Regex, String)]): Option[String] =
    translators match {
      case (r, s) +: tail =>
        val matcher = r.pattern.matcher(name)
        if (matcher.matches())
          Some(matcher.replaceAll(s))
        else
          matchTranslation(name, tail)
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
      resolveSrv(name, resolveOne = true)

    case GetAddresses(name) =>
      resolveSrv(name, resolveOne = false)

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
            resolveDns(srv.target).map(_ -> srv)
          }
      Future
        .sequence(resolutions)
        .map(ReplyContext(_, rc))
        .pipeTo(self)

    case ReplyContext(resolutions, rc) =>
      log.debug("Resolved: {}", resolutions)
      val addresses =
        resolutions
          .flatMap {
            case (resolved, srv) =>
              val protocol = protocolFromName(srv.name)
              val port = srv.port
              resolved.ipv4.map(host => ServiceAddress(protocol, srv.target, host.getHostAddress, port)) ++
                resolved.ipv6.map(host => ServiceAddress(protocol, srv.target, host.getHostAddress, port))
          }
      rc.replyTo ! Addresses(addresses)
  }

  private def resolveSrv(name: String, resolveOne: Boolean): Unit = {
    log.debug("Resolving: {}", name)
    val matchedName = matchTranslation(name, settings.nameTranslators)
    matchedName.foreach { mn =>
      if (name != mn)
        log.debug("Translated {} to {}", name, mn)

      val replyTo = sender()
      import context.dispatcher
      resolveSrvOnce(mn, settings.resolveTimeout1)
        .recoverWith {
          case _: AskTimeoutException =>
            resolveSrvOnce(mn, settings.resolveTimeout1)
              .recoverWith {
                case _: AskTimeoutException =>
                  resolveSrvOnce(mn, settings.resolveTimeout2)
              }
        }
        .recover {
          case iobe: IndexOutOfBoundsException =>
            log.error("Could not substitute the service name with the name translator {}", iobe.getMessage)
            SrvResolved(mn, Nil)

          case ate: AskTimeoutException =>
            log.debug("Timed out querying DNS SRV for {}", name)
            SrvResolved(mn, Nil)

          case NonFatal(e) =>
            log.error(e, "Unexpected error when resolving an SRV record")
            SrvResolved(mn, Nil)
        }
        .map(resolved =>
          RequestContext(
            replyTo,
            resolveOne,
            resolved.srv.map { record =>
              matchTranslation(record.name, settings.srvTranslators) match {
                case Some(newName) if name != newName =>
                  log.debug("Translated {} to {}", record.name, newName)
                  record.copy(name = newName)
                case _ =>
                  record
              }
            }))
        .pipeTo(self)
    }
    if (matchedName.isEmpty)
      sender() ! Addresses(Nil)
  }

  private def resolveSrvOnce(name: String, resolveTimeout: FiniteDuration): Future[SrvResolved] = {
    import context.dispatcher
    dns
      .ask(Dns.Resolve(name))(resolveTimeout)
      .map {
        case srvResolved: SrvResolved => srvResolved
        case _: Dns.Resolved          => SrvResolved(name, Nil)
      }
  }

  private def resolveDns(name: String): Future[Dns.Resolved] = {
    import context.dispatcher
    dns
      .ask(Dns.Resolve(name))(settings.resolveTimeout1)
      .recoverWith {
        case _: AskTimeoutException =>
          dns.ask(Dns.Resolve(name))(settings.resolveTimeout1)
            .recoverWith {
              case _: AskTimeoutException =>
                dns.ask(Dns.Resolve(name))(settings.resolveTimeout2)
            }
      }
      .mapTo[Dns.Resolved]
      .recover {
        case ate: AskTimeoutException =>
          log.debug("Timed out querying DNS for {}", name)
          Dns.Resolved(name, Nil)

        case NonFatal(e) =>
          log.error(e, "Unexpected error when resolving an DNS record")
          Dns.Resolved(name, Nil)
      }
  }
}
