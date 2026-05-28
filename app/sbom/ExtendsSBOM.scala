package sbom

trait ExtendsSBOM {
  def extendsSBOM(sbom: SBOM): (SBOM, Int)
}
