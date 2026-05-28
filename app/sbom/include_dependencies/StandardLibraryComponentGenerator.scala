package sbom.include_dependencies

import app.Start
import io.circe.{Json, JsonObject}

class StandardLibraryComponentGenerator(pythonVersion: String) extends LooksupModule {

  object SBOMFields {
    val descriptionField = "description"
    val typeField = "type"
    val nameField = "name"
    val versionField = "version"
    val bomRefField = "bom-ref"
  }

//      {
  //      "bom-ref": "requirements-L1",
  //      "description": "requirements line 1: aiohttp>=3.7.4,<4",
  //      "externalReferences": [
  //        {
  //          "comment": "implicit dist url",
  //          "type": "distribution",
  //          "url": "https://pypi.org/simple/aiohttp/"
  //        }
  //      ],
  //      "name": "aiohttp",
  //      "purl": "pkg:pypi/aiohttp",
  //      "type": "library"
  //    },

//  {
  //      "type": "application",
  //      "bom-ref": "CDXRef-DOCUMENT",
  //      "name": "Python-orjson"
  //    }

  protected def generateBomRef: Json = {
    val id = LooksupModule.newSbomRefId()
    Json.fromString(s"${Start.name}_ref_$id")
  }

  override def generateSBOMComponent(moduleName: String): Json = {
    /*
     Some meta-information (e.g., authors, distributor, licenses, CPE...) cannot be
     added unless we know the Python runtime.
     */

    import SBOMFields._
    val descriptionValue: Json = Json.fromString(s"Part of the Python standard library")
    val typeValue: Json = Json.fromString("library")
    val nameValue: Json = Json.fromString(moduleName)
    val versionValue: Json = Json.fromString(pythonVersion)
    val bomRefValue: Json = generateBomRef
    val fields: Map[String, Json] = Map(
      descriptionField -> descriptionValue,
      typeField -> typeValue,
      nameField -> nameValue,
      versionField -> versionValue,
      bomRefField -> bomRefValue
    )
    JsonObject(fields.toSeq*).toJson
  }
}
