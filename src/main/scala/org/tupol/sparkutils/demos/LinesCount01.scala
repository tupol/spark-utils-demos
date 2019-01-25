package org.tupol.sparkutils.demos

import com.typesafe.config.Config
import org.apache.spark.sql.SparkSession
import org.tupol.spark.SparkApp
import org.tupol.spark.implicits._
import org.tupol.spark.io._
import org.tupol.utils.config.Configurator

/**
 * spark-submit  -v --master local --deploy-mode client \
 * --class org.tupol.sparkutils.demos.LinesCount01 \
 * target/scala-2.11/spark-utils-demos-assembly.jar \
 * LinesCount01.input.format="text" LinesCount01.input.path="README.md"
 */
object LinesCount01 extends SparkApp[LinesCount01Context, Unit] {

  override def createContext(config: Config): LinesCount01Context =
    LinesCount01Context(config).get

  override def run(implicit spark: SparkSession, context: LinesCount01Context): Unit =
    println(s"There are ${spark.source(context.input).read.count} lines in the ${context.input.path} file.")
}

case class LinesCount01Context(input: FileSourceConfiguration)
object LinesCount01Context extends Configurator[LinesCount01Context] {
  import com.typesafe.config.Config
  import org.tupol.utils.config._
  import scalaz.ValidationNel

  override def validationNel(config: Config): ValidationNel[Throwable, LinesCount01Context] = {
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only text files are supported").toNel)(source => source.format == FormatType.Text)
      .map(input => new LinesCount01Context(input))
  }
}


