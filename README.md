epubcheck-invoker
=================

A Java utility library to invoke EpubCheck as an external process.


## Compile

Run the tests with `mvn test`

Build the jar with `mvn package`

Run the integration tests with `mvn verify`

## Usage

Invoke EpubCheck pragrammatically by calling:

```Java
EpubcheckBackend.run("src/test/resources/epub/valid.epub");
```

EpubCheck is invoked as an external process (a new JVM is spawned). Its output is parsed and returned as a list of `Issue` objects. See the code of this class for more details on the API.
