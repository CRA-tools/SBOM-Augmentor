package sbom

import os.Path

trait SBOMGenerator {

  def generateSBOM(): Either[Path, String]

}
