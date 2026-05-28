package util

object Log {
  def logError(message: String): Unit = {
    System.err.println(message)
  }

  def logMessage(message: String): Unit = {
    println(message)
  }
}
