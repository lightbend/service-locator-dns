# Service Locator DNS

[![Build Status](https://api.travis-ci.org/typesafehub/service-locator-dns.png?branch=master)](https://travis-ci.org/typesafehub/service-locator-dns)

## Motivation

DNS SRV is a proposed standard in order to support locating services over the DNS protocol. Service discovery backends commonly offer DNS SRV as a protocol, and so this project leverages that capability.

## Introduction

[DNS SRV](https://tools.ietf.org/html/rfc2782) lookups for [Akka](http://akka.io/) and [Lagom](http://www.lagomframework.com/) which therefore includes [Mesos/Marathon](https://mesosphere.github.io/marathon/), [Kubernetes](http://kubernetes.io/) and [Consul](https://www.consul.io/) usage. This project provides two libraries:

* a pure-Akka based service locator for locating services by name and returning their hosts and ports; and
* a Lagom service locator utilizing the Akka one.

This project uses the wonderful [akka-dns](https://github.com/ilya-epifanov/akka-dns) project in order to offer non-blocking DNS lookups for SRV records.

## Usage

You'll need a resolver for akka-dns:

```scala
resolvers += bintrayRepo("hajile", "maven")
```

For pure Akka based usage (without Lagom):

```scala
libraryDependencies += "com.lightbend" %% "service-locator-dns" % "0.1.0"
```

Alternatively, when using from Lagom:

```scala
libraryDependencies += "com.lightbend" %% "lagom-service-locator-dns" % "0.1.0"
```

Your platform will require differing "transformers" that will translate a service name
into a DNS SRV name to be looked up. Here is the Typesafe config declaration to be 
considered:

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
  name-translators = [
    {
      "^.*$" = "_$0._tcp.marathon.mesos"
    }
  ]
  name-translators = ${?SERVICE_LOCATOR_DNS_NAME_TRANSLATORS}

  # The amount of time to wait for a DNS resolution to occur
  resolve-timeout = 5 seconds
}
```

## DNS Resolvers

The DNS resolver must be told where to resolve from. Here's a sample Typesafe config entry:

```scala
akka.io.dns {
  resolver = async-dns
  async-dns {
    nameservers = ["8.8.8.8", "8.8.4.4"]
  }
}
```

NOTE: There is [an issue on akka-dns](https://github.com/ilya-epifanov/akka-dns/issues/2) in 
order to read the `/etc/resolv.conf` file by default. This should then obviate the need
to declare the above configuration.
