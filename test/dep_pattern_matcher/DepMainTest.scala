package dep_pattern_matcher

import call_graphs._
import org.scalatest.funsuite.AnyFunSuite

class DepMainTest extends AnyFunSuite {

  test("Check whether a WildCardImports is parsed correctly") {
    val optImportType1 = DepMain.convertCodeToImportType("import(foo, *)")
    assert(optImportType1.isDefined)
    assert(optImportType1.contains(WildCardImports("foo")))
    val optImportType2 = DepMain.convertCodeToImportType("import(  abcdefghij   ,* )")
    assert(optImportType2.isDefined)
    assert(optImportType2.contains(WildCardImports("abcdefghij")))
  }

  test("Check whether an ImportedFullName is parsed correctly") {
    val optImportType1 = DepMain.convertCodeToImportType("import(, foo)")
    assert(optImportType1.isDefined)
    assert(optImportType1.contains(ImportedFullName("foo")))
    val optImportType2 = DepMain.convertCodeToImportType("import(, abcdefghij  )")
    assert(optImportType2.isDefined)
    assert(optImportType2.contains(ImportedFullName("abcdefghij")))
  }

  test("Check whether an ImportedAliasedLibrary is parsed correctly") {
    val optImportType1 = DepMain.convertCodeToImportType("import(, foo, f)")
    assert(optImportType1.isDefined)
    assert(optImportType1.contains(ImportedAliasedLibrary("foo", "f")))
    val optImportType2 = DepMain.convertCodeToImportType("import(, abcdefghij,   abc  )")
    assert(optImportType2.isDefined)
    assert(optImportType2.contains(ImportedAliasedLibrary("abcdefghij", "abc")))
    val optImportType3 = DepMain.convertCodeToImportType("import(, abcdefghij,   abcdefghij  )")
    assert(optImportType3.isDefined)
    assert(optImportType3.contains(ImportedAliasedLibrary("abcdefghij", "abcdefghij")))
  }

  test("Check whether a SelectedImport is parsed correctly") {
    val optImportType1 = DepMain.convertCodeToImportType("import(foo, f)")
    assert(optImportType1.isDefined)
    assert(optImportType1.contains(SelectedImport("foo", "f")))
    val optImportType2 = DepMain.convertCodeToImportType("import(abcdefghij  ,   a  )")
    assert(optImportType2.isDefined)
    assert(optImportType2.contains(SelectedImport("abcdefghij", "a")))
  }

  test("Check whether a SelectedAliasedImport is parsed correctly") {
    val optImportType1 = DepMain.convertCodeToImportType("import(foo, f, f_of_foo)")
    assert(optImportType1.isDefined)
    assert(optImportType1.contains(SelectedAliasedImport("foo", "f", "f_of_foo")))
    val optImportType2 = DepMain.convertCodeToImportType("import(abcdefghij  ,   a  , abc_a  )")
    assert(optImportType2.isDefined)
    assert(optImportType2.contains(SelectedAliasedImport("abcdefghij", "a", "abc_a")))
  }

  test("Check whether an invalid import produces a None") {
    assert(DepMain.convertCodeToImportType("i(foo, *)").isEmpty)
    assert(DepMain.convertCodeToImportType("import()").isEmpty)
  }

}
