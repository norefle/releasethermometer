# Release thermometer

Simple and stupid script to monitor the current (latest) build jobs
on Jenkins server and compare it to expected results.

Currently it supports the following test outputs:

* Cucumber tests.
* QML tests (JUnit compatible).
* GCov (JUnit compatible).

## Usage

Create configuration file based on config.json.sample file and run:

    thermometer [-v|-h] path/to/config.json
