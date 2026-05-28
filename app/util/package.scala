import os.Path

package object util {

  def getOutputAbsolutePath(outputString: String): Path = {
    if (outputString.head == '/') {
      // outputString is an absolute path -> safe to pass it to Path
      Path(outputString)
    } else {
      val sequence = outputString.split('/')
      val result = sequence.foldLeft(os.pwd)((acc, subName) => acc / subName)
      result
    }
  }

}
