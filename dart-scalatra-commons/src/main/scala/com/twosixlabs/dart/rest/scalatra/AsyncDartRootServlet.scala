package com.twosixlabs.dart.rest.scalatra

import com.twosixlabs.dart.rest.scalatra.models.{HealthCheckResponse, HealthStatus}
import com.twosixlabs.dart.rest.scalatra.models.HealthStatus

import scala.concurrent.{ExecutionContext, Future}

/**
  * Asynchronous version of DartRootServlet (see comments on DartRootServlet.scala for usage)
  *
  * The main differences are that it includes future support and the healthcheck method returns a
  * future. It should mostly be used, however, when your service consists of only a single controller
  * and you want the health endpoint of DartRootServlet. In this case you need to extend DartRootServlet.
  * If your controller is asynchronous, however, you will want to extend this class to get the benefits
  * of AsyncDartScalatraServlet.
  */
class AsyncDartRootServlet( basePath : Option[ String ] = None,
                            version : Option[ String ] = None,
                            log : Boolean = false ) extends AsyncDartScalatraServlet {

    override protected implicit def executor : ExecutionContext = scala.concurrent.ExecutionContext.global

    if (log ) {
        setStandardConfig()
    } else {
        before() {
            setContentTypeJson()
        }
    }

    /**
      * Override this method to determine system health dynamically. It should return a tuple with the
      * status (using HealthStatus enum) and an optional string explaining why a non-healthy
      * status was returned.
      *
      * @return
      */
    def healthcheck : Future[ (HealthStatus.HealthStatus, Option[ String ]) ] = Future.successful( (HealthStatus.HEALTHY, None) )

    if ( basePath.isDefined ) get( basePath.get.stripSuffix( "/" ) + "/health" )( handleOutput {
        healthcheck map { tup =>
            val (health, message) = tup
            HealthCheckResponse( health.toString, message, version )
        }
    } )

}
