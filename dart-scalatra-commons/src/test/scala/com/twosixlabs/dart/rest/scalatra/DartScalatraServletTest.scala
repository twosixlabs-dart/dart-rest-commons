package com.twosixlabs.dart.rest.scalatra

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}
import com.twosixlabs.dart.exceptions.{AuthenticationException, AuthorizationException, BadQueryParameterException, BadRequestBodyException, GenericServerException, ResourceNotFoundException, ServiceUnreachableException}
import com.twosixlabs.dart.rest.scalatra.models.FailureResponse
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.json.JsonFormat.{marshalFrom, unmarshalTo}
import org.scalatra.test.scalatest.ScalatraSuite
import org.scalatra.{Created, Ok}

import scala.beans.BeanProperty
import scala.util.{Failure, Success, Try}

@JsonInclude( Include.NON_ABSENT )
case class TestObj( @BeanProperty @JsonProperty( "string_field" ) stringField : String,
                    @BeanProperty @JsonProperty( "integer_field" ) integerField : Int )

class DartScalatraServletTest extends StandardTestBase3x with ScalatraSuite {

    override def header = super.header

    private def testStr( stringField : String, integerField : Int ) : String = s"""{"string_field":"${stringField}","integer_field":${integerField}}"""

    private def failStr( status : Int, message : String ) : String = s"""{"status":${status},"error_message":"${message}"}"""

    class TestDartServlet extends DartScalatraServlet {

        setStandardConfig()

        get( "/overrideContentType" )( handleOutput {
            contentType = "straight up bananas; charset=ISO-8859-1"
            Ok( "bananas" )
        } )

        get( "/successfulJacksonObject" )( handleOutput {
            Ok( TestObj( "test output", 24 ) )
        } )

        get( "/successfulJacksonObjectTry" )( handleOutput {
            Success( Ok( TestObj( "test output", 24 ) ) )
        } )

        get( "/successfulJacksonObjectRaw" )( handleOutput {
            TestObj( "test output", 24 )
        } )

        post( "/successfulJacksonObject" )( handleOutput {
            Created( TestObj( "test output", 24 ) )
        } )

        put( "/successfulJacksonObject" )( handleOutput {
            Ok( TestObj( "test output", 24 ) )
        } )

        patch( "/successfulJacksonObject" )( handleOutput {
            Ok( TestObj( "test output", 24 ) )
        } )

        delete( "/successfulJacksonObject" )( handleOutput {
            Ok( TestObj( "test output", 24 ) )
        } )

        get( "/successfulListJacksonObject" )( handleOutput {
            Ok( List( TestObj( "test output 1", 345 ), TestObj( "test output 2", 562 ) ) )
        } )

        get( "/successfulMapStringString" )( handleOutput {
            Ok( Map( "field 1" -> "value 1", "field 2" -> "value 2", "field 3" -> "value 3" ) )
        } )

        get( "/successfulMapStringJacksonObject" )( handleOutput {
            Ok( Map[ String, TestObj ]( "field 1" -> TestObj( "test output 1", 345 ), "field 2" -> TestObj( "test output 2", 562 ) ) )
        } )

        get( "/successfulStringGet" )( handleOutput {
            Ok( "some string output" )
        } )

        get( "/successfulStringGetTry" )( handleOutput {
            Success( Ok( "some string output" ) )
        } )

        get( "/successfulIntGet" )( handleOutput {
            Ok( 43532 )
        } )

        get( "/successfulLongGet" )( handleOutput {
            Ok( 24234242342342L )
        } )

        get( "/successfulDoubleGet" )( handleOutput {
            Ok( 3.140923456789E200D )
        } )

        get( "/successfulDoubleGetRaw" )( handleOutput {
            3.140923456789E200D
        } )

        get( "/successfulFloatGet" )( handleOutput {
            Ok( 3.1409235E30F )
        } )

        get( "/badParameterFailure" )( handleOutput {
            val p = params.get( "param" )
            throw new BadQueryParameterException( "param", p, "CORRECT FORMAT" )
        } )

        get( "/badParameterFailureTry" )( handleOutput {
            val p = params.get( "param" )
            Failure( new BadQueryParameterException( "param", p, "CORRECT FORMAT" ) )
        } )

        post( "/badRequestBodyFailure" )( handleOutput {
            val body = request.body
            val requestObj = unmarshalTo( body, classOf[ TestObj ] ).get
            throw new BadRequestBodyException( "string_field", Some( requestObj.stringField ), "string_field should contain no spaces" )
        } )

        get( "/notFoundFailure/:resource" )( handleOutput {
            val resource = params( "resource" )
            throw new ResourceNotFoundException( "notFoundFailure resource", Some( resource ) )
        } )

        get( "/serviceNotReachableFailure" )( handleOutput {
            throw new ServiceUnreachableException( "resource datastore" )
        } )

