# Service Locator DNS

[![Build Status](https://api.travis-ci.org/typesafehub/service-locator-dns.png?branch=master)](https://travis-ci.org/typesafehub/service-locator-dns)

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

## Motivation

DNS SRV is a proposed standard in order to support locating services over the DNS protocol. Service discovery backends commonly offer DNS SRV as a protocol, and so this project leverages that capability.