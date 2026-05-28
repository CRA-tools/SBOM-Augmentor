package sbom.generators.python

import java.io.{File, FileWriter, PrintWriter}
import os.Path
import sbom.SBOMGenerator

import scala.sys.process.*
import util.Log

class CycloneDX(pathToManifestFile: Path) extends SBOMGenerator {

  protected def manifestToCycloneArguments(pathToManifest: String, manifestType: PythonManifestType): List[String] =
    manifestType match {
    case PythonPipenv => List("pipenv", pathToManifest)
    case PythonPoetry => List("poetry", pathToManifest)
    case PythonRequirements => List("requirements", pathToManifest)
    case PythonVirtualEnv => List("environment", pathToManifest)
  }

  protected def recogniseManifestType: Either[PythonManifestType, String] = {
    val file: File = pathToManifestFile.toIO
    val lowerCase = file.getName.toLowerCase
    if (lowerCase.startsWith("requirements.txt")) {
      Left(PythonRequirements)
    } else if (lowerCase.startsWith("poetry.lock")) {
      Left(PythonPoetry)
    } else if (lowerCase.startsWith("pipfile")) {
      Left(PythonPipenv)
    } else if (lowerCase.startsWith("pyproject.toml")) {
      Left(PythonVirtualEnv)
    } else {
      Right(file.getName)
    }
  }

  protected def outputPath(): Path = {
    os.pwd / "cyclonedx_sbom.json"
  }

  protected def generateCommand(): Seq[String] = {
    recogniseManifestType match {
      case Left(manifestType) =>
        val manifestArgs = manifestToCycloneArguments(pathToManifestFile.toString, manifestType)
        Seq("python3", "-m", "cyclonedx_py") ++ manifestArgs
      case Right(manifestFileName) =>
        val message = s"Could not recognise Python manifest file '$manifestFileName'"
        Log.logError(message)
        throw CycloneDxInvocationException(message)
    }
  }

  protected def startCycloneDX(): Either[Path, String] = {
    try {
      val command = generateCommand()
      val o = outputPath()
      val process = Process(command)
      val pb: ProcessBuilder = process
      try {
        (command #> new java.io.File(o.toString)).!!
        Left(o)
      } catch {
        case e: Exception =>
          val message = s"An error occurred while invoking CycloneDX: ${e.getMessage}.\nSee output for more details."
          Right(message)
      }
    } catch {
      case e: java.lang.RuntimeException =>
        val errorMessage = "ERROR: cyclonedx_py failed. Aborting"
        Log.logError(errorMessage)
        Right(errorMessage)
    }
  }

  /**
   * Checks whether all necessary requirements for running the augmentor have been satisfied.
   * Either returns None if all requirements are met, or Some, with a description of
   * the unsatisfied requirement, if not.
   */
  protected def checkRequirements(): Option[String] = {
    // TODO Should also check that python3 is installed with the cyclonedx_py package
    if (!os.isFile(pathToManifestFile)) {
      Some(s"Manifest file could not be found at '$pathToManifestFile'")
    } else {
      None
    }
  }

  override def generateSBOM(): Either[Path, String] = {
    checkRequirements() match {
      case Some(message) =>
        Log.logError(message)
        Log.logError("Requirements not satisfied. Aborting.")
        Right(message)
      case None => startCycloneDX()
    }
  }
}
