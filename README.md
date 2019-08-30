# Creating Simple Configurable Spark Applications in Scala


## Motivation

All of us can recall the first days of picking up a new technology and running the first “*Hello, World!*” or “*Count words*”
applications that get us started with a new language or a platform.

Up to a certain point of exploring and creating demos or prototypes everything is nice, but when it comes to creating
*production ready* configurable applications we all have a hard time and actually start thinking about the operational
use of our applications and how they will be deployed to a production system. This is the moment when a few lines of
beautiful code tend to get cluttered by a lot of configuration, wiring and setup.

When it comes to Apache Spark it gets even more complicated, setting up the Spark context and setting up the input
sources and outputs.
It would be really nice to have a simple framework that keeps our Spark code clean and uncluttered.


## Audience

Developers starting up into the [Apache Spark](https://spark.apache.org/) application development
in [Scala](https://www.scala-lang.org/).

Some basic Scala and Apache Spark knowledge is crucial to make sense of this presentation.

This article is not meant as a Scala or an Apache Spark tutorial.


## spark-utils

[`spark-utils`](https://github.com/tupol/spark-utils) is a simple framework, developed
across a few years of writing Spark applications that so far helped me starting up new projects and creating
applications fast and relatively easy.
The main ideas behind building a new Spark application are logic, configuration and execution.

[Full article is available as a Wiki page here.](https://github.com/tupol/spark-utils-demos/wiki)
