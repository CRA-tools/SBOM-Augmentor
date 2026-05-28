package app

import call_graphs.UnsafeFunction
import scopt.OptionParser

object CommandLineArgsParser {

  implicit val unsafeFunctionsRead: scopt.Read[UnsafeFunction] = {
    scopt.Read.reads((str: String) => {
      val splitBySpace = str.split(":")
      assert(splitBySpace.length == 2)
      UnsafeFunction(functionName = splitBySpace(1), moduleName = splitBySpace.head)
    })
  }

  private val parser: OptionParser[StartupConfiguration] = new OptionParser[StartupConfiguration](Start.name) {
    head(Start.name)
    help("help")
      .text("Prints the help message")
    arg[String]("<project>")
      .text("Path to the project to be analysed")
      .action({ (x, c) =>
        c.copy(inputProjectPath = x)
      })
    opt[String]("manifest")
      .text("Path to the manifest file of the project to be analysed")
      .action({ (x, c) =>
        c.copy(inputManifestPath = Some(x))
      })
    opt[String]("sbom")
      .action({ (x, c) =>
        c.copy(inputSbomPath = Some(x))
      })
    opt[String]('o', "output")
      .action({ (x, c) =>
        c.copy(output = Some(x))
      })
      .text(s"Output path to write SBOM to [default: \"${StartupConfiguration.defaultOutput}\"]")
    opt[String]("python_version")
      .action({ (x, c) =>
        c.copy(pythonVersion = x)
      })
      .text(s"The Python version used to run the application [default: \"${StartupConfiguration.defaultPythonVersion}\"]")
    opt[Unit]("include_standard_library")
      .action({ (_, c) =>
        c.copy(includeStandardLibraries = true)
      })
      .text(s"Include imported Python standard libraries in the SBOM document (for example, when also shipping the Python runtime)")
    opt[Seq[UnsafeFunction]]("unsafe")
      .valueName("<module_name1>:<function_name1>,<module_name2>:<function_name2>...")
      .action((x, c) => c.copy(unsafeFunctionsToCheck = x))
      .text("Unsafe functions that should be checked")
  }

  def usage: String = parser.usage

  def parseArgs(args: Array[String]): Option[StartupConfiguration] = {
    parser.parse(args, StartupConfiguration(""))
  }

}
