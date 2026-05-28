package app

import call_graphs.UnsafeUsagesResult
import sbom.SBOM

sealed trait SBOMAugmentorResult
case class PreciseSBOM(sbom: SBOM, prunedComponentsNames: Set[String], optNumberOfAddedComponents: Option[Int]) extends SBOMAugmentorResult
case class UnsafeUsagesScan(unsafeUsagesResult: UnsafeUsagesResult) extends SBOMAugmentorResult
