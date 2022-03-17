package com.twosixlabs.dart.rest.scalatra.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class HealthCheckResponse( @BeanProperty @JsonProperty( "status" ) status : String,
                                @BeanProperty @JsonProperty( "message" ) message : Option[ String ] = None,
                                @BeanProperty @JsonProperty( "version" ) version : Option[ String ] = None )
