package com.releasethermometer

/**
 * gcov compatible coverage test job.
 * @param success Success flag of the Jenkin's build (passed or failed).
 * @param build Jenkin's build number.
 * @param lines Line coverage in %.
 */
case class CoverageTestJob(success: Boolean, build: Int, lines: Double) extends JenkinsTestJob

object CoverageTestJob {
    /** Result pattern for coverage test: <coverage branch-rate="XX.XXX" line-rate="XX.XXX" timestamp="XXX" version="XXX">. */
    val resultPattern = """<coverage branch-rate="\d+\.\d+" line-rate="(\d+\.\d+)" timestamp="\d+" version=".+">""".r
}
