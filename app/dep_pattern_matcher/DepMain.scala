package dep_pattern_matcher

import better.files.File
import call_graphs.*
import dep_pattern_matcher.DependencyPatternMatcher
import io.shiftleft.codepropertygraph.generated.nodes.{Call, Literal, StoredNode, Tag}

import scala.util.matching.Regex

case class InputProject(projectId: String, projectPath: String, basePath: String)
case class OutputRow(projectId: String, plugin: String, dependency: String, pattern: String)


object DepMain:
  private var failedProjects: List[(String, String)] = Nil

  def run_main(projectPath: String): Set[(String, ImportType)] = {
    processProject(projectPath)
  }

  def sanitiseLibrary(libraryName: String): String = {
    if (libraryName.contains("/")) {
      val arr = libraryName.split("/")
      assert(arr.nonEmpty)
      arr(0)
    } else {
      libraryName
    }
  }

  private def getLibraryName(call: Call): Option[ImportType] = {
    val asCode = call.code
    convertCodeToImportType(asCode)
  }

  def convertCodeToImportType(asCode: String): Option[ImportType] = {
    // Try to parse how joern shows the code string of the call node
    // Don't use a \s* around the first group, as that one might be empty
    val threeArgsPattern: Regex = """import\((.*),\s*(.*),\s*(.*)\)""".r
    val twoArgsPattern: Regex = """import\((.*),\s*(.*)\)""".r
    if (threeArgsPattern.matches(asCode) || twoArgsPattern.matches(asCode)) {
      val importType = asCode match {
        case threeArgsPattern(a, b, c) =>
          if (a.strip() == "") {
            // import foo as f => import(, foo, f)
            val moduleName = b.strip()
            val alias = c.strip()
            ImportedAliasedLibrary(moduleName, alias)
          } else {
            // from foo import f as g => import(foo, f, g)
            val moduleName = a.strip()
            val functionName = b.strip()
            val alias = c.strip()
            SelectedAliasedImport(moduleName, functionName, alias)
          }
        case twoArgsPattern(a, b) =>
          if (a.strip() == "") {
            // import foo => import(, foo)
            val moduleName = b.strip()
            ImportedFullName(moduleName)
          } else if (b.strip() == "*") {
            // from foo import * => import(foo, *)
            val moduleName = a.strip()
            WildCardImports(moduleName)
          } else {
            // from foo import f => import(foo, f)
            val moduleName = a.strip()
            val functionName = b.strip()
            SelectedImport(moduleName, functionName)
          }
        case _ =>
          throw JoernProcessingException(s"Could not resolve import '$asCode'")
      }
      Some(importType)
    } else {
      None
    }
  }

  def processLibraries(imports: Set[Call]): Set[(String, ImportType)] = {
    var s: Set[String] = Set()
    val asSet = imports.map(getLibraryName).filter(_.isDefined).map(_.get).toSet
    val asTuples: Set[(String, ImportType)] = asSet.map(importType => (importType.moduleName, importType))
    val sanitised = asTuples.map(tuple => (sanitiseLibrary(tuple._1), tuple._2))
    sanitised
  }

  protected def processProject(projectPath: String): Set[(String, ImportType)] = {
    val imports = new DependencyPatternMatcher().run(projectPath)
    val libraries = processLibraries(imports)
    libraries
  }

  private def writeOutputHeader(outputFile: File): Unit =
    val header = List("projectId", "plugin", "dependency", "pattern").mkString(",")
    outputFile.writeText(header)

  private def appendOutput(outputFile: File, outputRows: List[OutputRow]): Unit =
    val content = outputRows.map(row => row.productIterator.mkString(","))
    outputFile.appendLines(content*)
