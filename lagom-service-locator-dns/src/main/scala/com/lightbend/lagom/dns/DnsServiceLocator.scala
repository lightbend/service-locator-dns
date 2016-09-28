/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.lagom.dns

import java.net.URI
import java.util.Optional
import java.util.concurrent.CompletionStage
import java.util.function.{ Function => JFunction }
import javax.inject.Inject

import com.lightbend.lagom.javadsl.api.{ Descriptor, ServiceLocator }

import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.{ ExecutionContext, Future }

/**
 * DnsServiceLocator implements Lagom's ServiceLocator by using the DNS Service Locator.
 */
class DnsServiceLocator @Inject() (implicit ec: ExecutionContext) extends ServiceLocator {

  private def locateAsScala(name: String): Future[Option[URI]] =
    Future.successful(None)

  override def locate(name: String, serviceCall: Descriptor.Call[_, _]): CompletionStage[Optional[URI]] =
    locateAsScala(name).map(_.asJava).toJava

  override def doWithService[T](name: String, serviceCall: Descriptor.Call[_, _], block: JFunction[URI, CompletionStage[T]]): CompletionStage[Optional[T]] =
    locateAsScala(name).flatMap(uriOpt => {
      uriOpt.fold(Future.successful(Optional.empty[T])) { uri =>
        block.apply(uri)
          .toScala
          .map(v => Optional.of(v))
      }
    }).toJava
}
