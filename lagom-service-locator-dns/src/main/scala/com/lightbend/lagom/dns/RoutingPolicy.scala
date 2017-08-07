/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.lightbend.lagom.dns

import com.typesafe.config.ConfigException.BadValue

/**
 * This module and its implementation is from the Consul example provided by J. Boner:
 * https://github.com/jboner/lagom-service-locator-consul
 *
 * The routing policy is not part of the service locator settings in the main project because it may violate
 * the RFC 2782 spec that requires multiple SRV addresses sorted by weight.  In the Lagom service locator, the lookup
 * function requires one result only, and without this fork only the first entry will ever be picked.  This is ok if
 * priority is only determined by weight, but we would like to support round robin as well.
 */
object RoutingPolicy {
  def apply(policy: String): RoutingPolicy = policy match {
    case "first"       => First
    case "random"      => Random
    case "round-robin" => RoundRobin
    case unknown       => throw new BadValue("service-locator-dns.routing-policy", s"[$unknown] is not a valid routing algorithm")
  }
}
sealed trait RoutingPolicy
case object First extends RoutingPolicy
case object Random extends RoutingPolicy
case object RoundRobin extends RoutingPolicy
