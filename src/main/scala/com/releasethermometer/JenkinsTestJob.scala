package com.releasethermometer

/** Abstract test job on Jenkins server. */
abstract class JenkinsTestJob

/** Empty test job is used for error case handling. */
case object EmptyTestJob extends JenkinsTestJob

