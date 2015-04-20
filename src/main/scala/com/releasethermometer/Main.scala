package com.releasethermometer

import scala.collection.JavaConversions._
import java.net.URI
import com.offbytwo.jenkins._
import com.offbytwo.jenkins.model._

object Thermometer {
    val version = "0.0.1"

    def main(args: Array[String]) {
        println("Release Thermometer v" + version)
        if (args.isEmpty || args.contains("-h")) {
            showHelp()
        }
        else {
            process(args.last, args.contains("-v"))
        }
    }

    def showHelp() = println(
        """
          |Usage:
          |java -jar thermometer [OPTION] CONFIG
          |
          |Options:
          |-v   Prints verbose output.
          |-h   Shows this help and exit.
          |
          |Config:
          |Full or relative path to JSON file with configuration with Jenkins's access info and job descriptions.
        """.stripMargin)

    def process(config: String, verbose: Boolean): Unit = {
        val tasks = JenkinsConfig.parseJson(scala.io.Source.fromFile(config).mkString)
        val results = for (task <- tasks) yield (task.group, task.passRate, Jenkins.getLatestTest(task))
        results.foreach(entry => {
            if (verbose) println(s"${entry._1} -> ${entry._3}")
            else print(".")
        })
        println("")

        val output = results.map{
            case (group, rate, CucumberTestJob(success, _, total, passed, failed, pending)) => (group, (rate, total - pending, passed))
            case (group, rate, QmlTestJob(success, _, total, failed)) => (group, (rate, total, total - failed))
            case (group, rate, CoverageTestJob(success, _, lines)) => (group, (rate, 100, lines.toInt))
            case _ => ("unknown", (0, 1, 0))
        }.toList

        println("")
        println(adjust("name", 30) + adjust("expected %", 15) + adjust("actual % (passed / total)", 35))
        println(adjust("", 80, "="))

        val grouped = output.groupBy(_._1).mapValues(_.map(_._2))
        grouped.foreach(entry => {
            val (key, value) = entry
            val (rate, total, passed) = value.foldLeft((0, 0, 0))((left, right) =>
                (if (left._1 > 0) left._1 else right._1, left._2 + right._2, left._3 + right._3)
            )

            val realRate = (passed.toDouble / Math.max(total.toDouble, 1.0)) * 100.0
            println(adjust(key, 30) + adjust(f"$rate%%", 15) + adjust(f"$realRate%.02f%% ($passed / $total)", 35))
        })
    }

    private def adjust(value: String, expectedSize: Int, stub: String = " "): String = {
        val difference = expectedSize - value.length
        if (0 < difference) value + (stub * difference)
        else value.take(expectedSize)
    }
}
