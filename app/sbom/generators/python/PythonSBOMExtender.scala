package sbom.generators.python

import java.nio.file.{Files, Path}
import io.circe.{Json, JsonObject, parser}

import app.PathLocations
import sbom.include_dependencies.StandardLibraryComponentGenerator
import sbom.{ExtendsSBOM, SBOM}
import util.{Environment, Log}

class PythonSBOMExtender(private val importedLibraries: Set[String],
                         val componentGenerator: StandardLibraryComponentGenerator) extends ExtendsSBOM {

  protected val pythonStandardLibraries: Set[String] = readPythonStandardLibraries()

  private def readPythonStandardLibraries(): Set[String] = {
    val path = PathLocations.pythonStdLibs

    val jsonFileContent: String = Files.readString(path.toNIO)
    parser.parse(jsonFileContent) match {
      case Left(failure) =>
        Log.logError(s"Could not read list of Python standard libraries, defaulting to empty set")
        Set()
      case Right(json) =>
        val asStringVec: Vector[Option[String]] = json.asArray.getOrElse(Vector()).map(jsonValue => jsonValue.asString)
        val stringVec: Vector[String] = asStringVec.filter({
          case Some(string) => true
          case None => false
        }).map((value: Option[String]) => value.get)
        stringVec.toSet
    }
  }

  private def shouldIncludeInSBOM(library: String): Boolean = {
    val blackList: List[String] = List("__future__", "__main__")
    (!blackList.contains(library) && pythonStandardLibraries.contains(library))
  }

  private def libraryAlreadyIncluded(sbom: SBOM, libraryName: String): Boolean = {
    def readComponentName(component: JsonObject): Option[String] = {
      component.toMap.get("name").flatMap(x => x.asString)
    }
    /*
     * Checks whether the given component of the SBOM describes the same library as `libraryName`
     */
    def libraryNameMatchesComponent(component: Json): Boolean = {
      component.asObject match {
        case Some(componentsObj) =>
          // Only return true if the "name" field of the object actually refers to a string and this
          // string equals the given libraryName
          readComponentName(componentsObj).contains(libraryName)
        case None => false
      }
    }
    sbom.toMap.get("components") match {
      case Some(components) =>
        components.arrayOrObject(false, vec => vec.exists(libraryNameMatchesComponent), obj => obj.keys.exists(_ == libraryName))
      case None => false
    }
  }

  def extendsSBOM(sbom: SBOM): (SBOM, Int) = {
    var filteredLibraries = importedLibraries.filter(shouldIncludeInSBOM)
    filteredLibraries = filteredLibraries.filter(! libraryAlreadyIncluded(sbom, _))
    val extendedComponentsSet: Set[Json] = filteredLibraries.map(componentGenerator.generateSBOMComponent)
    val existingComponents: Vector[Json] = sbom.toMap.get("components") match {
      case Some(components) => components.asArray.getOrElse(Vector())
      case None => Vector()
    }
    val updatedMap = sbom.toMap.updated("components", Json.fromValues(existingComponents.appendedAll(extendedComponentsSet)))
    (JsonObject.fromMap(updatedMap), filteredLibraries.size)
  }
}
