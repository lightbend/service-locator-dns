# Service Locator DNS

DNS SRV lookups for Akka and Lagom which therefore includes Mesos/Marathon, Kubernetes and Consul usage. This project provides two libraries:

* a pure-Akka based service locator for locating services by name and returning their hosts and ports; and
* a Lagom wrapping around the service locator