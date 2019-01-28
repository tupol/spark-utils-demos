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
 * --class org.tupol.sparkutils.demos.LinesCount03 \
 * target/scala-2.11/spark-utils-demos-assembly.jar \
 * LinesCount03.input.format="text" \
 * LinesCount03.input.path="README.md" \
 * LinesCount03.word-filter="Spark"
 */
object LinesCount03 extends SparkApp[LinesCount03Context, Unit] {

  override def createContext(config: Config): LinesCount03Context =
    LinesCount03Context(config).get

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
}

case class LinesCount03Context(input: FileSourceConfiguration, wordFilter: Option[String])
object LinesCount03Context extends Configurator[LinesCount03Context] {
  import com.typesafe.config.Config
  import scalaz.ValidationNel
  import org.tupol.utils.config._
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, LinesCount03Context] = {
    config.extract[FileSourceConfiguration]("input")
      .ensure(new IllegalArgumentException("Only text files are supported").toNel)(_.format == FormatType.Text) |@|
      config.extract[Option[String]]("word-filter") apply
      LinesCount03Context.apply
  }
}
