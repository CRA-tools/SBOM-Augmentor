package sbom.generators

import util.HasExitCode

package object python {
  case class CycloneDxInvocationException(cycloneDXErrorMessage: String)
    extends Exception(s"An error occurred while invoking CycloneDX: $cycloneDXErrorMessage")
      with HasExitCode {
    override def exitCode: Int = 2
  }

}
