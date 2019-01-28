# Creating Simple Configurable Spark Applications in Scala


- [Audience](#audience)
- [Motivation](#motivation)
- [`spark-utils`](#spark-utils)
  - [Logic](#logic)
  - [Configuration](#configuration)
  - [Execution](#execution)
- [Demos](#demos)
  - [Preparations](#preparations)
  - [Simple Record Counting](#simple-record-counting)
  - [Simple Lines Counting](#simple-lines-counting)

## Audience

Developers starting up into the Apache Spark application development in Scala.

Some basic Scala knowledge are required, but nothing too advanced.

Also basic Apache Spark knowledge is crucial to make sense of this.

## Motivation

All of us can recall the first days of picking up a new technology and running the first “Hello World” or “Count words”
applications that get us started with a new language or a platform.

Up to a certain point of exploring and creating demos or pilots everything it is nice, but when it comes to creating
*production ready* configurable applications, we all have a hard time and actually start thinking about the operational
use of our applications and how they will be deployed to a production system.
This is the moment where a few lines of beautiful code tend to get cluttered by a lot of configuration, wiring
and setup.

When it comes to Spark, it gets even more complicated, setting up the context and setting up the input sources and outputs.
It would be really nice to have a simple framework that keeps our Spark code clean and uncluttered.

## spark-utils

`spark-utils` is a simple framework, developed in a trickled fashion across a few years of writing Spark applications
that so far helped me starting up new projects and creating applications fast and relatively easy.

The main ideas behind building a brand new Spark application are *logic*, *configuration* and *execution*.

### Logic
Any Spark application, or, as a matter of fact, any application needs to **do something**,
otherwise it is not very useful. The **something** can be as simple as counting words and as complex as needed.

The logic is implemented through the `SparkRunnable` trait, which is comes down to implementing a single function, `run()`.

```scala
trait SparkRunnable[Context, Result]  {
    def run(implicit spark: SparkSession, context: Context): Result
}
```

We can see that the `run()` function takes two parameters, one being an active `SparkSession` and `context` which represents
the application context, and it can be anything, containing IO configurations or specific application parameters.
The most popular choice is the use of a case class.

Let's examine the most simple example, available in
[LinesCount01](https://github.com/tupol/spark-utils-demos/blob/master/src/main/scala/org/tupol/sparkutils/demos/LinesCount01.scala).
```scala
  override def run(implicit spark: SparkSession, context: LinesCount01Context): Unit =
    println(s"There are ${spark.source(context.input).read.count} lines in the ${context.input.path} file.")
```
It can be that simple or as complex as necessary.

The `Result` itself can be anything or nothing, leaving it to the developer to define. It is very useful however to have
a meaningful return type for unit and integration tests.
In the example above we might return the actual count so it will be easy to test the `run()` function itself rather than
it's side-effects.

At this point there are two reasonable questions:

***1) Where is the spark session coming from?***

Normally the developer does not implement the `SparkRunnable` trait directly, but implements it through the `SparkApp`
trait, which, in turn, extends `SparkRunnable`.

In a few strokes, the `SparkApp` looks like the following:

```scala
trait SparkApp[Context, Result] extends SparkRunnable[Context, Result] with Logging {
    def appName: String = getClass.getSimpleName.replaceAll("\\$", "")
    def buildConfig(config: Config): Context
    def main(implicit args: Array[String]): Unit = {...}
}
```

We will cover more about the functions here in the next sections.

About the `main()` function we will find more details in the [Setup](#setup) section.

***2) What about that configuration?***

About the `buildConfig()` and `appName()` functions we will find out more in the [Context](#configuration) section.

***3) What is the `spark.source()` function and where is it coming from?***

The `spark.source()` function is provided by the [`DataSource`](https://github.com/tupol/spark-utils/blob/master/docs/data-source.md)
framework and together with the corresponding configurations it can make reading input files really easy.


### Configuration
In order to perform the logic we usually need at least one of the two: an input and an output.
Also, some logic specific parameters might be necessary as well. We need to be able to pass all these parameters to
our **logic** as easy and transparent as possible.

By default, the `SparkApp` will use the simple class name as the application name. This can be useful when running the
application in Yarn, as it gives some indication on what is actually running, rather than some random name. The developer
is free to overwrite the `appName()` function to get a better name for the application.

The more challenging part is defining the actual application configuration.
Currently, the `SparkApp` requires the implementation of the `buildConfig()` function, that creates an application
configuration instance out of a given Typesafe Config instance.

Why use the [Typesafe Context](https://github.com/typesafehub/config) framework?
This framework provides a much more flexible way of dealing with application configuration, which goes beyond the
popular Java properties way, by adding both Jason and Hocon support
Other choices might be considered in the future, like Yaml. For now, being able to express a variety of types
in the configuration, including sequences and maps, also in a structured way, it is more than powerful enough to
reach or configuration goal. The principle of *least power* comes to mind.

Ok, but how do we create the application configuration out of the Typesafe Config instance?
The developer can always go and write some custom code to do it, but there is always a configuration framework in place
that can help a lot.

An example of an application that needs and input an output and some string parameter in the context configuration is
shown in the code sample bellow:

```scala
import org.tupol.spark.io.{ FileSinkConfiguration, FileSourceConfiguration }
import org.tupol.utils.config.Configurator

case class SomeApplicationContext(input: FileSourceConfiguration, output: FileSinkConfiguration, wordFilter: String)
object SomeApplicationContext extends Configurator[SomeApplicationContext] {
  import com.typesafe.config.Config
  import org.tupol.utils.config._
  import scalaz.ValidationNel
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, RecordsFilterContext] = {
    config.extract[FileSourceConfiguration]("input") |@|
    config.extract[FileSinkConfiguration]("output") |@|
    config.extract[String]("word-filter") apply
    SomeApplicationContext.apply
  }
}
```

An in-depth description of the configuration framework used is available in the
[`scala-utils`](https://github.com/tupol/scala-utils/blob/master/docs/configuration-framework.md) project.


### Execution
In order to get the Spark application going we need to create the Spark context or session.

The `spark-utils` framework is providing the **execution** out of the box, it is helping with creating the application
configuration and focuses the effort on creating the actual logic. The framework comes out of the box with
configuration tools for inputs and outputs, leaving for the developer to create the other configuration parameters and
assemble them into a case class.

The setup is done through the `main()` function, which makes any application implementing `SparkApp` an actual Spark
application. The following steps will be performed:

1. Configuration initialization and application context creation
2. Spark session creation
3. Execution of the `run()` function
4. Logging the steps and the success or failure of the application
5. Return the result and exit

The external configuration is passed to the `main()` function in the following ways, in the order specified bellow:
1. Application parameters; they are passed in a properties style, separated by whitespaces, like
    `app.name.param1=param1value app.name.param2=param2value`.
2. Configuration file; passed as an argument to `spark-submit --files=..../application.conf`
3. Configuration file `application.conf`, if available in the classpath.
4. Reference configuration file; it can be in the application jar itself as `reference.conf`.
The order is important because the a parameter defined in the application parameters overwrites the parameter
    with the same name defined in the application.conf, which in turn overwrites the parameter with the same name
    from the `reference.conf`.

### Failure\[T\] is Always an Option\[T\]
Almost all the functions exposed by this API van fail so write the code accordingly.
In the past all of these functions used to return a `Try[T]` but the API looks much cleaner this way and gives more power
and more responsibility to the developer.


## Demos

### Preparations

In order to build this project and run the demos one needs to have Scala and SBT installed on the local machine.

The project can be cloned from [here](https://github.com/tupol/spark-utils-demos).

```bash
git clone https://github.com/tupol/spark-utils-demos.git
```

To build and assemble the project run

```bash
cd spark-utils-demos
sbt compile assembly
```


### Simple Record Counting

One of the most basic example when starting with Apache Spark tutorials is counting lines and words.

In this demo we will examine counting the records out of any defined input source.

The source code for this demo is available in
[RecordsCount01.scala](src/main/scala/org/tupol/sparkutils/demos/RecordsCount01.scala).

#### Defining the Application Context

We need to define application context, which needs to contain the configuration or definition of the input source file.
We can achieve this by using the [`DataSource`](https://github.com/tupol/spark-utils/blob/master/docs/data-source.md)
framework.

```scala
import org.tupol.spark.io._
case class RecordsCount01Context(input: FileSourceConfiguration)
```

The `FileSourceConfiguration` contains all the various options of configuring a Spark file input source.
It supports all the formats currently supported by Spark out of the box, like `text`, `csv`, `json`, `parquet` and `orc`.
Additional formats like `avro` and `xml` are also supported.
All the necessary details about the configuration options and usage are available
[here](https://github.com/tupol/spark-utils/blob/master/docs/file-data-source.md).

So far so good, but this is not enough, since we have to define the `SparkApp.createContext()` function, so we need
to be able to take a Typesafe `Config` instance and create our `RecordsCount01Context` application context.

We can create a companion object to our context class that can provide the context factory functionality.
In this case we are using the
[`Configuration Framework`](https://github.com/tupol/scala-utils/blob/master/docs/configuration-framework.md) that
gives us a DSL to extract and validate the given configuration.
The following lines might look a little scary for the developers just starting with Scala, but it is not necessary
to fully understand the internal to use this pattern:

```scala
object RecordsCount01Context extends Configurator[RecordsCount01Context] {
  import com.typesafe.config.Config
  import org.tupol.utils.config._
  import scalaz.ValidationNel

  override def validationNel(config: Config): ValidationNel[Throwable, RecordsCount01Context] = {
    config.extract[FileSourceConfiguration]("input")
      .map(new RecordsCount01Context(_))
  }
}
```
By extending the `Configurator[T]` trait we need to provide the implementation of the `validationNel()` function.
In short, a `ValidationNel` holds wither an accumulated list of `Throwable` or the actual result if there were no exceptions.
We will see more advanced examples in the next demos.

The configuration framework provides a simple way of extracting the data source configuration out of the Typesafe
`Config`.
The main function provided by the framework is the `config()` function, which takes a type parameter telling what is the
type of the class being extracted and the path inside the configuration to extract it from.
```scala
  config.extract[FileSourceConfiguration]("input")
```

So far we decided on how out application context should look like and how we can build it from a configuration instance.

#### Creating the Application

In order to create our executable Spark application we need to create an object that extends the `SparkApp` trait and
defining the type of the application context and the return type. In our case we decide to return nothing and use the
previously defined `RecordsCount01Context` as our application context.

```scala
object RecordsCount01 extends SparkApp[RecordsCount01Context, Unit] {
  ...
}
```

At this point we have to define two more things: the context creation and the run functions.

In the case of the context creation we can just refer to the previously defined `RecordsCount01Context` factory.

```scala
  override def createContext(config: Config): RecordsCount01Context =
    RecordsCount01Context(config).get
}
```

Notice that we are calling the `get()` function on our factory. Our `createContext()` function needs to return a context
so we kind of have to. But not to worry, the execution will take care of the exception handling.

The last and most important step, we need to define out **logic** by implementing hte `run()` function.

```scala
  override def run(implicit spark: SparkSession, context: RecordsCount01Context): Unit =
    println(s"There are ${spark.source(context.input).read.count} records in the ${context.input.path} file.")
}
```

Out application is quite concise and clean, covering very few lines of code

```scala
object RecordsCount01 extends SparkApp[RecordsCount01Context, Unit] {

  override def createContext(config: Config): RecordsCount01Context =
    RecordsCount01Context(config).get

  override def run(implicit spark: SparkSession, context: RecordsCount01Context): Unit =
    println(s"There are ${spark.source(context.input).read.count} records in the ${context.input.path} file.")
}
```


#### Running the Demo

Run the demo with the following command:
```
spark-submit  -v --master local \
--deploy-mode client \
--class org.tupol.sparkutils.demos.RecordsCount01 \
target/scala-2.11/spark-utils-demos-assembly.jar \
RecordsCount01.input.format="text" \
RecordsCount01.input.path="README.md"
```

To check the results one needs to read the console output to find a line like
`There are ... records in the .... file.`

Notice how the configuration parameters are passed on to our Spark application:
```
RecordsCount01.input.format="text"
RecordsCount01.input.path="README.md"
```

All the application parameters need to be prefixed by the application name, as defined by the `appName()` function.

The application parameters in our case define the input source, as we defined it in the `RecordsCount01` factory.
All the parameters that define the input source are defined with the `RecordsCount01.input` prefix.

Let's see what happens if we do not specify the format by running the following command:
```
spark-submit  -v --master local \
--deploy-mode client \
--class org.tupol.sparkutils.demos.RecordsCount01 \
target/scala-2.11/spark-utils-demos-assembly.jar \
RecordsCount01.input.path="README.md"
```
We should see in the logs and exception like the following:
```
...
2019-01-28 07:28:27 ERROR RecordsCount01:75 - RecordsCount01: Job failed.
ConfigurationException[Invalid configuration. Please check the issue(s) listed bellow:
- Invalid configuration. Please check the issue(s) listed bellow:
- No configuration setting found for key 'format']
...
```

### Simple Lines Counting

In the simple record count demo we counted the lines of any given input file, no matter the format.

Let's adjust the example so we only accept text files and we count the lines

The only change we need to do to the records count is to enforce the input format to be text.
The best way to do it is through the application context factory, after extracting the `FileSourceConfiguration`.
The way to do it is pretty simple, using the `ensure()` function as in the example below:
```scala
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only text files are supported").toNel)(source => source.format == FormatType.Text)
```

The source code for this demo is available in
[LinesCount01.scala](src/main/scala/org/tupol/sparkutils/demos/LinesCount01.scala).


#### Running the Demo

Run the demo with the following command:
```
spark-submit  -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.LinesCount01 \
target/scala-2.11/spark-utils-demos-assembly.jar \
LinesCount01.input.format="text" \
LinesCount01.input.path="README.md"
```

To check the results one needs to read the console output to find a line like
`There are ... lines in the .... file.`

The count should match the one from the simple record counting demo.

Let's try to see what happens if the format is other than text, by running the following command:
```
spark-submit  -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.LinesCount01 \
target/scala-2.11/spark-utils-demos-assembly.jar \
LinesCount01.input.format="json" \
LinesCount01.input.path="README.md"
```

We should get an exception in the logs like the following:
```
...
2019-01-27 07:01:10 ERROR LinesCount01:75 - LinesCount01: Job failed.
ConfigurationException[Invalid configuration. Please check the issue(s) listed bellow:
- Only text files are supported]
...
```
