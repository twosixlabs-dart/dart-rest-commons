package com.twosixlabs.dart.rest.scalatra

import com.twosixlabs.dart.exceptions.Exceptions.getStackTraceText
import com.twosixlabs.dart.exceptions.{AuthenticationException, AuthorizationException, BadQueryParameterException, BadRequestBodyException, GenericServerException, ResourceNotFoundException, ServiceUnreachableException}
import com.twosixlabs.dart.json.JsonFormat.{marshalFrom, unmarshalTo}
import com.twosixlabs.dart.rest.scalatra.models.{Constants, FailureResponse}
import org.scalatra.{ActionResult, BadRequest, Forbidden, InternalServerError, MethodNotAllowed, NotFound, NotImplemented, Ok, RouteTransformer, ScalatraServlet, ServiceUnavailable, Unauthorized}
import org.slf4j.{Logger, LoggerFactory}

import javax.servlet.http.HttpServletRequest
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Dart Scalatra rest controller classes should extend this or AsyncDartScalatraServlet traits
  * to get the following 3 basic features:
  *   1) use handleOutput method to a) marshal results to json and b) convert standard exceptions
  *      (mainly DartRestExceptions) to standard error responses
  *   2) generate dart standard error messages for 404 and 405 responses instead of standard jetty
  *      pages
  *   3) use standardBefore method to a) log all requests and b) set contentType to correct header
  *      for json response (application/json; charset=utf-8)
  */
trait DartScalatraServlet extends ScalatraServlet {

    // JsonUtility methods taken from JsonFormat
    def marshal[ T ]( obj : T ) : Try[ String ] = marshalFrom( obj )
    def unmarshal[ T : ClassTag ]( json : String, klass : Class[ T ] ) : Try[ T ] = unmarshalTo[ T ]( json, klass )

    /**
      * Call this method at the top of your controller class to set up two standard actions at
      * the beginning of each route action you define:
      * 1) log (debug only) the request to that endpoint (captures both method and full url including query)
      * 2) set the contentType to "application/json; charset=utf-8"
      *
      * Note that the content type assignment can be overridden by calling "contentType = ... "
      * in your route action.
      *
      * @param transformers
      */
    def setStandardConfig( transformers : RouteTransformer* ) : Unit = before( transformers : _* ) {
        logRouteDebug
        setContentTypeJson()
    }

    /**
      * Executes a code block, catching all exceptions and generating an appropriate ActionResult.
      * It should be used as a wrapper around all route actions. Ex:
      *
      * get( "/some/route" )( handleOutput {
      * // code goes here
      * result
      * } )
      *
      * In the above example the result can be of any type. It will handle results the following way:
      * 1) strings or numbers will be passed through as is with a 200 status
      * 2) objects will be marshalled into json strings and sent with a 200 status
      * 3) ActionResults will be passed through, but their bodies will be marshalled if they are
      * objects and not string or text. In this way you can send 201 responses by returning
      * Create( result ) instead of just result.
      * 4) a result of type Try[ _ ] will be matched for errors and the successful case will be
      * handled as in cases 1-3.
      *
      * Error handling works as following:
      * 1) DartRestException errors will be converted into appropriate 4xx and 5xx responses with
      * standard error messages based on the exceptions' messages
      * 2) scala.NotImplementedError will return a 501 response with a standard error message
      * 3) all other exceptions will generate a 500 response with a simple "Internal Server
      * Error" message
      *
      * @param attempt
      * @return ActionResult
      */
    def handleOutput( attempt : => Any ) : ActionResult = Try( attempt ) match {
        case Success( res : Try[ _ ] ) => handleOutputTry( res )
        case res : Try[ _ ] => handleOutputTry( res )
    }

    /**
      * If you don't use standardBefore (see above) you can use logRoute to log requests to an
      * endpoint. It prints out the method and the whole url, including query string.
      *
      * The request parameter will be provided implicitly by the servlet context, so it isn't
      * necessary to pass it explicitly.
      *
      * @param request
      */
    def logRoute( implicit request : HttpServletRequest ) : Unit = {
        val queryString = {
            if ( request.queryString == null || request.queryString == "" ) ""
            else s"?${request.queryString}"
        }

        LOG.info( s"${request.getMethod.toUpperCase} ${request.getRequestURL.toString}${queryString}" )
    }

    def logRouteDebug( implicit request : HttpServletRequest ) : Unit = if ( LOG.isDebugEnabled ) {
        val queryString = {
            if ( request.queryString == null || request.queryString == "" ) ""
            else s"?${request.queryString}"
        }

        LOG.debug( s"${request.getMethod.toUpperCase} ${request.getRequestURL.toString}${queryString}" )
    }

    val LOG : Logger = LoggerFactory.getLogger( getClass )

    def setContentTypeJson( ) : Unit = contentType_=( Constants.CONTENT_TYPE_JSON )

