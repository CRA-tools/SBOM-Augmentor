package app

import app.{CommandLineArgsParser, PreciseSBOM, Start, StartupConfiguration}
import call_graphs.{UnsafeCallDetected, UnsafeIdentifierDetected, UnsafeUsagesResult}
import sbom.SBOM
import util.{HasExitCode, Log, getOutputAbsolutePath}

import java.io.IOException

object Main {

  private def printUnsafeUsagesResult(result: UnsafeUsagesResult): Unit = {
    if (result.unsafeCalls.nonEmpty) {
      result.unsafeCalls.foreach((unsafeCall: UnsafeCallDetected) => {
        println(unsafeCall.formatAsString)
      })
      println(s"${Console.RED}Unsafe calls from vulnerable module have been detected${Console.RESET}")
    } else {
      if (result.unsafeIdentifiersUsed.nonEmpty) {
        result.unsafeIdentifiersUsed.foreach((unsafeIdentifier: UnsafeIdentifierDetected) => {
          println(unsafeIdentifier.formatAsString)
        })
        println(s"${Console.RED}Unsafe identifiers from vulnerable module have been detected${Console.RESET}")
      } else {
        println(s"${Console.GREEN}No unsafe calls or identifiers from vulnerable modules have been detected${Console.RESET}")
      }
    }
  }

  private def writeSBOMToFile(newSbomObject: SBOM, startupConfiguration: StartupConfiguration): Unit = {
    val outputPath = startupConfiguration.output match {
      case None => StartupConfiguration.defaultOutput
      case Some(path) if path.isBlank => StartupConfiguration.defaultOutput
      case Some(path) => path
    }
    val absoluteOutputPath = getOutputAbsolutePath(outputPath)
    try {
      if (os.isFile(absoluteOutputPath)) {
        os.remove(absoluteOutputPath)
      }
      os.write(absoluteOutputPath, newSbomObject.toJson.spaces2)
    } catch {
      case _: Exception => throw new IOException(s"Could not write SBOM to path $absoluteOutputPath")
    }
  }

  private def printResultsOfAugmentation(result: PreciseSBOM): Unit = {
    val sizeDifference = result.prunedComponentsNames.size
    val componentsMessage = if sizeDifference == 1 then "1 unused component" else s"$sizeDifference unused components"
    val formattedComponentNames = result.prunedComponentsNames.mkString(", ")
    println(s"${Console.GREEN}Removed $componentsMessage from SBOM: $formattedComponentNames${Console.RESET}")
    result.optNumberOfAddedComponents match {
      case None =>
      case Some(nrOfComponentsAdded) =>
        println(s"${Console.GREEN}Added $nrOfComponentsAdded component(s)${Console.RESET}")
    }
  }

  private def start(configuration: StartupConfiguration): Either[Unit, Int] = {
    try {
      if (configuration.unsafeFunctionsToCheck.nonEmpty) {
        val result = Start.scanUnsafeUsages(configuration.unsafeFunctionsToCheck, configuration.inputProjectPath)
        printUnsafeUsagesResult(result.unsafeUsagesResult)
      } else {
        val result = Start.generate_sbom(configuration)
        printResultsOfAugmentation(result)
        val sbom = result.sbom
        writeSBOMToFile(sbom, configuration)
      }
      Left(()) // Don't have to do anything, just return with an empty value to indicate a success
    } catch {
      case e: Exception =>
        val exitCode = e match {
          case e: HasExitCode => e.exitCode
          case _ => 1
        }
        Right(exitCode)
    }
  }

  def main(args: Array[String]): Unit = {
    try {
      val optConfig = CommandLineArgsParser.parseArgs(args)
      optConfig match {
        case None =>
          Log.logError(s"${Start.name} invoked incorrectly. Usage:\n${CommandLineArgsParser.usage}")
          System.exit(1)
        case Some(arg) =>
          val result = start(arg)
          result match {
            case Left(_) => // Don't have to do anything
            case Right(exitCode) => System.exit(exitCode)
          }
      }
    } catch {
      case _: Throwable | _: Error => System.exit(2)
    }
  }

}
