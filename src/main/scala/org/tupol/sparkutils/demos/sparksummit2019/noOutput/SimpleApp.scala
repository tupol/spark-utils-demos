package org.tupol.sparkutils.demos.sparksummit2019.noOutput

import com.typesafe.config.Config
import org.apache.spark.sql.{Dataset, SparkSession}
import org.tupol.spark.SparkApp
import org.tupol.spark.implicits._
import org.tupol.spark.io._
import org.tupol.utils.config._

object SimpleApp extends SparkApp[SimpleAppContext, Unit] {
  override def createContext(config: Config) = SimpleAppContext(config).get
  override def run(implicit spark: SparkSession, context: SimpleAppContext): Unit = {
    import spark.implicits._
    val logData = spark.source(context.input).read.as[String]
    val (numAs, numBs) = appLogic(logData, context)
    println(s"Lines with a: $numAs, Lines with b: $numBs")
  }
  def appLogic(data: Dataset[String], context: SimpleAppContext): (Long, Long) = {
    val numAs = data.filter(line => line.contains(context.filterA)).count()
    val numBs = data.filter(line => line.contains(context.filterB)).count()
    (numAs, numBs)
  }
}

case class SimpleAppContext(input: FileSourceConfiguration, filterA: String, filterB: String)

object SimpleAppContext extends Configurator[SimpleAppContext] {
  import scalaz.ValidationNel
  import scalaz.syntax.applicative._
  override def validationNel(config: com.typesafe.config.Config):
  ValidationNel[Throwable, SimpleAppContext] = {
    config.extract[FileSourceConfiguration]("input").ensure(new IllegalArgumentException(
      "Only 'text' format files are supported").toNel)(_.format == FormatType.Text) |@|
      config.extract[String]("filterA") |@|
      config.extract[String]("filterB") apply
      SimpleAppContext.apply
  }
}
