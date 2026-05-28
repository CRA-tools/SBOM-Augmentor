package call_graphs

import scala.util.{Failure, Success, Try}
import io.joern.pysrc2cpg.{Py2CpgOnFileSystem, Py2CpgOnFileSystemConfig}
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{Call, CallDb, Identifier}
import io.shiftleft.semanticcpg.language.*

case class UnsafeFunction(functionName: String, moduleName: String)

class JoernAnalysis(val toCheck: Set[UnsafeFunction], val imports: Set[ImportType]) {

  // Check whether the seemingly unsafe function is actually a function from the vulnerable library
  protected def callMatchesImport(node: Call, importType: ImportType): Boolean = importType match {
    case ImportedFullName(moduleName) => node.code.startsWith(s"$moduleName.")
    case ImportedAliasedLibrary(moduleName, aliasName) => node.code.startsWith(s"$aliasName.")
    case SelectedImport(moduleName, selectedFunction) => node.code.startsWith(selectedFunction)
    case WildCardImports(moduleName) => true
    case SelectedAliasedImport(_, _, _) => false // Case should not be triggered, should have already been caught in checkAliasedImports
  }

  protected def identifierMatchesImport(node: Identifier, importType: ImportType): Boolean = importType match {
    case ImportedFullName(moduleName) => node.name.startsWith(s"$moduleName.")
    case ImportedAliasedLibrary(moduleName, aliasName) => node.name.startsWith(s"$aliasName.")
    case SelectedImport(moduleName, selectedFunction) => node.name == selectedFunction
    case WildCardImports(moduleName) => true
    case SelectedAliasedImport(_, _, _) => false // Case should not be triggered, should have already been caught in checkAliasedImports
  }

  protected def checkCalls(cpg: Cpg, unsafeFunction: UnsafeFunction, imports: Set[ImportType]): Set[Call] = {
    val called: Set[Call] = cpg.call(unsafeFunction.functionName).toSet
    called.filter((callNode: Call) => {
      imports.exists((importType: ImportType) => {
        val isCallFromModule = callMatchesImport(callNode, importType)
        isCallFromModule
      })
    })
  }

  protected def checkIdentifiers(cpg: Cpg, unsafeFunction: UnsafeFunction, imports: Set[ImportType]): Set[Identifier] = {
    val called: Set[Identifier] = cpg.identifier(unsafeFunction.functionName).toSet
    called.filter((identifierNode: Identifier) => {
      imports.exists((importType: ImportType) => {
        val isCallFromModule = identifierMatchesImport(identifierNode, importType)
        isCallFromModule
      })
    })
  }

  protected def checkAliasedImports(cpg: Cpg, unsafeFunction: UnsafeFunction, imports: Set[ImportType]): Set[Call] = {
    imports.flatMap({
      case SelectedAliasedImport(_, selectedFunction, alias) if selectedFunction == unsafeFunction.functionName =>
        // Currently, just returns a boolean indicator of whether
        cpg.call(alias).toSet
      case _ => Set()
    })
  }

  protected def filterSuspiciousImports(imports: Set[ImportType], unsafeFunction: UnsafeFunction): Set[ImportType] = {
    imports.filter(_.moduleName == unsafeFunction.moduleName)
  }

  protected def checkImportTypes(cpg: Cpg, toCheck: UnsafeFunction): Set[Call] = {
    val suspiciousImports = filterSuspiciousImports(imports, toCheck)
    val aliasedImportsCheck = checkAliasedImports(cpg, toCheck, suspiciousImports)
    if (aliasedImportsCheck.nonEmpty) {
      println(s"Found a call site of an alias of the unsafe function ${toCheck.functionName}")
      aliasedImportsCheck
    } else {
      checkCalls(cpg, toCheck, suspiciousImports)
    }
  }

  def analyzeApplication(projectPath: String): Either[Set[(UnsafeFunction, Set[Call])], String] = {
    val config = Py2CpgOnFileSystemConfig().withInputPath(projectPath)
    val frontend = new Py2CpgOnFileSystem()
    val tryCpg = frontend.createCpg(config)
    tryCpg match {
      case Failure(exception) =>
        Right(s"Could not create a code property graph for project at '$projectPath'")
      case Success(cpg) =>
        Left(toCheck.map((unsafeFunction: UnsafeFunction) => {
          (unsafeFunction, checkImportTypes(cpg, unsafeFunction))
        }))
    }
  }



  protected def checkIdentifierUsages(cpg: Cpg, toCheck: UnsafeFunction): Set[Identifier] = {
    val suspiciousImports = filterSuspiciousImports(imports, toCheck)
    checkIdentifiers(cpg, toCheck, suspiciousImports)
  }

  def analyzeIdentifierNodes(projectPath: String): Either[Set[(UnsafeFunction, Set[Identifier])], String] = {
    val config = Py2CpgOnFileSystemConfig().withInputPath(projectPath)
    val frontend = new Py2CpgOnFileSystem()
    val tryCpg = frontend.createCpg(config)
    tryCpg match {
      case Failure(exception) =>
        Right(s"Could not create a code property graph for project at '$projectPath'")
      case Success(cpg) =>
        Left(toCheck.map((unsafeFunction: UnsafeFunction) => {
          (unsafeFunction, checkIdentifierUsages(cpg, unsafeFunction))
        }))
    }
  }
}