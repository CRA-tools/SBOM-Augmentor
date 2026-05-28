package sbom

import os.Path
import scala.collection.immutable.Map

import io.circe.*
import io.circe.parser.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import util.Log

class SBOMLoader {

  private var cache: Map[String, Json] = Map()

  protected implicit val decoder: Decoder[Json] = deriveDecoder[Json]
  protected implicit val encoder: Encoder[Json] = deriveEncoder[Json]

  protected def isLibraryComponent(jsonObject: JsonObject): Option[Boolean] = {
    val typeField = "type"
    jsonObject.toMap.get(typeField) match {
      case None =>
        Log.logError(s"Object $jsonObject does not have a field '$typeField'")
        None
      case Some(typeValue) =>
        typeValue.asString match {
          case None =>
            Log.logError(s"$typeValue is not a string")
            None
          case Some(typeString) => Some(typeString == "library")
        }
    }
  }

  protected def readName(jsonObject: JsonObject): Option[String] = {
    val nameField = "name"
    jsonObject.toMap.get(nameField) match {
      case None =>
        Log.logError(s"Object $jsonObject does not have a field '$nameField'")
        None
      case Some(nameValue) => nameValue.asString
    }
  }

  protected def readComponents(sbomObject: JsonObject): Option[Vector[Json]] = {
    val componentsFieldName = "components"
    sbomObject.toMap.get(componentsFieldName) match {
      case None =>
        Log.logError(s"Object $sbomObject does not have a field '$componentsFieldName'")
        None
      case Some(value) => value.asArray
    }
  }

  protected def parseAsJsonDictOrExit(path: Path, content: String): Option[JsonObject] = {
    val optContent = parse(content)
    optContent match {
      case Left(error) =>
        Log.logError(s"ERROR: Could not parse JSON at $path")
        Log.logError(content)
        System.exit(1)
        None
      case Right(content) =>
        content.asObject match {
          case None =>
            Log.logError(s"ERROR: Could not parse JSON at $path as a dictionary")
            System.exit(1)
            None
          case Some(dict) => Some(dict)
        }
    }
  }

  def loadJson(jsonFilePath: Path): (JsonObject, List[String]) = {
    val jsonFileContent: String = os.read(jsonFilePath)
    val jsonDict = parseAsJsonDictOrExit(jsonFilePath, jsonFileContent).get
    val components = readComponents(jsonDict)
    val libraryComponents = components.get.map(_.asObject.get).filter(component => isLibraryComponent(component).contains(true))
    val libraryNames = libraryComponents.map(readName)
    (jsonDict, libraryNames.toList.flatten)
  }


}
