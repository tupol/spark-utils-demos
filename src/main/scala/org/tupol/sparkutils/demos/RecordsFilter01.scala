package org.tupol.sparkutils.demos

import com.typesafe.config.Config
import org.apache.spark.sql.SparkSession
import org.tupol.spark.SparkApp
import org.tupol.spark.implicits._
import org.tupol.spark.io._
import org.tupol.utils.config.Configurator

/**
 * spark-submit  -v --master local --deploy-mode client \
 * --class org.tupol.sparkutils.demos.RecordsFilter \
 * target/scala-2.11/spark-utils-demos-assembly.jar \
 * RecordsFilter.input.format="text" RecordsFilter.input.path="README.md" \
 * RecordsFilter.output.format="text" RecordsFilter.output.path="tmp/README-FILTERED.md" \
 * RecordsFilter.word-filter="Spark"
 */
object RecordsFilter extends SparkApp[RecordsFilterContext, Unit] {

  override def createContext(config: Config): RecordsFilterContext =
    RecordsFilterContext(config).get

  override def run(implicit spark: SparkSession, context: RecordsFilterContext): Unit = {
    import spark.implicits._
    val lines = spark.source(context.input).read.as[String]
    val filteredRecords = lines.filter(line => line.contains(context.wordFilter)).toDF
    filteredRecords.sink(context.output).write
  }
}

case class RecordsFilterContext(input: FileSourceConfiguration, output: FileSinkConfiguration, wordFilter: String)
object RecordsFilterContext extends Configurator[RecordsFilterContext] {
  import com.typesafe.config.Config
  import scalaz.ValidationNel
  import org.tupol.utils.config._
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, RecordsFilterContext] = {
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only input text files are supported").toNel)(_.format == FormatType.Text) |@|
    config.extract[FileSinkConfiguration]("output")
      .ensure(new IllegalArgumentException("Only output text files are supported").toNel)(_.format == FormatType.Text) |@|
    config.extract[String]("word-filter") apply
    RecordsFilterContext.apply
  }
}


