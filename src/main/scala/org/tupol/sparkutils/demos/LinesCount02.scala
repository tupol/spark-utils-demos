package org.tupol.sparkutils.demos

import com.typesafe.config.Config
import org.apache.spark.sql.SparkSession
import org.tupol.spark.SparkApp
import org.tupol.spark.implicits._
import org.tupol.spark.io._
import org.tupol.utils.config.Configurator

/**
 * spark-submit  -v --master local --deploy-mode client \
 * --class org.tupol.sparkutils.demos.LinesCount02 \
 * target/scala-2.11/spark-utils-demos-assembly.jar \
 * LinesCount02.input.format="text" LinesCount02.input.path="README.md" LinesCount02.word-filter="Spark"
 */
object LinesCount02 extends SparkApp[LinesCount02Context, Unit] {

  override def createContext(config: Config): LinesCount02Context =
    LinesCount02Context(config).get

  override def run(implicit spark: SparkSession, context: LinesCount02Context): Unit = {
    import spark.implicits._
    val lines = spark.source(context.input).read.as[String]
    val count = lines.filter(line => line.contains(context.wordFilter)).count
    println(s"There are $count lines in the ${context.input.path} file containing the word ${context.wordFilter}.")
  }
}

case class LinesCount02Context(input: FileSourceConfiguration, wordFilter: String)
object LinesCount02Context extends Configurator[LinesCount02Context] {
  import com.typesafe.config.Config
  import scalaz.ValidationNel
  import org.tupol.utils.config._
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, LinesCount02Context] = {
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only text files are supported").toNel)(_.format == FormatType.Text) |@|
    config.extract[String]("word-filter") apply
    LinesCount02Context.apply
  }
}


