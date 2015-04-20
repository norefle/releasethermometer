package com.releasethermometer

/**
 * JUnit compatible QML test job.
 * @param success Success flag of the Jenkin's build (passed or failed).
 * @param build Jenkin's build number.
 * @param total Total amount of executed scenarios.
 * @param failed Amount of failed scenarios.
 */
case class QmlTestJob(success: Boolean, build: Int, total: Int, failed: Int) extends JenkinsTestJob


object QmlTestJob {
    /** Result pattern for qml test execution: <testsuite errors="XX" failures="XX" tests="XXX" name="XXX">. */
    val resultPattern = """<testsuite errors="\d+" failures="(\d+)" tests="(\d+)" name=".+">""".r

    def combine(left: QmlTestJob, right: QmlTestJob): QmlTestJob = QmlTestJob(
        left.success && right.success, left.build, left.total + right.total, left.failed + right.failed
    )
}
