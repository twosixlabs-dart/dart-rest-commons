# Dart REST Commons

Libraries used throughout DART for defining REST services consistently  
[![build and publish](https://github.com/twosixlabs-dart/dart-rest-commons/actions/workflows/build-and-publish.yml/badge.svg)](https://github.com/twosixlabs-dart/dart-rest-commons/actions/workflows/build-and-publish.yml)
## Overview

dart-rest-commons includes definitions of abstract servlet classes that can be mixed into 
servlet definitions in DART REST services to simplify and standardize error handling and 
serialization. There are versions for synchronous and asynchronous (`Future`-based) 
services.

It also includes definitions of standard exceptions as well as the base path used across all 
DART services.

## Dependencies

In addition to the publicly available third-party libraries it uses, dart-auth has dependencies
on a number of other Scala libraries. In order to build DART these dependencies must be
accessible via the local filesystem (in the SBT cache) or over the network via
[Sonatype Nexus](https://www.sonatype.com/products/repository-oss-download) where they are
published. dart-auth requires the following dependencies to be built/installed:

| Group ID              | Artifact ID          |
|-----------------------|----------------------|
| com.twosixlabs.dart   | dart-utils_2.12      |
| com.twosixlabs.dart   | dart-exceptions_2.12 |
| com.twosixlabs.dart   | dart-json_2.12       |
| com.twosixlabs.dart   | dart-test-base_2.12  |
| com.twosixlabs.cdr4s  | cdr4s-core_2.12      |

## Building

This project is built using SBT. For more information on installation and configuration
of SBT please [see the documentation](https://www.scala-sbt.org/1.x/docs/)

dart-auth is a library containing no runnable main classes. The only supported build tasks are
compilation, testing, and publication:

```bash
sbt clean         # clear out all build artifacts
sbt compile       
sbt test          # run all test suites
sbt publish       # publish all modules to maven
sbt publishLocal  # publish all modules locally
```
