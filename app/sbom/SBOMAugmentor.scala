package sbom

import app.{PreciseSBOM, StartupConfiguration}
import os.Path
import io.circe.{Json, JsonObject}
import sbom.generators.python.PythonSBOMExtender
import sbom.include_dependencies.StandardLibraryComponentGenerator
import sbom.package_information.{DependencyWheelInspector, UrlExtractor}

class SBOMAugmentor(val componentGenerator: StandardLibraryComponentGenerator,
                    val config: StartupConfiguration) extends SBOMLoader {

  def libraryRefersToImportedLibrary(importedLibrary: String, libraryName: String): Boolean = {
    // Include the component if it is in the set of imported libraries
    // Rather than checking whether the library name appears literally in the set of imported libraries,
    // we include it if we find it as a substring of an imported library (or vice versa).
    // This leads to false includes, but reduces the risk of incorrectly excluding a library
    importedLibrary.contains(libraryName) || libraryName.contains(importedLibrary)
  }

  def shouldIncludeComponent(component: JsonObject, importedLibraries: Set[String]): Boolean = {
    val isLibrary = isLibraryComponent(component)
    if (isLibrary.isEmpty || isLibrary.contains(false)) {
      // Always include everything that is not a library
      true
    } else {
      val libraryName = readName(component).get
      importedLibraries.exists(importedLibrary => libraryRefersToImportedLibrary(importedLibrary, libraryName))
    }
  }

  def componentUsedWithAlternateLibraryNames(component: JsonObject, libraryNames: Set[String], importedLibraries: Set[String]): Boolean = {
    val isLibrary = isLibraryComponent(component)
    if (isLibrary.isEmpty || isLibrary.contains(false)) {
      // Always include everything that is not a library
      true
    } else {
      // Is there any overlap at all between (any of) the library name(s) of this component, and the set of
      // libraries that are actually imported?
      libraryNames.exists(libraryName => {
        importedLibraries.exists(importedLibrary => libraryRefersToImportedLibrary(importedLibrary, libraryName))
      })
    }
  }

  private def getDepNameAndUrl(nonIncludedComponents: Vector[JsonObject]): Set[(JsonObject, String)] = {
    val optExtractedComponents: Vector[Option[(JsonObject, String)]] = nonIncludedComponents.map(UrlExtractor.extractUrlFromSBOMComponent)
    val mapWithoutExtractedComponents: Set[(JsonObject, String)] = optExtractedComponents.filter(_.isDefined).map(_.get).toSet
    mapWithoutExtractedComponents
  }

  private def componentsToComponentNames(components: Seq[JsonObject]): Seq[String] = {
    val defaultName = "<unknown>"
    components.map((component: JsonObject) => {
      try {
        val asMap = component.toMap
        asMap.get("name").flatMap(_.asString) match {
          case Some(name) => name
          case None => defaultName
        }
      } catch {
        case _: Throwable => defaultName
      }
    })
  }

  def makePrecise(sbom: JsonObject, importedLibraries: Set[String]): PreciseSBOM = {
    val v: Vector[Json] = readComponents(sbom).get
    val components: Vector[JsonObject] = v.map(_.asObject.get)
    val (includedComponents: Vector[JsonObject], nonIncludedComponents: Vector[JsonObject]) = components.partition(component => shouldIncludeComponent(component, importedLibraries))
    val depNamesAndUrls: Set[(JsonObject, String)] = getDepNameAndUrl(nonIncludedComponents)
    val packageNameAndLibNames: Set[(JsonObject, Set[String])] = depNamesAndUrls.map((depName, url) => (depName, DependencyWheelInspector.downloadDependencyWheel(url)))

    val componentsIncludedWithAlternativeName: Set[(JsonObject, Set[String])] = packageNameAndLibNames.filter(componentAndLibNames => {
      val (component, libNames) = componentAndLibNames
      componentUsedWithAlternateLibraryNames(component, libNames, importedLibraries)
    })
    val onlyComponents = componentsIncludedWithAlternativeName.map(_._1)

    val allFilteredComponents: Vector[JsonObject] = includedComponents ++ onlyComponents
    val allFilteredComponentsAsJson: Vector[Json] = allFilteredComponents.map(_.toJson)
    val sizeDifference = components.length - allFilteredComponents.length
    val originalComponentNames = componentsToComponentNames(components).toSet
    val remainingComponentNames = componentsToComponentNames(allFilteredComponents).toSet
    val prunedComponentNames = originalComponentNames -- remainingComponentNames

    val m: Map[String, Json] = sbom.toMap.updated("components", Json.fromValues(allFilteredComponentsAsJson))
    val filteredSbom = JsonObject.fromMap(m)

    if (config.includeStandardLibraries) {
      val sbomExtender = new PythonSBOMExtender(importedLibraries, componentGenerator)
      val (newSbomObject, nrOfComponentsAdded) = sbomExtender.extendsSBOM(filteredSbom)

      val extendedComponentsSet: Set[Json] = importedLibraries.map(componentGenerator.generateSBOMComponent)
      val extendedComponentsVector = extendedComponentsSet.toVector
      val numberOfComponentsAdded = extendedComponentsVector.length - components.length
      PreciseSBOM(newSbomObject, prunedComponentNames, Some(nrOfComponentsAdded))
    } else {
      PreciseSBOM(filteredSbom, prunedComponentNames, None)
    }
  }
}
