package call_graphs

import io.shiftleft.codepropertygraph.generated.nodes.{Call, Identifier}

object UnsafeUsagesAnalyser {

  private def checkForUnsafeCalls(inputProjectPath: String, joern: JoernAnalysis): Set[UnsafeCallDetected] = {
    val analysisResult = joern.analyzeApplication(inputProjectPath)
    analysisResult match {
      case Left(called) =>
        called.flatMap(tuple => {
          val (unsafeFunction: UnsafeFunction, unsafeCalls: Set[Call]) = tuple
          unsafeCalls.map((callNode: Call) => {
            UnsafeCallDetected(unsafeFunction.functionName, callNode.lineNumber, callNode.columnNumber)
          })
        })
      case _ => Set()
    }
  }

  private def checkForUnsafeIdentifiersUsed(inputProjectPath: String, joern: JoernAnalysis): Set[UnsafeIdentifierDetected] = {
    val unsafeIdentifiersSet = joern.analyzeIdentifierNodes(inputProjectPath)
    unsafeIdentifiersSet match {
      case Left(identifiers) =>
        identifiers.flatMap(tuple => {
          val (unsafeFunction: UnsafeFunction, unsafeIdentifiers: Set[Identifier]) = tuple
          unsafeIdentifiers.map((idNode: Identifier) => {
            UnsafeIdentifierDetected(unsafeFunction.functionName, idNode.lineNumber, idNode.columnNumber)
          })
        })
      case _ =>  Set()
    }
  }

  def analyseUnsafeUsages(inputProjectPath: String, joern: JoernAnalysis): UnsafeUsagesResult = {
    val unsafeCalls = checkForUnsafeCalls(inputProjectPath, joern)
    val unsafeIdentifiersUsed = checkForUnsafeIdentifiersUsed(inputProjectPath, joern)
    UnsafeUsagesResult(unsafeCalls, unsafeIdentifiersUsed)
  }

}
