package com.twosixlabs.dart.rest.scalatra

import com.twosixlabs.dart.rest.scalatra.models.{HealthCheckResponse, HealthStatus}
import com.twosixlabs.dart.rest.scalatra.models.HealthStatus

/**
  * This servlet is designed to be mounted at the root path of a Dart scalatra
  * REST service. It serves two functions:
  *
  * 1) Handles all 404 errors using standard dart error response instead of default
  *    Jetty 404 page. This is especially important because Jetty lists all the
  *    live endpoints, which is a bad practice in production.
  * 2) Optionally adds a health endpoint, which sends a healthy response by default, but can
  *    be configured with a custom health-check method.
  *
  * It should be the first servlet mounted in its context, and its path should be "/".
  * This will ensure it is the last route to be matched, and will therefore not override
  * any other controllers. I.e., ScalatraInit.init should look like this:
  *
  *     override def init( context : ServletContext ) : Unit = {
  *         context.mount( new DartRootServlet( Some( basePath ), "/" )
  *         context.mount( new SomeController, basePath + "/somepath/" )
  *         context.mount( new OtherController, basePath + "/otherpath/" )
  *     }
  *
  * If your project includes only one controller mounted at basePath, you will not
  * be able to use DartRootServlet to provide the health service, as the paths will
  * overlap. In this case you need to mount your controller at "/" and make it
  * extend DartRootServlet. You will then need to hard code basePath in the
  * transformers of each route in your controller.
  *
  * To enable the health endpoint, pass it your service's base path. Passing a
  * "version" parameter will include that version in the response. (You can use
  * getClass.getPackage.getImplementationVersion to provide your service's current
  * version).
  *
  * To use a custom health check function, you need to override the function by extending
  * the class. The easiest way to do this is to make an anonymous object:
  *
  *     val rootController = new DartRootServlet( Some( "/base/path" ), Some( "1.0.0" ) ) {
  *         override def healthcheck = customHealthCheckMethod
  *     }
  */
class DartRootServlet( basePath : Option[ String ] = None,
                       version : Option[ String ] = None,
                       log : Boolean = false ) extends DartScalatraServlet {

    if ( log ) {
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
    def healthcheck : (HealthStatus.HealthStatus, Option[ String ]) = (HealthStatus.HEALTHY, None)

    if ( basePath.isDefined ) get( basePath.get.stripSuffix( "/" ) + "/health" )( handleOutput {
        val (health, message) = healthcheck
        HealthCheckResponse( health.toString, message, version  )
    } )
}