    notFound( resourceNotFound( s"${routeBasePath + requestPath} does not exist" ) )

    methodNotAllowed( _ => badMethod( request.getMethod, routeBasePath + requestPath ) )

    def getMessage( source : Any ) : String = source match {
        case e : Throwable => e.getMessage
        case str : String => str
        case e : Any => e.toString
    }

    def badMethod( method : String, path : String ) : ActionResult = {
        val message = s"${method.toUpperCase} is not allowed on ${path}"
        val result = FailureResponse( 405, message )
        LOG.warn( message )

        MethodNotAllowed( marshal( result ).get )
    }

    def badRequest( source : Any ) : ActionResult = {
        val message = s"Bad request: ${getMessage( source )}"
        val result = FailureResponse( 400, message )
        LOG.warn( message )

        BadRequest( marshal( result ).get )
    }

    def resourceNotFound( source : Any ) : ActionResult = {
        val detail = source match {
            case e : Throwable => getMessage( e )
            case str : String => getMessage( str )
            case other : Any => other.toString
        }
        val message = s"Resource not found: ${detail}"
        val result = FailureResponse( 404, message )
        LOG.warn( message )

        NotFound( marshal( result ).get )
    }


    def serviceUnavailable( source : Any ) : ActionResult = {
        val detail = source match {
            case e : Throwable => getMessage( e )
            case str : String => getMessage( str )
            case other : Any => other.toString
        }
        val message = s"Service unavailable: ${detail}"
        val result = FailureResponse( 503, message )
        LOG.warn( message )

        ServiceUnavailable( marshal( result ).get )
    }

    def authenticationFailure( source : Any ) : ActionResult = {
        val detail = source match {
            case e : Throwable => getMessage( e )
            case str : String => getMessage( str )
            case other : Any => other.toString
        }
        val message = s"Failed to authenticate: ${detail}"
        val result = FailureResponse( 401, message )
        LOG.warn( message )

        Unauthorized( marshal( result ).get )
    }

    def authorizationFailure( source : Any ) : ActionResult = {
        val detail = source match {
            case e : Throwable => getMessage( e )
            case str : String => getMessage( str )
            case other : Any => other.toString
        }
        val message = s"Operation not authorized: ${detail}"
        val result = FailureResponse( 403, message )
        LOG.warn( message )

        Forbidden( marshal( result ).get )
    }

    def internalServerError( source : Any ) : ActionResult = {
        source match {
            case e : GenericServerException =>
                LOG.error( s"Generic server error: ${e.getMessage}" )
                if (e.getCause != null) LOG.error( s"Caused by: ${e.getCause.getClass.toString}: ${e.getCause.getMessage}" )
                LOG.error( getStackTraceText( e ) )
            case e : Throwable =>
                LOG.error( s"${e.getClass}: ${e.getMessage}" )
                LOG.error( getStackTraceText( e ) )
            case str : String =>
                LOG.error( str )
                getMessage( str )
            case other : Any =>
                LOG.error( other.toString )
        }
        val message = source match {
            case e : GenericServerException => s"Internal server error: ${e.getMessage}"
            case _ => "Internal server error"
        }
        val result = FailureResponse( 500, message )

        InternalServerError( marshal( result ).get )
    }

    def serviceNotImplemented : ActionResult = {
        val message = "Service not yet implemented"
        val result = FailureResponse( 501, message )
        LOG.warn( message )

        NotImplemented( marshal( result ).get )
    }

    private def marshalOutput( output : Any ) : Try[ String ] = output match {
        case str : String => Success( str )
        case other : Any => marshal( other )
    }

    private def handleOutputTry( output : Try[ Any ] ) : ActionResult = output match {
        // Make sure body is serialized -- if it is already a string, nothing will happen to it
        case Success( result : ActionResult ) => result.body match {
            case () => result
            case _ : Any => marshalOutput( result.body ) match {
                case Success( str ) => result.copy( body = str )
                case Failure( e : Throwable ) => internalServerError( e )
            }
        }
        case Success( result : Any ) => marshalOutput( result ) match {
            case Success( str ) => Ok( str )
            case Failure( e : Throwable ) => internalServerError( e )
        }
        case Failure( e : GenericServerException ) => internalServerError( e )
        case Failure( e : BadQueryParameterException ) => badRequest( e )
        case Failure( e : BadRequestBodyException ) => badRequest( e )
        case Failure( e : ServiceUnreachableException ) => serviceUnavailable( e )
        case Failure( e : ResourceNotFoundException ) => resourceNotFound( e )
        case Failure( e : AuthenticationException ) => authenticationFailure( e )
        case Failure( e : AuthorizationException ) => authorizationFailure( e )
        case Failure( _ : NotImplementedError ) => serviceNotImplemented
        case Failure( e : Throwable ) => internalServerError( e )
    }

}

object DartScalatraServletMethods extends DartScalatraServlet
