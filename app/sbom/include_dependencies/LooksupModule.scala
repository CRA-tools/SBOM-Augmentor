package sbom.include_dependencies

import io.circe.Json

trait LooksupModule {

  // {
  //      "bom-ref": "requirements-L4",
  //      "description": "requirements line 4: orjson",
  //      "externalReferences": [
  //        {
  //          "comment": "implicit dist url",
  //          "type": "distribution",
  //          "url": "https://pypi.org/simple/orjson/"
  //        }
  //      ],
  //      "name": "orjson",
  //      "purl": "pkg:pypi/orjson",
  //      "type": "library"
  //    }

  def generateSBOMComponent(moduleName: String): Json

}

object LooksupModule {
  private var id: Int = 1
  def newSbomRefId(): Int = {
    val temp = id
    id += 1
    temp
  }
}
