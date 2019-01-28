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
 * --class org.tupol.sparkutils.demos.LinesFilter02 \
 * --files src/main/resources/LinesFilter02/application.conf \
 * target/scala-2.11/spark-utils-demos-assembly.jar \
 * LinesFilter02.input.format="text" \
 * LinesFilter02.input.path="README.md" \
 * LinesFilter02.output.format="text" \
 * LinesFilter02.output.mode="overwrite" \
 * LinesFilter02.output.path="tmp/README-FILTERED.md"
 */
object LinesFilter02 extends SparkApp[LinesFilter02Context, Unit] {

  override def createContext(config: Config): LinesFilter02Context =
    LinesFilter02Context(config).get

  override def run(implicit spark: SparkSession, context: LinesFilter02Context): Unit = {
    import spark.implicits._
    val lines = spark.source(context.input).read.as[String]
    def lineFilter(line: String): Boolean = context.wordsFilter.foldLeft(false)((res, word) => res || line.contains(word))
    val filteredRecords = lines.filter(lineFilter(_)).toDF
    filteredRecords.sink(context.output).write
  }
}

case class LinesFilter02Context(input: FileSourceConfiguration, output: FileSinkConfiguration, wordsFilter: Seq[String])
object LinesFilter02Context extends Configurator[LinesFilter02Context] {
  import com.typesafe.config.Config
  import scalaz.ValidationNel
  import org.tupol.utils.config._
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, LinesFilter02Context] = {
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only input text files are supported").toNel)(_.format == FormatType.Text) |@|
    config.extract[FileSinkConfiguration]("output")
      .ensure(new IllegalArgumentException("Only output text files are supported").toNel)(_.format == FormatType.Text) |@|
    config.extract[Seq[String]]("wordsFilter") apply
    LinesFilter02Context.apply
  }
}


