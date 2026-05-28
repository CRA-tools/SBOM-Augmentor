package sbom.package_information

import io.circe.{Json, JsonObject}

object UrlExtractor {

  private def fetchUrlFromExternalReference(reference: Json): Option[String] = {
    reference.asObject match {
      case None => None
      case Some(referenceObject) =>
        val asMap = referenceObject.toMap
        val optType = asMap.get("type")
        // Check whether the external reference actually refers to the PyPi distribution
        if (optType.flatMap(_.asString).contains("distribution")) {
          val optUrl = asMap.get("url")
          optUrl.flatMap(_.asString) match {
            case None => None
            case Some(url) =>
              // Does the url refer to a pypi link?
              if (url.contains("pypi.org")) {
                Some(url)
              } else {
                None
              }
          }
        } else {
          None
        }
    }
  }

  def extractUrlFromSBOMComponent(component: JsonObject): Option[(JsonObject, String)] = {
    val asMap = component.toMap
    asMap.get("externalReferences") match {
      case None => None
      case Some(references) =>
        references.asArray match {
          case None => None
          case Some(arrayOfReferences) =>
            val result: Option[String] = arrayOfReferences.foldLeft(None)((acc, reference) => acc match {
              case Some(url) => Some(url)
              case None => fetchUrlFromExternalReference(reference)
            })
            result match {
              case None => None
              case Some(url) => Some((component, url))
            }
        }
    }
  }

}
