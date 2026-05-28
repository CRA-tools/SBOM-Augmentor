package sbom.package_information

import org.jsoup.*
import org.jsoup.nodes.{Document, Element}
import java.io.File
import java.net.{HttpURLConnection, URI, URL}
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.sys.process.*

import app.PathLocations

// Tries to download any Python wheel file that is available, given the link to a particular PyPi dependency.
// It then uses a Python script (located at /scripts/inspect_wheel.py) to extract the metadata of the wheel
// file in order to determine the name of the actual dependency (or dependencies).

// This seems to be one the few approaches available to find these names without having to actually
// install the dependency itself.
object DependencyWheelInspector {

  private def extractWheelUrl(anchorElement: Element): Option[String] = {
    val anchorValue = anchorElement.nodeValue()
    if (anchorValue.endsWith(".whl")) {
      val maybeHref = anchorElement.attr("abs:href")
      if (maybeHref.nonEmpty) {
        Some(maybeHref)
      } else {
        None
      }
    } else {
      None
    }
  }

  private def findAnyLinkToWheel(document: Document): Option[String] = {
    // Find any anchor link that ends in ".whl", as it will likely refer to the wheel package
    val anchors = document.select("a").asList.asScala
    val optUrlToWheel: Option[String] = anchors.foldLeft(None)((acc, anchorElement) => acc match {
      case Some(link) => Some(link)
      case None => extractWheelUrl(anchorElement)
    })
    optUrlToWheel
  }

  // From: https://stackoverflow.com/a/51976194
  private def downloadInto(url: URL, file: File): Boolean = {
    try {
      val connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setConnectTimeout(5000)
      connection.setReadTimeout(5000)
      connection.connect()
      if (connection.getResponseCode >= 400)
        false
      else {
        (url #> file).!!
        true
      }
    } catch {
      case _: Throwable => false
    }
  }

  private def removeSuffix(name: String, suffix: String) = {
    if (name.endsWith(suffix)) {
      Set(name.replace(suffix, ""))
    } else {
      Set()
    }
  }

  private def fileContentToDependencies(fileOutput: String): Set[String] = {
    val dependenciesMentioned = fileOutput.split("\n").toSet
    val sanitised = dependenciesMentioned.filter(dependencyName => dependencyName.strip().nonEmpty)
    val namesWithoutPyExtensions = sanitised.flatMap((name: String) => {
      removeSuffix(name, ".py") ++ removeSuffix(name, ".so")
    })
    sanitised ++ namesWithoutPyExtensions
  }

  private def extractDependenciesWithPython(wheelFile: File): Option[Set[String]] = {
    try {
      val pathToPythonScript = PathLocations.pythonWheelInspector
      val command = Seq("python3", pathToPythonScript.toString, wheelFile.getAbsolutePath)
      val process = Process(command)
      val pb: ProcessBuilder = process
      var optDependencies: Option[Set[String]] = None
      better.files.File.usingTemporaryFile() { tempFile =>
        val asJavaFile = tempFile.toJava
        (command #> asJavaFile).!!
        val fileContent: String = os.read(os.Path(asJavaFile.getAbsolutePath))
        optDependencies = Some(fileContentToDependencies(fileContent))
      }
      optDependencies
    } catch {
      case e: Throwable => None
    }
  }

  private def checkDependenciesOfLink(link: String): Option[Set[String]] = {
    try {
      var optDependencies: Option[Set[String]] = None
      val url = URI.create(link).toURL
      better.files.File.usingTemporaryFile() { tempFile =>
        val asJavaFile = tempFile.toJava
        val success = downloadInto(url, asJavaFile)
        if (success) {
          optDependencies = extractDependenciesWithPython(asJavaFile)
        } else {
          optDependencies = None
        }
      }
      optDependencies
    } catch {
      case _: Throwable => None
    }
  }

  def downloadDependencyWheel(urlToPipFiles: String): Set[String] = {
    try {
      val document = Jsoup.connect(urlToPipFiles).get()
      val optUrlToWheel: Option[String] = findAnyLinkToWheel(document)
      optUrlToWheel match {
        case Some(url) =>
          val dependencies = checkDependenciesOfLink(url)
          dependencies.getOrElse(Set())
        case None => Set()
      }
    } catch {
      case _: Throwable => Set()
    }
  }
}
