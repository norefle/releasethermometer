package com.releasethermometer

/**
 * Cucumber test job.
 * @param timestamp Amount of milliseconds since the epoch.
 * @param success Success flag of the Jenkin's build (passed or failed).
 * @param build Jenkin's build number.
 * @param total Total amount of executed scenarios.
 * @param passed Amount of passed scenarios.
 * @param failed Amount of failed scenarios.
 * @param pending Amount of pending scenarios.
 */
case class CucumberTestJob(timestamp: Long, success: Boolean, build: Int, total: Int, passed: Int, failed: Int, pending: Int) extends JenkinsTestJob

object CucumberTestJob {
    /** Result pattern for cucumber test execution: XX scenarios (XX failed, XX pending, XX passed). */
    val resultPattern = """(\d+)\s+scenario(?:s)?\s+\((?:(\d+)\s+failed,\s+)?(?:(\d+)\s+(?:pending|skipped),\s+)?(?:(\d+)\s+passed\)?)""".r

    /**
     * Combines test result for the original cucumber job and its rerun.
     * @param original Original result of the cucumber job.
     * @param rerun Results of the rerun failed and passed tests from original.
     * @return Combination of the original results and its reruns, where
     *         rerun is supposed to be execution of the failed and passed scenarios only.
     *         For instance:
     *         first run returns: 14 scenarios (2 failed, 1 pending, 11 passed)
     *         first rerun (all failed and passed tests): 3 scenarios (1 failed, 1 pending, 1 passed)
     *         Result is: 14 scenarios (1 failed, 1 pending, 12 passed)
     */
    def combineWithRerun(original: CucumberTestJob, rerun: CucumberTestJob): CucumberTestJob = {
        CucumberTestJob(
            original.timestamp,
            original.success,
            original.build,
            math.max(original.total, rerun.total),
            original.passed + rerun.passed,
            rerun.failed,
            rerun.pending
        )
    }
}
