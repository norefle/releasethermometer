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
            case (group, rate, CucumberTestJob(timestamp, success, _, total, passed, failed, pending)) => (group, (timestamp, rate, total - pending, passed))
            case (group, rate, QmlTestJob(timestamp, success, _, total, failed)) => (group, (timestamp, rate, total, total - failed))
            case (group, rate, CoverageTestJob(timestamp, success, _, lines)) => (group, (timestamp, rate, 100, lines.toInt))
            case _ => ("unknown", (0l, 0, 1, 0))
        }.toList

        println("")
        println(adjust("date", 30) + adjust("name", 30) + adjust("expected %", 15) + adjust("actual % (passed / total)", 35))
        println(adjust("", 110, "="))

        val grouped = output.groupBy(_._1).mapValues(_.map(_._2))
        grouped.foreach(entry => {
            val (key, value) = entry
            val (timestamp, rate, total, passed) = value.foldLeft((0l, 0, 0, 0))((left, right) =>
                (if (left._1 > 0l) left._1 else right._1, if (left._2 > 0) left._2 else right._2, left._3 + right._3, left._4 + right._4)
            )

            val realRate = (passed.toDouble / Math.max(total.toDouble, 1.0)) * 100.0

            val color = if (rate <= realRate) Console.GREEN else Console.RED
            println(color +
                adjust(java.time.Instant.ofEpochMilli(timestamp).toString, 30) +
                adjust(key, 30) + adjust(f"$rate%%", 15) +
                adjust(f"$realRate%.02f%% ($passed / $total)", 35) +
                Console.RESET
            )
        })
    }

    private def adjust(value: String, expectedSize: Int, stub: String = " "): String = {
        val difference = expectedSize - value.length
        if (0 < difference) value + (stub * difference)
        else value.take(expectedSize)
    }
}
