import sbt._

object Dependencies {

    val slf4jVersion = "1.7.20"
    val logbackVersion = "1.2.9"
    val betterFilesVersion = "3.8.0"

    val jacksonVersion = "2.9.9"

    val embeddedKafkaVersion = "2.2.0"
    val scalaTestVersion = "3.2.3"

    val scalatraVersion = "2.7.1"
    val servletApiVersion = "3.1.0"

    val dartCommonsVersion = "3.0.307"

    val logging = Seq( "org.slf4j" % "slf4j-api" % slf4jVersion,
                       "ch.qos.logback" % "logback-classic" % logbackVersion )

    val betterFiles = Seq( "com.github.pathikrit" %% "better-files" % betterFilesVersion )

    /// testing
    val scalaTest = Seq( "org.scalatest" %% "scalatest" % scalaTestVersion % Test )

    // JSON
    val jackson = Seq( "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
                       "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion )


    val scalatra = Seq( "org.scalatra" %% "scalatra" % scalatraVersion,
                        "javax.servlet" % "javax.servlet-api" % servletApiVersion,
                        "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % Test )

    val dartCommons = Seq( "com.twosixlabs.dart" %% "dart-utils" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-json" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-exceptions" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-test-base" % dartCommonsVersion % Test )
}
