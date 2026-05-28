package app

import call_graphs.UnsafeFunction

case class StartupConfiguration(inputProjectPath: String,
                                inputManifestPath: Option[String] = None,
                                inputSbomPath: Option[String] = None,
                                output: Option[String] = None,
                                pythonVersion: String = StartupConfiguration.defaultPythonVersion,
                                includeStandardLibraries: Boolean = StartupConfiguration.defaultIncludeStandardLibraries,
                                unsafeFunctionsToCheck: Seq[UnsafeFunction] = Nil)

object StartupConfiguration {
  val defaultOutput: String = "sbom_out"
  val defaultPythonVersion: String = "3.13.6"
  val defaultIncludeStandardLibraries: Boolean = false
}
