package com.twosixlabs.dart.rest.scalatra.models

object HealthStatus extends Enumeration {
    type HealthStatus = Value

    val UNHEALTHY, FAIR, HEALTHY = Value
}
