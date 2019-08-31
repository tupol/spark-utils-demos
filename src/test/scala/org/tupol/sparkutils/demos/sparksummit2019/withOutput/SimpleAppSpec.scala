package org.tupol.sparkutils.demos.sparksummit2019.withOutput

import org.scalatest.{FunSuite, Matchers}
import org.tupol.spark.SharedSparkSession
import org.tupol.spark.io.FileSourceConfiguration
import org.tupol.spark.io.sources.TextSourceConfiguration


class SimpleAppSpec extends FunSuite with Matchers with SharedSparkSession  {

  import spark.implicits._

  val DummyInput = FileSourceConfiguration("no path", TextSourceConfiguration())
  val DummyContext = SimpleAppContext(input = DummyInput, filterA = "", filterB = "")

  test("appLogic should return 0 counts of a and b for an empty DataFrame") {
    val testData = spark.emptyDataset[String]
    val result = SimpleApp.appLogic(testData, DummyContext)
    result shouldBe (0, 0)
  }
  test("appLogic should return 0 counts of a and b for the given data") {
    val testData = spark.createDataset(Seq("c", "d", "c", "e"))
    val result = SimpleApp.appLogic(testData, DummyContext.copy(filterA = "a", filterB = "b"))
    result shouldBe (0, 0)
  }

  test("appLogic should return (1, 2) as count of a and b for the given data") {
    val testData = spark.createDataset(Seq("a", "b", "c", "b"))
    val result = SimpleApp.appLogic(testData, DummyContext.copy(filterA = "a", filterB = "b"))
    result shouldBe (1, 2)
  }

  test("run should return (1, 2) as count of a and b for the given data") {
    val input = FileSourceConfiguration("src/test/resources/input-test-01", TextSourceConfiguration())
    val context = SimpleAppContext(input = input, filterA = "a", filterB = "b")
    val result = SimpleApp.run(spark, context)
    result shouldBe (1, 2)
  }

}
