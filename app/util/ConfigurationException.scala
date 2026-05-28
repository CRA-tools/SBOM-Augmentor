package util

case class ConfigurationException(message: String) extends Exception(message) with HasExitCode {
  override def exitCode: Int = 1
}
