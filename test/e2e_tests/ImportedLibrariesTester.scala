package e2e_tests

import call_graphs.*
import dep_pattern_matcher.DepMain
import org.scalatest.funsuite.AnyFunSuite
import os.Path

class ImportedLibrariesTester extends AnyFunSuite {

  test("Check the imported libraries of the Python fizzbuzz.py fixture") {
    val pathToFixture = pathToFixtures / "python_applications" / "fizzbuzz" / "fizzbuzz.py"
    val importedLibraries = DepMain.run_main(pathToFixture.toString)
    assert(importedLibraries.size == 7)
    assert(importedLibraries.contains("fibonacci", ImportedFullName("fibonacci")))
    assert(importedLibraries.contains("palindrome", ImportedAliasedLibrary("palindrome", "p")))
    assert(importedLibraries.contains("bar", WildCardImports("bar")))
    assert(importedLibraries.contains("foo", SelectedAliasedImport("foo", "bb", "foo_bb")))
    assert(importedLibraries.contains("foo", SelectedAliasedImport("foo", "cc", "foo_cc")))
    assert(importedLibraries.contains("foobar", SelectedImport("foobar", "foo")))
    assert(importedLibraries.contains("foobar", SelectedImport("foobar", "g")))
  }

  test("Check the imported libraries of the Python code in the fizzbuzz folder") {
    val pathToFixture = pathToFixtures / "python_applications" / "fizzbuzz"
    val importedLibraries = DepMain.run_main(pathToFixture.toString)
    assert(importedLibraries.size == 9)
    assert(importedLibraries.contains("fibonacci", ImportedFullName("fibonacci")))
    assert(importedLibraries.contains("re", ImportedFullName("re")))
    assert(importedLibraries.contains("palindrome", ImportedAliasedLibrary("palindrome", "p")))
    assert(importedLibraries.contains("bar", WildCardImports("bar")))
    assert(importedLibraries.contains("foo", SelectedAliasedImport("foo", "bb", "foo_bb")))
    assert(importedLibraries.contains("foo", SelectedAliasedImport("foo", "cc", "foo_cc")))
    assert(importedLibraries.contains("foobar", SelectedImport("foobar", "foo")))
    assert(importedLibraries.contains("foobar", SelectedImport("foobar", "g")))
    assert(importedLibraries.contains("bar", SelectedImport("bar", "b")))
  }

}
