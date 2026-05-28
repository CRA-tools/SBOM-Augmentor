package app

import call_graphs.{JoernAnalysis, UnsafeCallDetected, UnsafeFunction, UnsafeIdentifierDetected, UnsafeUsagesAnalyser, UnsafeUsagesResult}
import dep_pattern_matcher.DepMain
import io.circe.JsonObject
import os.Path
import sbom.{SBOM, SBOMAugmentor, SBOMLoader}
import sbom.generators.python.{CycloneDX, CycloneDxInvocationException}
import sbom.include_dependencies.StandardLibraryComponentGenerator
import util.{ConfigurationException, Log}

object Start {

  val name: String = "SBOM Augmentor"

  def scanUnsafeUsages(unsafeFunctionsToCheck: Seq[UnsafeFunction], inputProjectPath: String): UnsafeUsagesScan = {
    val unsafeUsages = if (unsafeFunctionsToCheck.nonEmpty) {
      val importedLibraries = DepMain.run_main(inputProjectPath)
      val joern = new JoernAnalysis(unsafeFunctionsToCheck.toSet, importedLibraries.map(_._2))
      val unsafeUsagesResult = UnsafeUsagesAnalyser.analyseUnsafeUsages(inputProjectPath, joern)
      unsafeUsagesResult
    } else {
      UnsafeUsagesResult(Set(), Set())
    }
    UnsafeUsagesScan(unsafeUsages)
  }

  def generate_sbom(startupConfiguration: StartupConfiguration): PreciseSBOM = {
    loadOrGenerateSbom(startupConfiguration) match {
      case Left(sbomPath) =>
        val sbom: JsonObject = loadSBOM(sbomPath)
        val importedLibraries = DepMain.run_main(startupConfiguration.inputProjectPath)
        val componentGenerator = new StandardLibraryComponentGenerator(startupConfiguration.pythonVersion)
        val augmentor = new SBOMAugmentor(componentGenerator, startupConfiguration)
        val onlyModuleNames = importedLibraries.map(_._1)
        val preciseSBOM = augmentor.makePrecise(sbom, onlyModuleNames)
        preciseSBOM
      case Right(message) =>
        Log.logError(message)
        throw CycloneDxInvocationException(message)
    }
  }

  private def loadOrGenerateSbom(startupConfiguration: StartupConfiguration): Either[Path, String] =
    (startupConfiguration.inputSbomPath, startupConfiguration.inputManifestPath) match {
      case (Some(inputSbomPath), Some(inputManifestPath)) =>
        Log.logMessage("Both an SBOM file and a manifest file were provided as input.\n" +
          "Continuing with the SBOM file and ignoring the manifest file.")
        Left(Path(inputSbomPath))
      case (Some(inputSbomPath), _) => Left(Path(inputSbomPath))
      case (None, Some(inputManifestPath)) => generateSBOM(Path(inputManifestPath))
      case (None, None) =>
        throw ConfigurationException("Neither an SBOM file or a manifest file were provided. Exiting")
    }

  private def loadSBOM(sbomPath: Path): JsonObject = {
    val sbomLoader = new SBOMLoader
    val (sbom, _) = sbomLoader.loadJson(sbomPath)
    sbom
  }

  private def generateSBOM(requirementsPath: Path): Either[Path, String] = {
    val sbomGenerator = new CycloneDX(requirementsPath)
    sbomGenerator.generateSBOM()
  }

}
