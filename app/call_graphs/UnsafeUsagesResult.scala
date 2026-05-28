package call_graphs

trait UnsafeUsage {
  def name: String
  def formatAsString: String
  protected def formatOptNumber(optNumber: Option[Integer]): String = {
    optNumber.map(_.toString).getOrElse("NaN")
  }
}
case class UnsafeCallDetected(functionName: String, lineNumber: Option[Integer], columnNumber: Option[Integer]) extends UnsafeUsage {
  def name: String = functionName
  def formatAsString: String = {
    val lineString: String = formatOptNumber(lineNumber)
    val columnString: String = formatOptNumber(columnNumber)
    f"Unsafe function $functionName is called at line $lineString:$columnString"
  }
}
case class UnsafeIdentifierDetected(identifierName: String, lineNumber: Option[Integer], columnNumber: Option[Integer]) extends UnsafeUsage {
  def name: String = identifierName
  def formatAsString: String = {
    val lineString: String = formatOptNumber(lineNumber)
    val columnString: String = formatOptNumber(columnNumber)
    f"Unsafe identifier $identifierName is used at line $lineString:$columnString"
  }
}

case class UnsafeUsagesResult(unsafeCalls: Set[UnsafeCallDetected], unsafeIdentifiersUsed: Set[UnsafeIdentifierDetected])
