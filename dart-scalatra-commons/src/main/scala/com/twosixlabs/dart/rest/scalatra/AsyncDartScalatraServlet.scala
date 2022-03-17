package com.twosixlabs.dart.rest.scalatra

import com.twosixlabs.dart.rest.scalatra.models.Constants

import javax.servlet.http.HttpServletRequest
import org.scalatra.{ActionResult, FutureSupport, RouteTransformer, ScalatraServlet}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Version of DartScalatraServlet with support for asynchronous responses (using Future). See
  * DartScalatraServlet for usage, which is nearly identical. See method comments below for
  * important differences.
  *
  * One thing to note is that the DartScalatraServlet helper methods other than logRoute
  * and standardBefore are not available. To use these, you can import
  * com.twosixlabs.dart.rest.scalatra.DartScalatraServletMethods.
  *
  * You will need to override the executor method when you extend this class.
  * The simplest value to use is scala.concurrent.ExecutionContext.global:
  *
  *   protected implicit def executor : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  */
abstract class AsyncDartScalatraServlet extends ScalatraServlet with FutureSupport {

    /**
      * Can handle all outputs handled by DartScalatraServlet.handleOutput, as well as these
      * outputs wrapped in a Future {}.
      *
      * Note: this will cache the request object (which is mutable and therefore a problem
      * for accessing things like request.getContentType() or params()). It handles the route
      * action in the context of the cached request instead of the mutable version. The
      * result is that you don't have to worry about calling params() or reading any request
      * properties in a concurrent process.
      *
      * @param attempt
      * @param request
      * @return
      */
    def handleOutput( attempt : => Any )( implicit request : HttpServletRequest ) : Future[ ActionResult ] = Try{
        val req = request
        withRequest( req ) {
            attempt
        }
    } match {
        case Success( res : Future[ _ ] ) => handleOutputFuture( res )
        case Success( res : Any ) => Future.successful( DartScalatraServletMethods.handleOutput( res ) )
        case Failure( e : Throwable ) => Future.successful( DartScalatraServletMethods.handleOutput( throw e ) )
    }

    def logRouteDebug( implicit request : HttpServletRequest ) : Unit = DartScalatraServletMethods.logRouteDebug

    def logRoute( implicit request : HttpServletRequest ) : Unit = DartScalatraServletMethods.logRoute

    def setContentTypeJson() : Unit = contentType_=( Constants.CONTENT_TYPE_JSON )

    def setStandardConfig( transformers : RouteTransformer* ) : Unit = before( transformers : _* ) {
        logRoute
        setContentTypeJson()
    }

    notFound( pathNotFound )

    methodNotAllowed( _ => DartScalatraServletMethods.badMethod( request.getMethod, routeBasePath + requestPath ) )

    def pathNotFound( implicit req : HttpServletRequest ) : ActionResult =
        DartScalatraServletMethods.resourceNotFound( s"${routeBasePath( req ) + requestPath( req )} does not exist" )

    private def handleOutputFuture( futResult : Future[ Any ] ) : Future[ ActionResult ] = {
        futResult.map( ( res : Any ) => {
            DartScalatraServletMethods.handleOutput( res )
        } ).recover {
            case e : Throwable => DartScalatraServletMethods.handleOutput( throw e )
        }
    }

}
