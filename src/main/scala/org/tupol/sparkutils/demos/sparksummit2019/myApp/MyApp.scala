package org.tupol.sparkutils.demos.sparksummit2019.myApp

import com.typesafe.config.Config
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.tupol.spark.SparkApp
import org.tupol.spark.io.{FormatAwareDataSinkConfiguration, FormatAwareDataSourceConfiguration}
import org.tupol.spark.implicits._
import org.tupol.utils.config._

object MyApp extends SparkApp[MyAppContext, DataFrame] {

  override def createContext(config: Config): MyAppContext = MyAppContext(config).get

  override def run(implicit spark: SparkSession, context: MyAppContext): DataFrame = {
    val inputData = spark.source(context.input).read
    val outputData = transform(inputData)
    outputData.sink(context.output).write
  }
  def transform(data: DataFrame)(implicit spark: SparkSession, context: MyAppContext) = {
    data  // Transformation logic here
  }

}

case class MyAppContext(input: FormatAwareDataSourceConfiguration,
                        output: FormatAwareDataSinkConfiguration)

object MyAppContext extends Configurator[MyAppContext] {
  import scalaz.ValidationNel
  import scalaz.syntax.applicative._
  def validationNel(config: Config): ValidationNel[Throwable, MyAppContext] = {
    config.extract[FormatAwareDataSourceConfiguration]("input") |@|
      config.extract[FormatAwareDataSinkConfiguration]("output") apply
      MyAppContext.apply
  }
}
