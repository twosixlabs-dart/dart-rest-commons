import Dependencies._
import sbt._

organization in ThisBuild := "com.twosixlabs.dart.rest"
name := "dart-rest-commons"
scalaVersion in ThisBuild := "2.12.7"

resolvers in ThisBuild ++= Seq( "Spray IO Repository" at "https://repo.spray.io/",
                                "Maven Central" at "https://repo1.maven.org/maven2/",
                                "JCenter" at "https://jcenter.bintray.com",
                                "Local Ivy Repository" at s"file://${System.getProperty( "user.home" )}/.ivy2/local/default" )


publishTo in ThisBuild := {
    // TODO
    None
}

test in publish in ThisBuild := {}
test in publishLocal in ThisBuild := {}
publishMavenStyle := true

lazy val root = ( project in file( "." ) ).aggregate( scalatraCommons )

lazy val scalatraCommons = ( project in file( "dart-scalatra-commons" ) ).settings( libraryDependencies ++= scalatra
                                                                                                            ++ logging
                                                                                                            ++ dartCommons,
                                                                                    excludeDependencies ++= Seq( ExclusionRule( "org.slf4j", "slf4j-log4j12" ),
                                                                                                                 ExclusionRule( "org.slf4j", "log4j-over-slf4j" ),
                                                                                                                 ExclusionRule( "log4j", "log4j" ),
                                                                                                                 ExclusionRule( "org.apache.logging.log4j", "log4j-core" ) ) )

javacOptions in ThisBuild ++= Seq( "-source", "11", "-target", "11" )
scalacOptions in ThisBuild += "-target:jvm-1.8"
