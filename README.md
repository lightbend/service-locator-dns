# Service Locator DNS

[![Build Status](https://api.travis-ci.org/typesafehub/service-locator-dns.png?branch=master)](https://travis-ci.org/typesafehub/service-locator-dns)

## Motivation

DNS SRV is specification that describes how to locate services over the DNS protocol. Service discovery backends commonly offer DNS SRV as a protocol, and so this project leverages that capability.

## Introduction

[DNS SRV](https://tools.ietf.org/html/rfc2782) lookups for [Akka](http://akka.io/) and [Lagom](http://www.lagomframework.com/) which therefore includes [Mesos/Marathon](https://mesosphere.github.io/marathon/), [Kubernetes](http://kubernetes.io/) and [Consul](https://www.consul.io/) usage. This project provides two libraries:

* a pure-Akka based service locator for locating services by name and returning their hosts and ports; and
* a Lagom service locator utilizing the Akka one.

This project uses the wonderful [akka-dns](https://github.com/ilya-epifanov/akka-dns) project in order to offer non-blocking DNS lookups for SRV records.

## Usage

The service locator is written using Akka and can be used directly as via its Actor. Alternatively Lagom can be configured to use this project's service locator with no additional coding for your project.

### Pure Akka usage

You'll need a resolver for akka-dns:

```scala
resolvers += bintrayRepo("hajile", "maven")
```

For pure Akka based usage (without Lagom):

```scala
libraryDependencies += "com.lightbend" %% "service-locator-dns" % "1.0.0"
```

An example:

```scala
  // Requisite import
  import com.lightbend.dns.locator.ServiceLocator
  
  // Create a service locator
  val serviceLocator = system.actorOf(ServiceLocator.props, ServiceLocator.Name)
  
  // Send a request to get addresses. Expect a `ServiceLocator.Addresses` reply.
  serviceLocator ! ServiceLocator.GetAddresses("_some-service._tcp.marathon.mesos")
```

### Lagom usage

Alternatively, when using from Lagom:

```scala
libraryDependencies += "com.lightbend" %% "lagom-service-locator-dns" % "1.0.0"
```

Simply adding the above dependency to a Lagom project should be sufficient. There is a `ServiceLocatorModule` that will be automatically discovered by the Lagom environment. All of your Lagom service locator calls will automatically route via the service locator implementation that this project provides.

## Advanced configuration

Your platform may require differing "transformers" that will translate a service name
into a DNS SRV name to be looked up. Here is the Typesafe config declaration to be 
considered in its entirety.

```
service-locator-dns {
  # A list of translators - their order is significant. Translates a service name passed to the
  # service locator into a name that will be used for DNS SRV resolution. Only the first match
  # will be used. This can be easily overridden by providing a SERVICE_LOCATOR_DNS_NAME_TRANSLATORS
  # environment variable.
  #
  # The default translator below should be all that is required for a DC/OS or Mesos/Marathon
  # environment. Kubernetes DNS SRV records take the form:
  #
  #   _my-port-name._my-port-protocol.my-svc.my-namespace.svc.cluster.local
  #
  # ...which implies that your service name should provide the port name. For example with
  # Cassandra where there are typically 3 ports ("native" => 9042, "rpc" => 9160 and "storage" => 7200),
  # your service name might use a hyphen as a delim for the namespace, service name and port name e.g.
  # "customers-cassandra-native". In this case your translator would look like:
  #
  #   "(.*)-(.*)-(.*)" = "_$3._tcp.$2.$1.svc.cluster.local"
  #
  # You may also want to encode the service's protocol into its name given that the caller
  # that is interested in the service location will typically know whether it will be tcp or udp
  # (or anything else). Taking the above example, you might then have "customers-cassandra-native-tcp"
  # and translate these four components.
  #
  # You can of course have multiple translators though and statically declare the translations as
  # your service's configuration (you'll be supplying environment specific configuration quite typically
  # anyway...), and thus keep your service names nice and clean.
  #
  # By default though, we don't translate much i.e. we let it all pass through and put the onus on the
  # client to specify the right service name.
  name-translators = [
    {
      "^.*$" = "$0"
    }
  ]
  name-translators = ${?SERVICE_LOCATOR_DNS_NAME_TRANSLATORS}

  # The amount of time to wait for a DNS resolution to occur for the first and second lookups of a given
  # name.
  resolve-timeout1 = 1 second

  # The amount of time to wait for a DNS resolution to occur for the third lookup of a given
  # name.
  resolve-timeout2 = 2 seconds
}
```
