package app

import os.Path
import util.Environment

import java.nio.file.Path as JPath

object PathLocations {

  private def getAugmentorHomeOrPwd: Path = {
    try {
      val home: String = Environment.getSBOMAugmentorHome
      os.Path.apply(home)
    } catch {
      case t: Throwable => os.pwd
    }
  }
  
  val pythonStdLibs: Path = getAugmentorHomeOrPwd / "data" / "python-stdlib.json"
  val pythonWheelInspector: Path = getAugmentorHomeOrPwd / "scripts" / "inspect_wheel.py"

}
