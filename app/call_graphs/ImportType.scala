package call_graphs

sealed trait ImportType {
  def moduleName: String
}

case class ImportedFullName(moduleName: String) extends ImportType
case class ImportedAliasedLibrary(moduleName: String, alias: String) extends ImportType
case class SelectedAliasedImport(moduleName: String, selectedFunction: String, alias: String) extends ImportType
case class SelectedImport(moduleName: String, selectedFunction: String) extends ImportType
case class WildCardImports(moduleName: String) extends ImportType
