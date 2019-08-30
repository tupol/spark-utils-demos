package org.tupol.sparkutils.demos.sparksummit2019.noConfigOutput

import com.typesafe.config.Config
import org.apache.spark.sql.SparkSession
import org.tupol.spark.SparkApp

object SimpleApp extends SparkApp[Unit, Unit]{
  override def createContext(config: Config): Unit = Unit
  override def run(implicit spark: SparkSession, context: Unit): Unit = {
    val logFile = "YOUR_SPARK_HOME/README.md"
    val logData = spark.read.textFile(logFile).cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    println(s"Lines with a: $numAs, Lines with b: $numBs")
  }
}
