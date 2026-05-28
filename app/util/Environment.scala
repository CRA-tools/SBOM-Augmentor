package util

object Environment {
  
  val SBOMAugmentorHomeVar: String = "AUGMENTOR_HOME"
  
  def getSBOMAugmentorHome: String = {
    try {
      System.getenv(SBOMAugmentorHomeVar)
    } catch {
      case _: NullPointerException =>
        val message = s"Environment variable $SBOMAugmentorHomeVar not set, aborting"
        Log.logError(message)
        throw ConfigurationException(message)
    }
  }

}
