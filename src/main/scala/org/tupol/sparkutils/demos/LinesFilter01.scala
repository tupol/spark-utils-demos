package org.tupol.sparkutils.demos

import com.typesafe.config.Config
import org.apache.spark.sql.SparkSession
import org.tupol.spark.SparkApp
import org.tupol.spark.implicits._
import org.tupol.spark.io._
import org.tupol.utils.config.Configurator

/**
 * spark-submit  -v \
 * --master local --deploy-mode client \
 * --class org.tupol.sparkutils.demos.LinesFilter01 \
 * target/scala-2.11/spark-utils-demos-assembly.jar \
 * LinesFilter01.input.format="text" \
 * LinesFilter01.input.path="README.md" \
 * LinesFilter01.output.format="text" \
 * LinesFilter01.output.mode="overwrite" \
 * LinesFilter01.output.path="tmp/README-FILTERED.md" \
 * LinesFilter01.wordFilter="Spark"
 */
object LinesFilter01 extends SparkApp[LinesFilter01Context, Unit] {

  override def createContext(config: Config): LinesFilter01Context =
    LinesFilter01Context(config).get

  override def run(implicit spark: SparkSession, context: LinesFilter01Context): Unit = {
    import spark.implicits._
    val lines = spark.source(context.input).read.as[String]
    def lineFilter(line: String): Boolean = line.contains(context.wordFilter)
    val filteredRecords = lines.filter(lineFilter(_)).toDF
    filteredRecords.sink(context.output).write
  }
}

case class LinesFilter01Context(input: FileSourceConfiguration, output: FileSinkConfiguration, wordFilter: String)
object LinesFilter01Context extends Configurator[LinesFilter01Context] {
  import com.typesafe.config.Config
  import scalaz.ValidationNel
  import org.tupol.utils.config._
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, LinesFilter01Context] = {
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only input text files are supported").toNel)(_.format == FormatType.Text) |@|
    config.extract[FileSinkConfiguration]("output")
      .ensure(new IllegalArgumentException("Only output text files are supported").toNel)(_.format == FormatType.Text) |@|
    config.extract[String]("wordFilter") apply
    LinesFilter01Context.apply
  }
}


