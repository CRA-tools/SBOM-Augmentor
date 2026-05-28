package dep_pattern_matcher

import util.HasExitCode

case class JoernProcessingException(message: String) extends Exception(message) with HasExitCode {
  override def exitCode: Int = 2
}
