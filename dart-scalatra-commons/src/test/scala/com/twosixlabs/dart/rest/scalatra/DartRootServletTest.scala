package com.twosixlabs.dart.rest.scalatra

import com.twosixlabs.dart.rest.scalatra.models.HealthStatus
import com.twosixlabs.dart.rest.scalatra.models.HealthStatus.HealthStatus
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatra.test.scalatest.ScalatraSuite

class DartRootServletMinTest extends StandardTestBase3x with ScalatraSuite {

    addServlet( new DartRootServlet( None, None ), "/*" )

    behavior of "DartRootServlet"

    it should "return a dart failure response for 404 instead of jetty default" in {
        get( "/somebadendpoint" ) {
            status shouldBe 404
            body should include( "error_message" )
            body should include( "/somebadendpoint does not exist" )
        }
    }

    it should "return 404 for the health endpoint" in {
        get( "/test/path/health" ) {
            status shouldBe 404
            body should include( "error_message" )
            body should include( "/test/path/health does not exist" )
        }
    }
}

class DartRootServletTest extends StandardTestBase3x with ScalatraSuite {

    addServlet( new DartRootServlet( Some( "/test/path" ), None ), "/*" )

    behavior of "DartRootServlet"

    it should "return a dart failure response for 404 instead of jetty default" in {
        get( "/somebadendpoint" ) {
            status shouldBe 404
            body should include( "error_message" )
            body should include( "/somebadendpoint does not exist" )
        }
    }

    it should "return only a healthy status if no version is passed and healthcheck is not overridden" in {
        get( "/test/path/health" ) {
            status shouldBe 200
            body shouldBe """{"status":"HEALTHY"}"""
        }
    }
}

class DartRootServletVersionTest extends StandardTestBase3x with ScalatraSuite {

    addServlet( new DartRootServlet( Some( "/test/path" ), Some( "1.0.363" ) ), "/*" )

    it should "return a healthy status and version if version is passed but healthcheck is not overridden" in {
        get( "/test/path/health" ) {
            status shouldBe 200
            body shouldBe """{"status":"HEALTHY","version":"1.0.363"}"""
        }
    }

}

class DartRootServletCheckerTest extends StandardTestBase3x with ScalatraSuite {

    val rootServlet = new DartRootServlet( Some( "/test/path" ), Some( "1.1.5" ) ) {
        override def healthcheck : (HealthStatus, Option[ String ]) = ( HealthStatus.FAIR, Some( "test message" ) )
    }

    addServlet( rootServlet, "/*" )

    it should "return an unhealthy status and version if version is passed an a healthcheck function is passed that returns unhealthy" in {
        get( "/test/path/health" ) {
            status shouldBe 200
            body shouldBe """{"status":"FAIR","message":"test message","version":"1.1.5"}"""
        }
    }

}
