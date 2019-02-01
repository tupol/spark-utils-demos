# Creating Simple Configurable Spark Applications in Scala

- [Introduction](#introduction)
  - [Motivation](#motivation)
  - [Audience](#audience)
- [`spark-utils`](#spark-utils)
  - [Logic](#logic)
  - [Configuration](#configuration)
  - [Execution](#execution)
- [Demos](#demos)
  - [Preparations](#preparations)
  - [Simple Record Counting](#simple-record-counting)
  - [Simple Lines Counting](#simple-lines-counting)
  - [Filtered Lines Counting](#filtered-lines-counting)
  - [Optionally Filtered Lines Counting](#optionally-filtered-lines-counting)
  - [Writing Filtered Lines](#writing-filtered-lines)
  - [Writing Filtered Lines by a Sequence of Words](#writing-filtered-lines-by-a-sequence-of-words)
  - [MySparkApp More Tips and Tricks](#mysparkapp-more-tips-and-tricks)
- [References and Links](#references-and-links)


## Introduction

### Motivation

All of us can recall the first days of picking up a new technology and running the first “*Hello World*” or “*Count words*”
applications that get us started with a new language or a platform.

Up to a certain point of exploring and creating demos or prototypes everything is nice, but when it comes to creating
*production ready* configurable applications we all have a hard time and actually start thinking about the operational
use of our applications and how they will be deployed to a production system. This is the moment when a few lines of
beautiful code tend to get cluttered by a lot of configuration, wiring and setup.

When it comes to Apache Spark it gets even more complicated, setting up the Spark context and setting up the input
sources and outputs.
It would be really nice to have a simple framework that keeps our Spark code clean and uncluttered.



### Audience

Developers starting up into the [Apache Spark](https://spark.apache.org/) application development
in [Scala](https://www.scala-lang.org/).

Developers starting up into the Apache Spark application development in Scala.

Some basic Scala and Apache Spark knowledge is crucial to make sense of this presentation.

This article is not meant as a Scala or an Apache Spark tutorial.


## spark-utils

[`spark-utils`](https://github.com/tupol/spark-utils) is a simple framework, developed
across a few years of writing Spark applications that so far helped me starting up new projects and creating
applications fast and relatively easy.

The main ideas behind building a new Spark application are [*logic*](#logic), [*configuration*](#configuration) and
[*execution*](#execution).


### Logic
Any Spark application, or as a matter of fact any application, needs to **do something**, otherwise it is not very useful.
The **something** can be as simple as counting words and as complex as needed.

The logic is implemented through the `SparkRunnable` trait, which is comes down to implementing a single function, `run()`.

```scala
trait SparkRunnable[Context, Result]  {
    def run(implicit spark: SparkSession, context: Context): Result
}
```

We can see that the `run()` function takes two parameters, one being an active `SparkSession` and `context` which represents
the application context . The application context can be anything, containing IO configurations or specific application
configuration parameters. The most popular choice is the use of a case class.

Let's examine the most simple example, available in
[RecordsCount01](https://github.com/tupol/spark-utils-demos/blob/master/src/main/scala/org/tupol/sparkutils/demos/RecordsCount01.scala).
```scala
  override def run(implicit spark: SparkSession, context: LinesCount01Context): Unit =
    println(s"There are ${spark.source(context.input).read.count} records in the ${context.input.path} file.")
```

The `Result` itself can be anything or nothing, leaving it to the developer to define. It is very useful however to have
a meaningful return type for unit and integration tests.
In the example above, we might return the actual count so it will be easy to test the `run()` function itself rather than
it's side-effects.

At this point there are two reasonable questions:

***1) Where is the Spark session coming from?***

Normally the developer does not implement the [`SparkRunnable`](https://github.com/tupol/spark-utils/blob/master/docs/spark-runnable.md)
trait directly, but implements it through the [`SparkApp`](https://github.com/tupol/spark-utils/blob/master/docs/spark-app.md)
trait, which, in turn, extends `SparkRunnable`.

In a few strokes, the `SparkApp` looks like the following:

```scala
trait SparkApp[Context, Result] extends SparkRunnable[Context, Result] with Logging {
    def appName: String = getClass.getSimpleName.replaceAll("\\$", "")
    def createContext(config: Config): Context
    def main(implicit args: Array[String]): Unit = {...}
}
```

We will cover more about the functions here in the next sections.

The `main()` function is the one responsible for creating the Spark session.
We will find more details in the [Execution](#execution) section.

***2) What about that configuration?***

About the `createContext()` and `appName()` functions we will find out more in the [Configuration](#configuration) section.

***3) What is the `spark.source()` function and where is it coming from?***

The `spark.source()` function is provided by the [`DataSource`](https://github.com/tupol/spark-utils/blob/master/docs/data-source.md)
framework and together with the corresponding configurations it can make reading input files really easy.


### Configuration
In order to perform the logic, we usually need at least one of the two: an input and an output.
Also, some logic specific parameters might be necessary as well. We need to be able to pass all these parameters to
our **logic** as easy and transparent as possible.

By default, the `SparkApp` will use the simple class name as the application name. This can be useful when running the
application in Yarn, as it gives some indication on what is actually running, rather than some random name. The developer
is free to overwrite the `appName()` function to get a better name for the application.

The more challenging part is defining the actual application configuration.
Currently, the `SparkApp` requires the implementation of the `createContext()` function, that creates an application
context instance out of a given Typesafe Config instance.

Why use the [Typesafe Context](https://github.com/typesafehub/config) framework?
This framework provides a much more flexible way of dealing with application configuration, which goes beyond the
popular Java properties way, by adding both Jason and Hocon support.
Other choices might be considered in the future, like Yaml. For now, being able to express a variety of types
in the configuration, including sequences and maps, also in a structured way, it is more than powerful enough to
reach or configuration goal. The principle of *least power* comes to mind.

Ok, but how do we create the application configuration out of the Typesafe Config instance?
The developer can always go and write some custom code to do it, but there is already a configuration framework in place
that can help a lot.

An example of an application that needs and input an output and some string parameter in the context configuration is
shown in the code sample below:

```scala
import org.tupol.spark.io.{ FileSinkConfiguration, FileSourceConfiguration }
import org.tupol.utils.config.Configurator

case class SomeApplicationContext(input: FileSourceConfiguration, output: FileSinkConfiguration, wordFilter: String)
object SomeApplicationContext extends Configurator[SomeApplicationContext] {
  import com.typesafe.config.Config
  import org.tupol.utils.config._
  import scalaz.ValidationNel
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, SomeApplicationContext] = {
    config.extract[FileSourceConfiguration]("input") |@|
    config.extract[FileSinkConfiguration]("output") |@|
    config.extract[Option[String]]("wordFilter") apply
    SomeApplicationContext.apply
  }
}
```

An in-depth description of the configuration framework used is available in the
[`scala-utils`](https://github.com/tupol/scala-utils/blob/master/docs/configuration-framework.md) project.


### Execution
In order to get the Spark application going we need to create the Spark context or session.

The `spark-utils` framework is providing the **execution** support out of the box.
It helps with creating the application configuration and focuses the actual development effort on creating the
application logic.
The framework provides configuration tools for inputs and outputs, leaving for the developer to create the other
configuration parameters and assemble them into a case class.

The setup is done through the `main()` function, which makes any application implementing `SparkApp` an actual Spark
application. The following steps will be performed:

1. Configuration initialization and application context creation
2. Spark session creation
3. Execution of the `run()` function
4. Logging the steps and the success or failure of the application
5. Return the result and exit

The external configuration is passed to the `main()` function in the following ways, in the order specified below:

1. Application parameters; they are passed in a properties style, separated by whitespaces, like
    `app.name.param1=param1value app.name.param2=param2value`.
2. Configuration file; passed as an argument to `spark-submit --files=..../application.conf`
3. Configuration file `application.conf`, if available in the classpath.
4. Reference configuration file; it can be in the application jar itself as `reference.conf`.
The order is important because a parameter defined in the application parameters overwrites the parameter
    with the same name defined in the `application.conf`, which in turn overwrites the parameter with the same name
    from the `reference.conf`.

### Failure\[T\] is Always an Option\[T\]
Almost all the functions exposed by the `spark-utils` API can fail so write the code accordingly.
In the past all of these functions used to return a `Try[T]` but the API looks much cleaner this way and gives more power
and more responsibility to the developer.


## Demos

### Preparations
In order to build this project and run the demos one needs to have Scala, SBT and Git installed on the local machine.

* Java 6 or higher
* Scala 2.11 or 2.12
* Apache Spark 2.3.X


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
One of the most basic examples when starting with Apache Spark tutorials is counting lines and words.

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
to fully understand the internals to use this pattern:

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

So far, we decided on how our application context should look like and how we can build it from a configuration instance.

#### Creating the Application
In order to create our executable Spark application, we need to create an object that extends the `SparkApp` trait and
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

The last and most important step, we need to define our **logic** by implementing the `run()` function.

```scala
  override def run(implicit spark: SparkSession, context: RecordsCount01Context): Unit =
    println(s"There are ${spark.source(context.input).read.count} records in the ${context.input.path} file.")
}
```

Our application is quite concise and clean, covering very few lines of code

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
spark-submit -v --master local \
--deploy-mode client \
--class org.tupol.sparkutils.demos.RecordsCount01 \
target/scala-2.11/spark-utils-demos-assembly.jar \
RecordsCount01.input.format="text" \
RecordsCount01.input.path="README.md"
```

To check the results, one needs to read the console output to find a line like
```
There are ... records in the .... file.
```

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
spark-submit -v --master local \
--deploy-mode client \
--class org.tupol.sparkutils.demos.RecordsCount01 \
target/scala-2.11/spark-utils-demos-assembly.jar \
RecordsCount01.input.path="README.md"
```
We should see in the logs an exception like the following:
```
...
2019-01-28 07:28:27 ERROR RecordsCount01:75 - RecordsCount01: Job failed.
ConfigurationException[Invalid configuration. Please check the issue(s) listed below:
- Invalid configuration. Please check the issue(s) listed below:
- No configuration setting found for key 'format']
...
```

### Simple Lines Counting
In the simple record count demo, we counted the lines of any given input file, no matter the format.

Let's adjust the example so we only accept text files and we count the lines.

#### Defining the Application Context
The only change we need to do to the records count is to enforce the input format to be text.
The best way to do it is through the application context factory, after extracting the `FileSourceConfiguration`.
The way to do it is pretty simple, using the `ensure()` function as in the example below:
```scala
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only 'text' format files are supported").toNel)(source => source.format == FormatType.Text)
```

The source code for this demo is available in
[LinesCount01.scala](src/main/scala/org/tupol/sparkutils/demos/LinesCount01.scala).

#### Running the Demo
Run the demo with the following command:
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.LinesCount01 \
target/scala-2.11/spark-utils-demos-assembly.jar \
LinesCount01.input.format="text" \
LinesCount01.input.path="README.md"
```

To check the results, one needs to read the console output to find a line like
```
There are ... lines in the .... file.
```

The count should match the one from the simple record counting demo.

Let's try to see what happens if the format is other than text, by running the following command:
```
spark-submit -v \
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
ConfigurationException[Invalid configuration. Please check the issue(s) listed below:
- Only 'text' format files are supported]
...
```


### Filtered Lines Counting
In the simple lines count demo we counted the lines of any given input text file.

Let's adjust the example so we only count lines containing a certain word which should be passed in as a parameter.

The source code for this demo is available in
[LinesCount02.scala](src/main/scala/org/tupol/sparkutils/demos/LinesCount02.scala).

#### Defining the Application Context
We need to change the context configuration so that it will take an extra string parameter, let's call it `wordFilter`.

```scala
import org.tupol.spark.io._
case class LinesCount02Context(input: FileSourceConfiguration, wordFilter: String)
```

Also, we need to be able to extract this new parameter out of the configuration provided.

The context factory change is not big and it looks like the following:
```scala
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only 'text' format files are supported").toNel)(_.format == FormatType.Text) |@|
    config.extract[String]("wordFilter") apply
    LinesCount02Context.apply
```

You might have noticed the funky operator `|@|`. This operator is part of the [Scalaz](https://github.com/scalaz/scalaz)
DSL for constructing applicative expressions. It sounds more complicated that it is, so we can think of it as a way of
composing or extracting configurations into the final case class.
We will use this pattern a lot in the following examples and demos:
```scala
  ...
  override def validationNel(config: Config): ValidationNel[Throwable, NameOfOurContextCaseClass] = {
    config.extract[Something]("from_somewhere") |@|
    config.extract[SomethingElse]("from_somewhere_else") |@|
    config.extract[YetAnotherThing]("from_somewhere_different") apply
    NameOfOurContextCaseClass.apply
  }
  ...
```

Also, the extraction of the `wordFilter` parameter itself is as simple as `config.extract[String]("wordFilter")`.
The configuration framework provides extractors for most of the primitive types. It also provides some extractors 
for container types like `Option[T]`, `Either[L, R]`, `Seq[T]` and `Map[String, V]`.

#### Creating the Application
The logic for counting filtered lines is not very difficult, so our `run()` function remains trivial:
```scala
  override def run(implicit spark: SparkSession, context: LinesCount02Context): Unit = {
    import spark.implicits._
    val lines = spark.source(context.input).read.as[String]
    val count = lines.filter(line => line.contains(context.wordFilter)).count
    println(s"There are $count lines in the ${context.input.path} file containing the word ${context.wordFilter}.")
  }
```

#### Running the Demo
Run the demo with the following command:
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.LinesCount02 \
target/scala-2.11/spark-utils-demos-assembly.jar \
LinesCount02.input.format="text" \
LinesCount02.input.path="README.md" \
LinesCount02.wordFilter="Spark"
```

To check the results, one needs to read the console output to find a line like
```
There are ... lines in the ... file containing the word ....
```


### Optionally Filtered Lines Counting

Just for the sake of making our filtered lines counting application a little more complicated, let's say that the 
`wordFilter` parameter is optional.

The source code for this demo is available in
[LinesCount03.scala](src/main/scala/org/tupol/sparkutils/demos/LinesCount03.scala).

#### Defining the Application Context
The only change to the application context is to change the type of `wordFilter` from `String` to `Option[String]`.
The same change needs to be done in the application context factory as well, using
`config.extract[Option[String]]("wordFilter")` instead of `config.extract[String]("wordFilter")`.

#### Creating the Application
The logic for counting filtered lines is not very difficult, so our `run()` function remains trivial:
```scala
  override def run(implicit spark: SparkSession, context: LinesCount03Context): Unit = {
    import spark.implicits._
    val lines = spark.source(context.input).read.as[String]
    context.wordFilter match {
      case Some(word) =>
        val count = lines.filter(line => line.contains(word)).count
        println(s"There are $count lines in the ${context.input.path} file containing the word $word.")
      case None =>
        val count = lines.count
        println(s"There are $count lines in the ${context.input.path} file.")
    }
  }
```

#### Running the Demo
Run the demo with the defined filter using the following command:
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.LinesCount03 \
target/scala-2.11/spark-utils-demos-assembly.jar \
LinesCount03.input.format="text" \
LinesCount03.input.path="README.md" \
LinesCount03.wordFilter="Spark"
```

To check the results, one needs to read the console output to find a line like
```
There are ... lines in the ... file containing the word ....
```

Run the demo without the defined filter using the following command:
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.LinesCount03 \
target/scala-2.11/spark-utils-demos-assembly.jar \
LinesCount03.input.format="text" \
LinesCount03.input.path="README.md"
```

To check the results, one needs to read the console output to find a line like
```
There are ... lines in the ... file.
```


### Writing Filtered Lines

So far, we only defined an input source and we printed the results to the console.

Let's expand on our filtered lines counting application by writing the filtered lines to an output file rather than
just printing the count. This is much more useful in real-life applications.

The source code for this demo is available in
[LinesFilter01.scala](src/main/scala/org/tupol/sparkutils/demos/LinesFilter01.scala).

#### Defining the Application Context
As we have done already a couple of times, we need to define our new application context, which needs to include as well
the definition of the output. This comes down to two small changes in the configuration:
```scala
case class LinesFilter01Context(input: FileSourceConfiguration, output: FileSinkConfiguration, wordFilter: String)
```
Notice that we added the output definition in the form of a `FileSinkConfiguration`.
The `FileSinkConfiguration` contains all the various options of configuring a Spark writing to output files.
It supports all the formats currently supported by Spark out of the box, like `text`, `csv`, `json`, `parquet` and `orc`.
Additional formats like `avro` and `xml` are also supported.
All the necessary details about the configuration options and usage are available
[here](https://github.com/tupol/spark-utils/blob/master/docs/file-data-sink.md).
In addition to the `FileSinkConfiguration`, there is also a `JdbcSinkConfiguration`.

The application context factory needs to be adjusted as well, but the change remains trivial:
```scala
...
    config.extract[FileSinkConfiguration]("output")
      .ensure(new IllegalArgumentException("Only output text files are supported").toNel)(_.format == FormatType.Text) |@|
...

```
We essentially added the `config.extract[FileSinkConfiguration]("output")` line with a simple check on the type of the
file to write to.

#### Creating the Application
The logic for saving the filtered lines to the output file stubbornly remains very simple:
```scala
  override def run(implicit spark: SparkSession, context: LinesFilter01Context): Unit = {
    import spark.implicits._
    val lines = spark.source(context.input).read.as[String]
    def lineFilter(line: String): Boolean = line.contains(context.wordFilter)
    val filteredRecords = lines.filter(lineFilter(_)).toDF
    filteredRecords.sink(context.output).write
  }
```

The `filteredRecords.sink()` function is provided by the [`DataSink`](https://github.com/tupol/spark-utils/blob/master/docs/data-sink.md)
framework and together with the corresponding configurations can make reading input files really easy.

#### Running the Demo
Run the demo with the defined filter using the following command:
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.LinesFilter01 \
target/scala-2.11/spark-utils-demos-assembly.jar \
LinesFilter01.input.format="text" \
LinesFilter01.input.path="README.md" \
LinesFilter01.output.format="text" \
LinesFilter01.output.mode="overwrite" \
LinesFilter01.output.path="tmp/README-FILTERED.md" \
LinesFilter01.wordFilter="Spark"
```
This will produce a Spark file in the relative folder `tmp/README-FILTERED.md`, overwriting any pre-existing one.


### Writing Filtered Lines by a Sequence of Words

So far, we have seen how we can filter the lines by a single word. What if we want to pass in a list of words that
should be used as a filter?

The source code for this demo is available in
[LinesFilter02.scala](src/main/scala/org/tupol/sparkutils/demos/LinesFilter02.scala).

#### Defining the Application Context
First, we need to change our `wordFilter` parameter to something more suitable, let's call it `wordsFilter` and give it
the type `Seq[String]`.
```scala
case class LinesFilter02Context(input: FileSourceConfiguration, output: FileSinkConfiguration, wordsFilter: Set[String])
```
The configuration framework comes to the rescue one more time and the only thing we need to change in the application
context factory is one line:
```scala
  config.extract[Seq[String]]("wordsFilter")
```
As mentioned before, the configuration framework provides support for some of the popular Scala containers, including
`Option[T]` and `Seq[T]`.

#### Creating the Application
Obviously the logic in this case will be much more complex... no, it won't :)
```scala
  override def run(implicit spark: SparkSession, context: LinesFilter02Context): Unit = {
    import spark.implicits._
    val lines = spark.source(context.input).read.as[String]
    def lineFilter(line: String): Boolean = context.wordsFilter.foldLeft(false)((res, word) => res || line.contains(word))
    val filteredRecords = lines.filter(lineFilter(_)).toDF
    filteredRecords.sink(context.output).write
  }
```

#### Running the Demo
At this point we have reached the limit of passing parameters to the application as simple arguments.
Passing in a structure, like a list or a map, requires a different approach.
We are dealing with this by creating a separate application configuration file, `application.conf`, that supports Json
and Hocon format so we can express easily structured data types.
The name of the configuration file is mandatory to be `application.conf`.
In our case the [`application.conf`](src/main/resources/LinesFilter02/application.conf) looks as following:
```json
LinesFilter02.wordsFilter: ["Spark", "DataSource", "DataSink", "framework"]
```

Run the demo with the defined filter using the following command:
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.LinesFilter02 \
--files src/main/resources/LinesFilter02/application.conf \
target/scala-2.11/spark-utils-demos-assembly.jar \
LinesFilter02.input.format="text" \
LinesFilter02.input.path="README.md" \
LinesFilter02.output.format="text" \
LinesFilter02.output.mode="overwrite" \
LinesFilter02.output.path="tmp/README-FILTERED.md"
```
This will produce a Spark file in the relative folder `tmp/README-FILTERED.md`, overwriting any pre-existing one.


### MySparkApp More Tips and Tricks

Now we have almost all the ingredients provided by `spark-utils`, or at least a decent taste of it.
There are a few more interesting options for configuration of data source and data sink,
which we will cover with this demo.

Our simple app will take a structured file containing personal data and make a transformation of height from
centimeters to inches.

The source code for this demo is available in
[MySparkApp.scala](src/main/scala/org/tupol/sparkutils/demos/MySparkApp.scala).

#### Running the Demo Using Just Application Parameters
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.MySparkApp \
target/scala-2.11/spark-utils-demos-assembly.jar \
MySparkApp.input.format="csv" \
MySparkApp.input.header="true" \
MySparkApp.input.delimiter=";" \
MySparkApp.input.path="src/main/resources/data/people.csv" \
MySparkApp.output.format="json" \
MySparkApp.output.mode="overwrite" \
MySparkApp.output.path="tmp/MySpakApp.json"
```

#### Running the Demo Using Application Parameters and External Schema
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.MySparkApp \
target/scala-2.11/spark-utils-demos-assembly.jar \
MySparkApp.input.format="csv" \
MySparkApp.input.header="true" \
MySparkApp.input.delimiter=";" \
MySparkApp.input.schema.path="src/main/resources/MySparkApp03/people.schema" \
MySparkApp.input.path="src/main/resources/data/people-headerless.csv" \
MySparkApp.output.format="json" \
MySparkApp.output.mode="overwrite" \
MySparkApp.output.path="tmp/MySpakApp.json"
```

#### Running the Demo Using `application.conf`
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.MySparkApp \
--files src/main/resources/MySparkApp01/application.conf \
target/scala-2.11/spark-utils-demos-assembly.jar
```

#### Running the Demo Using `application.conf` with Internal Schema
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.MySparkApp \
--files src/main/resources/MySparkApp02/application.conf \
target/scala-2.11/spark-utils-demos-assembly.jar
```

#### Running the Demo Using `application.conf` with External Schema
```
spark-submit -v \
--master local --deploy-mode client \
--class org.tupol.sparkutils.demos.MySparkApp \
--files src/main/resources/MySparkApp03/application.conf \
target/scala-2.11/spark-utils-demos-assembly.jar
```


## References and Links

- [Apache Spark](https://spark.apache.org/)
- [Scala](https://www.scala-lang.org/)
- [Scalaz](https://github.com/scalaz/scalaz)
- [`spark-utils`](https://github.com/tupol/spark-utils)
    - [`SparkRunnable`](https://github.com/tupol/spark-utils/blob/master/docs/spark-runnable.md)
    - [`SparkApp`](https://github.com/tupol/spark-utils/blob/master/docs/spark-app.md)
    - [`DataSource`](https://github.com/tupol/spark-utils/blob/master/docs/data-source.md)
    - [`DataSink`](https://github.com/tupol/spark-utils/blob/master/docs/data-sink.md)
- [`spark-tools`](https://github.com/tupol/spark-tools) This is a project that makes use of `spark-utils` to provide some generic tools:
    - [`FormatConverter`](https://github.com/tupol/spark-tools/blob/master/docs/format-converter.md)
    - [`SimpleSQLProcessor`](https://github.com/tupol/spark-tools/blob/master/docs/simple-sql-processor.md)
- [`scala-utils`](https://github.com/tupol/scala-utils)
    This project holds the [Configuration Framework](https://github.com/tupol/scala-utils/blob/master/docs/configuration-framework.md) that we used and much more.