        get( "/authenticationFailure" )( handleOutput {
            throw new AuthenticationException( "could not authenticate" )
        } )

        get( "/authorizationFailure" )( handleOutput {
            throw new AuthorizationException( "operation not authorized" )
        } )

        get( "/genericServerFailure" )( handleOutput {
            throw new GenericServerException( "some failure that needs to be reported to user" )
        } )

        get( "/genericServerFailureWithException" )( handleOutput {
            Try( throw new NullPointerException( "this is scala; where did this NPE come from ??" ) ) match {
                case Failure( e : NullPointerException ) => throw new GenericServerException( "threw an NPE", e )
                case _ => Ok()
            }
        } )

        get( "/otherFailure" )( handleOutput {
            throw new NullPointerException( "this is scala; where did this NPE come from??" )
        } )

    }

    addServlet( new TestDartServlet, "/*" )

    behavior of "DartScalatraServlet"

    it should "return an appropriate 405 error response when a bad method is used" in {
        post( "/overrideContentType" ) {
            status shouldBe 405
            body shouldBe marshalFrom( FailureResponse( 405, "POST is not allowed on /overrideContentType" ) ).get
        }
    }

    it should "return an appropriate 404 error response when a endpoint doesn't exist" in {
        get( "/nonExistentEndpoint" ) {
            status shouldBe 404
            body shouldBe marshalFrom( FailureResponse( 404, "Resource not found: /nonExistentEndpoint does not exist" ) ).get
        }
    }

    it should "set content type to application/json;charset=utf-8 when standardBefore is called" in {
        get( "/successfulJacksonObject" ) {
            response.getContentType() shouldBe "application/json;charset=utf-8"
        }
    }

    it should "override standardBefore's content type when contentType is assigned in a route action" in {
        get( "/overrideContentType" ) {
            response.getContentType() shouldBe "straight up bananas; charset=ISO-8859-1"
        }
    }

    it should "successfully marshalFrom( a jackson-formatted case class wrapped in a 200 action result" in {
        get( "/successfulJacksonObject" ) {
            status shouldBe 200
            body shouldBe testStr( "test output", 24 )
        }
    }

    it should "successfully marshalFrom( a jackson-formatted case class wrapped in a different action result" in {
        post( "/successfulJacksonObject" ) {
            status shouldBe 201
            body shouldBe testStr( "test output", 24 )
        }
    }

    it should "work with other methods besides GET and POST" in {
        put( "/successfulJacksonObject" ) {
            status shouldBe 200
            body shouldBe testStr( "test output", 24 )
        }

        patch( "/successfulJacksonObject" ) {
            status shouldBe 200
            body shouldBe testStr( "test output", 24 )
        }

        delete( "/successfulJacksonObject" ) {
            status shouldBe 200
            body shouldBe testStr( "test output", 24 )
        }
    }

    it should "successfully marshalFrom( a list of jackson-formatted objects" in {
        get( "/successfulListJacksonObject" ) {
            status shouldBe 200
            body shouldBe s"""[${testStr( "test output 1", 345 )},${testStr( "test output 2", 562 )}]"""
        }
    }

    it should "successfully marshalFrom( a map of string to jackson-formatted objects" in {
        get( "/successfulMapStringJacksonObject" ) {
            status shouldBe 200
            body shouldBe s"""{"field 1":${testStr( "test output 1", 345 )},"field 2":${testStr( "test output 2", 562 )}}"""
        }
    }

    it should "successfully marshalFrom( a map of string to string" in {
        get( "/successfulMapStringString" ) {
            status shouldBe 200
            body shouldBe s"""{"field 1":"value 1","field 2":"value 2","field 3":"value 3"}"""
        }
    }

    it should "successfully pass a string wrapped in an ActionResult through" in {
        get( "/successfulStringGet" ) {
            status shouldBe 200
            body shouldBe "some string output"
        }
    }

    it should "successfully pass a number value wrapped in an ActionResult through" in {
        get( "/successfulIntGet" ) {
            status shouldBe 200
            body shouldBe "43532"
        }

        get( "/successfulLongGet" ) {
            status shouldBe 200
            body shouldBe "24234242342342"
        }

        get( "/successfulDoubleGet" ) {
            status shouldBe 200
            body shouldBe "3.140923456789E200"
        }

        get( "/successfulFloatGet" ) {
            status shouldBe 200
            body shouldBe "3.1409235E30"
        }
    }

    it should "translate a thrown BadQueryParameterException into an appropriate failure response" in {
        get( "/badParameterFailure?param=bad-value" ) {
            status shouldBe 400
            body shouldBe failStr( 400, s"Bad request: invalid query: parameter param=bad-value does not conform to required format: CORRECT FORMAT" )
        }

        get( "/badParameterFailure" ) {
            status shouldBe 400
            body shouldBe failStr( 400, s"Bad request: invalid query: parameter param=<EMPTY> does not conform to required format: CORRECT FORMAT" )
        }
    }

