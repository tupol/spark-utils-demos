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
 * --class org.tupol.sparkutils.demos.RecordsCount01 \
 * target/scala-2.11/spark-utils-demos-assembly.jar \
 * RecordsCount01.input.format="text" \
 * RecordsCount01.input.path="README.md"
 */
object RecordsCount01 extends SparkApp[RecordsCount01Context, Unit] {

  override def createContext(config: Config): RecordsCount01Context =
    RecordsCount01Context(config).get

  override def run(implicit spark: SparkSession, context: RecordsCount01Context): Unit =
    println(s"There are ${spark.source(context.input).read.count} records in the ${context.input.path} file.")
}

case class RecordsCount01Context(input: FileSourceConfiguration)
object RecordsCount01Context extends Configurator[RecordsCount01Context] {
  import com.typesafe.config.Config
  import org.tupol.utils.config._
  import scalaz.ValidationNel

  override def validationNel(config: Config): ValidationNel[Throwable, RecordsCount01Context] = {
    config.extract[FileSourceConfiguration]("input")
      .map(new RecordsCount01Context(_))
  }
}


