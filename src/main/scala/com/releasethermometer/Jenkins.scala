package com.releasethermometer

import scala.collection.JavaConversions._
import com.offbytwo.jenkins.model._
import com.offbytwo.jenkins.JenkinsServer

object Jenkins {
    /** Processes jenkins job based on the passed configuration. */
    def getLatestTest(config: JenkinsConfig): JenkinsTestJob = config match {
        case JenkinsConfig(JenkinsTestType.Cucumber, _, _, _, _, _, _, _) => getLatestCucumberTest(config)
        case JenkinsConfig(JenkinsTestType.QmlTest,  _, _, _, _, _, _, _) => getLatestQmlUnitTest(config)
        case JenkinsConfig(JenkinsTestType.Coverage, _, _, _, _, _, _, _) => getLatestCoverageTest(config)
        case _ => EmptyTestJob
    }

    /** Processes cucumber test job. */
    private def getLatestCucumberTest(config: JenkinsConfig): CucumberTestJob = {
        val jenkins = new JenkinsServer(config.uri, config.user, config.password)
        val job = jenkins.getJob(config.name)
        val build = job.getLastCompletedBuild
        val info = build.details
        val timestamp = info.getTimestamp
        val result = for {
            result <- CucumberTestJob.resultPattern.findAllMatchIn(info.getConsoleOutputText)
        } yield {
            val total = getOrZero(result.group(1))
            val failed = getOrZero(result.group(2))
            val pending = getOrZero(result.group(3))
            val passed = getOrZero(result.group(4))
            CucumberTestJob(
                timestamp, isSuccessful(info.getResult), build.getNumber, total, passed, failed, pending
            )
        }
        result.foldLeft(CucumberTestJob(
            timestamp, isSuccessful(info.getResult), build.getNumber, 0, 0, 0, 0)
        )(
            CucumberTestJob.combineWithRerun
        )
    }

    /** Processes QML (JUnit compatible) test job. */
    private def getLatestQmlUnitTest(config: JenkinsConfig): QmlTestJob = {
        val jenkins = new JenkinsServer(config.uri, config.user, config.password)
        val job = jenkins.getJob(config.name)
        val build = job.getLastCompletedBuild
        val info = build.details
        val buildNumber = build.getNumber
        val buildStatus = isSuccessful(info.getResult)
        val timestamp = info.getTimestamp
        val results = for (artifact <- info.getArtifacts) yield {
            val string = try {
                val file = info.downloadArtifact(artifact)
                val iterator = scala.io.Source.fromInputStream(file).getLines()
                iterator.find(_.startsWith("<testsuite"))
            }
            catch {
                case _: Throwable => None
            }
            string match {
                case Some(origin) => origin match {
                    case QmlTestJob.resultPattern(fail, total) =>
                        QmlTestJob(timestamp, buildStatus, buildNumber, getOrZero(total), getOrZero(fail))
                    case _ => QmlTestJob(timestamp, buildStatus, buildNumber, 0, 0)
                }
                case _ => QmlTestJob(timestamp, buildStatus, buildNumber, 0, 0)
            }
        }
        results.foldLeft(QmlTestJob(timestamp, buildStatus, buildNumber, 0, 0))(QmlTestJob.combine)
    }

    /** Processes test coverage job (GCov compatible). */
    private def getLatestCoverageTest(config: JenkinsConfig): CoverageTestJob = {
        config.file match {
            case Some(fileName) => {
                val jenkins = new JenkinsServer (config.uri, config.user, config.password)
                val job = jenkins.getJob (config.name)
                val build = job.getLastCompletedBuild
                val info = build.details
                val buildNumber = build.getNumber
                val buildStatus = isSuccessful (info.getResult)
                val timestamp = info.getTimestamp
                info.getArtifacts.find(_.getFileName == fileName) match {
                    case Some(artifact) => {
                        val string = try {
                            val file = info.downloadArtifact(artifact)
                            val iterator = scala.io.Source.fromInputStream(file).getLines()
                            iterator.find(_.startsWith("<coverage"))
                        }
                        catch {
                            case _: Throwable => None
                        }
                        string match {
                            case Some(origin) => origin match {
                                case CoverageTestJob.resultPattern(lineRate) =>
                                    CoverageTestJob(timestamp, buildStatus, buildNumber, getOrZeroDouble(lineRate) * 100.0)
                                case _ => CoverageTestJob(timestamp, buildStatus, buildNumber, 0.0)
                            }
                            case _ => CoverageTestJob(timestamp, buildStatus, buildNumber, 0.0)
                        }
                    }
                    case _ => CoverageTestJob(timestamp, buildStatus, buildNumber, 0.0)
                }
            }
            case _ => CoverageTestJob(0, false, 0, 0.0)
        }
    }

    private def getOrZero(value: String): Int = {
        if (null != value) value.toInt else 0
    }

    private def getOrZeroDouble(value: String): Double = {
        if (null != value) value.toDouble else 0.0
    }

    private def isSuccessful(result: BuildResult): Boolean = {
        BuildResult.SUCCESS == result
    }
}