    it should "translate a thrown BadRequestBodyException into an appropriate failure resposne" in {
        post( "/badRequestBodyFailure", body = testStr( "val with spaces", 987 ) ) {
            status shouldBe 400
            body shouldBe failStr( 400, "Bad request: invalid request body: field string_field=val with spaces does not conform to required format: string_field should contain no spaces" )
        }
    }

    it should "translate a thrown ResourceNotFoundException into an appropriate failure response" in {
        get( "/notFoundFailure/some-resource" ) {
            status shouldBe 404
            body shouldBe failStr( 404, """Resource not found: notFoundFailure resource \"some-resource\" does not exist""" )
        }
    }

    it should "translate a thrown ServiceUnreachableException into an appropriate failure response" in {
        get( "/serviceNotReachableFailure" ) {
            status shouldBe 503
            body shouldBe failStr( 503, "Service unavailable: unable to reach resource datastore" )
        }
    }

    it should "translate a thrown AuthenticationFailure exception into an appropriate failure response" in {
        get( "/authenticationFailure" ) {
            status shouldBe 401
            body shouldBe failStr( 401, "Failed to authenticate: unable to authenticate request: could not authenticate" )
        }
    }

    it should "translate a thrown AuthorizationFailure exception into an appropriate failure response" in {
        get( "/authorizationFailure" ) {
            status shouldBe 403
            body shouldBe failStr( 403, "Operation not authorized: operation not authorized" )
        }
    }

    it should "translate a thrown GenericServerException into a (500) response with a message" in {
        get( "/genericServerFailure" ) {
            status shouldBe 500
            body shouldBe failStr( 500, "Internal server error: some failure that needs to be reported to user" )
        }
    }

    it should "translate a thrown GenericServerException with an exception into a (500) response with a message" in {
        get( "/genericServerFailureWithException" ) {
            status shouldBe 500
            body shouldBe failStr( 500, "Internal server error: threw an NPE" )
        }
    }

    it should "translate any other thrown exception into an internal server error (500) response" in {
        get( "/otherFailure" ) {
            status shouldBe 500
            body shouldBe failStr( 500, "Internal server error" )
        }
    }

    it should "handle a Try directly" in {
        get( "/successfulJacksonObjectTry" ) {
            status shouldBe 200
            body shouldBe testStr( "test output", 24 )
        }

        get( "/successfulStringGetTry" ) {
            status shouldBe 200
            body shouldBe "some string output"
        }

        get( "/badParameterFailureTry?param=bad-value" ) {
            status shouldBe 400
            body shouldBe failStr( 400, s"Bad request: invalid query: parameter param=bad-value does not conform to required format: CORRECT FORMAT" )
        }
    }

    it should "automatically wrap a non-ActionResult result in a successful ActionResult (using Ok(): status = 200)" in {
        get( "/successfulJacksonObjectRaw" ) {
            status shouldBe 200
            body shouldBe testStr( "test output", 24 )
        }

        get( "/successfulDoubleGetRaw" ) {
            status shouldBe 200
            body shouldBe "3.140923456789E200"
        }
    }
}

class TestDummyServlet extends DartScalatraServlet {
    get( "/valid-endpoint" )( handleOutput {
        "response"
    } )
}

class DartScalatraServlet404RootTest extends StandardTestBase3x with ScalatraSuite {

    addServlet( new TestDummyServlet, "/*" )

    behavior of "DartScalatraServlet"

    it should "return the full path in a 404 response when it matches on root contoller" in {
        get( "/invalid-endpoint" ) {
            status shouldBe 404
            body shouldBe """{"status":404,"error_message":"Resource not found: /invalid-endpoint does not exist"}"""
        }
    }

    override def header = super.header
}

class DartScalatraServlet404PathTest extends StandardTestBase3x with ScalatraSuite {

    addServlet( new TestDummyServlet, "/some-path/*" )

    it should "return the full path in a 404 response when it matches a controller mounted on a simple path" in {
        get( "/some-path/invalid-endpoint" ) {
            status shouldBe 404
            body shouldBe """{"status":404,"error_message":"Resource not found: /some-path/invalid-endpoint does not exist"}"""
        }
    }

    override def header = super.header

}

class DartScalatraServlet404ComplexPathTest extends StandardTestBase3x with ScalatraSuite {

    addServlet( new TestDummyServlet, "/another/set/of/paths/*" )

    it should "return the full path in a 404 response when it matches a controller mounted on a complex path" in {
        get( "/another/set/of/paths/invalid-endpoint" ) {
            status shouldBe 404
            body shouldBe """{"status":404,"error_message":"Resource not found: /another/set/of/paths/invalid-endpoint does not exist"}"""
        }
    }

    override def header = super.header
}
