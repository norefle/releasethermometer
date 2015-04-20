package com.releasethermometer

import java.net.URI

import org.json4s.JsonAST.{JInt, JArray, JString, JValue}
import org.json4s.jackson.JsonMethods._

/** Supported types of the jenkins job. */
object JenkinsTestType extends Enumeration {
    type Type = Value
    val Cucumber, QmlTest, Coverage, Unknown = Value
}

/**
 * Configuration of one Jenkins job.
 * @param testType Type of the Jenkins' job.
 * @param name The name of the job.
 * @param group The name of the group of the jobs. Jobs within the same group will be combined as a single job.
 * @param passRate Expected pass rate for the job.
 * @param file Optional artifact name to parse the output.
 * @param uri URI to the Jenkins root.
 * @param user User name to access Jenkins' jobs.
 * @param password Password to access Jenkin's jobs.
 */
case class JenkinsConfig(
    testType: JenkinsTestType.Type,
    name: String,
    group: String,
    passRate: Int,
    file: Option[String],
    uri: URI,
    user: String,
    password: String
)

object JenkinsConfig {
    val supportedTests = List("cucumber", "qmltest", "coverage")

    /**
     * Parses a configuration file in JSON format to jenkins config.
     * @param data Content of the JSON config file.
     */
    def parseJson(data: String): Stream[JenkinsConfig] = {
        val json = parse(data)
        val username = asString(json \ "username")
        val password = asString(json \ "password")
        val tasks: List[JenkinsConfig] = for {
            testType <- supportedTests
            set <- asList(json \ testType)
            url = asString(set \ "url")
            job <- asList(set \ "jobs")
        } yield {
            val name = asString(job \ "name")
            val group = asString(job \ "group")
            val passRate = asInt(job \ "passRate")
            val file = asOptionString(job \ "file")
            JenkinsConfig(testTypeByName(testType), name, group, passRate, file, new URI(url), username, password)
        }

        tasks.toStream
    }

    private def asString(source: JValue): String = source match {
        case JString(value) => value
        case _ => ""
    }

    private def asOptionString(source: JValue): Option[String] = source match {
        case JString(value) => Some(value)
        case _ => None
    }

    private def asList(source: JValue) = source match {
        case JArray(list) => list
        case _ => List()
    }

    private def asInt(source: JValue): Int = source match {
        case JInt(value) => value.toInt
        case _ => 0
    }

    private def testTypeByName(name: String): JenkinsTestType.Type = name match {
        case "cucumber" => JenkinsTestType.Cucumber
        case "qmltest" => JenkinsTestType.QmlTest
        case "coverage" => JenkinsTestType.Coverage
        case _ => JenkinsTestType.Unknown
    }
}
