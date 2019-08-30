package org.tupol.sparkutils.demos.sparksummit2019.withOutput

import org.scalatest.{FunSuite, Matchers}
import org.tupol.spark.SharedSparkSession
import org.tupol.spark.io.FileSourceConfiguration
import org.tupol.spark.io.sources.TextSourceConfiguration


class SimpleAppSpec extends FunSuite with Matchers with SharedSparkSession  {

  import spark.implicits._

  val DummyContext = SimpleAppContext(
    input = FileSourceConfiguration("no path", TextSourceConfiguration()),
    filterA = "", filterB = "")

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

}
