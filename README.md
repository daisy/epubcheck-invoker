epubcheck-invoker
=================

[![Build Status](https://travis-ci.org/daisy/epubcheck-invoker.png?branch=master)](https://travis-ci.org/daisy/epubcheck-invoker)


A Java utility library to invoke EPUBCheck as an external process.


## Compile

Run the tests with `mvn test`

Build the jar with `mvn package`

Run the integration tests with `mvn verify`

## Usage

Invoke EPUBCheck pragrammatically by calling:

```Java
EpubCheckInvoker.run("src/test/resources/epub/valid.epub");
```

EPUBCheck is invoked as an external process (a new JVM is spawned). Its output is parsed and returned as a list of `Issue` objects. See the code of this class for more details on the API.
