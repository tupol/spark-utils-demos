package org.tupol.sparkutils.demos

import com.typesafe.config.Config
import org.apache.spark.sql.SparkSession
import org.tupol.spark.SparkApp
import org.tupol.spark.implicits._
import org.tupol.spark.io._
import org.tupol.utils.config.Configurator


object MySparkApp extends SparkApp[MySparkAppContext, Unit] {
  override def createContext(config: Config): MySparkAppContext =
    MySparkAppContext(config).get

  override def run(implicit spark: SparkSession, context: MySparkAppContext): Unit = {
    import spark.implicits._
    val people = spark.source(context.input).read
    val peopleWithHeightInch = people.withColumn("HeightInch", $"height" * 0.393701 )
    peopleWithHeightInch.sink(context.output).write
  }
}

case class MySparkAppContext(input: FileSourceConfiguration, output: FileSinkConfiguration)
object MySparkAppContext extends Configurator[MySparkAppContext] {
  import com.typesafe.config.Config
  import org.tupol.utils.config._
  import scalaz.ValidationNel
  import scalaz.syntax.applicative._

  override def validationNel(config: Config): ValidationNel[Throwable, MySparkAppContext] = {
    config.extract[FileSourceConfiguration]("input") |@|
      config.extract[FileSinkConfiguration]("output") apply
      MySparkAppContext.apply
  }
}
