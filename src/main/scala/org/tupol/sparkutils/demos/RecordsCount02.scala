package org.tupol.sparkutils.demos

import com.typesafe.config.Config
import org.apache.spark.sql.SparkSession
import org.tupol.spark.SparkApp
import org.tupol.spark.implicits._
import org.tupol.spark.io._
import org.tupol.utils.config.Configurator

/**
 * spark-submit  -v --master local --deploy-mode client \
 * --class org.tupol.sparkutils.demos.RecordsCount02 \
 * target/scala-2.11/spark-utils-demos-assembly.jar \
 * RecordsCount02.input.format="text" RecordsCount02.input.path="README.md"
 */
object RecordsCount02 extends SparkApp[RecordsCount02Context, Unit] {

  override def createContext(config: Config): RecordsCount02Context =
    RecordsCount02Context(config).get

  override def run(implicit spark: SparkSession, context: RecordsCount02Context): Unit =
    println(s"There are ${spark.source(context.input).read.count} records in the ${context.input.path} file.")
}

case class RecordsCount02Context(input: FileSourceConfiguration, wordFilter: String)
object RecordsCount02Context extends Configurator[RecordsCount02Context] {
  import com.typesafe.config.Config
  import scalaz.ValidationNel
  import org.tupol.utils.config._
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, RecordsCount02Context] = {
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only text files are supported").toNel)(_.format == FormatType.Text) |@|
      config.extract[String]("word-filter") apply
      RecordsCount02Context.apply
  }
}


