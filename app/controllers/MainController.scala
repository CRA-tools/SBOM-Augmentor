package controllers

import app._
import better.files
import call_graphs._

import java.io.File
import java.nio.file.{Files, Path}
import javax.inject.*
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.util.ByteString
import play.api.*
import play.api.data.Form
import play.api.data.Forms.*
import play.api.libs.streams.*
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.*
import play.core.parsers.Multipart.FileInfo
import io.circe.Json

import java.util.zip.ZipEntry
import scala.concurrent.{ExecutionContext, Future}

// Contains the fields of the request for generating an augmented SBOM
case class SBOMFormDataExtra(inputProject: Option[File],
                             inputManifest: Option[File],
                             inputSBOM: Option[File],
                             pythonVersion: String,
                             includeStandardLibraries: Boolean)

object SBOMFormDataExtra {
  def unapply(formData: SBOMFormDataExtra): Option[(Option[File], Option[File], Option[File], String, Boolean)] = {
    Some(formData.inputProject, formData.inputManifest, formData.inputSBOM, formData.pythonVersion, formData.includeStandardLibraries)
  }
}

// Contains the fields of the request for scanning a project
case class ProjectScanForm(inputProject: Option[File],
                           unsafeFunctionsToCheck: String)

object ProjectScanForm {
  def unapply(formData: ProjectScanForm): Option[(Option[File], String)] = {
    Some(formData.inputProject, formData.unsafeFunctionsToCheck)
  }
}

@Singleton
class MainController @Inject()(cc:MessagesControllerComponents)
                              (implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = Logger(this.getClass)

  val form = Form(
    mapping(
      "inputProject" -> ignored(Option.empty[java.io.File]),
      "inputManifest" -> ignored(Option.empty[java.io.File]),
      "inputSBOM" -> ignored(Option.empty[java.io.File]),
      "pythonVersion" -> text,
      "includeStandardLibraries" -> boolean
    )(SBOMFormDataExtra.apply)(SBOMFormDataExtra.unapply)
  )

  val scanForm = Form(
    mapping(
      "inputProject" -> ignored(Option.empty[java.io.File]),
      "unsafeFunctionsToCheck" -> text
    )(ProjectScanForm.apply)(ProjectScanForm.unapply)
  )

  def toStartupConfiguration(SBOMFormDataExtra: SBOMFormDataExtra,
                             optInputProject: Option[File],
                             optInputManifest: Option[File],
                             optInputSbom: Option[File]): Option[StartupConfiguration] = {
    val pythonVersion = if (SBOMFormDataExtra.pythonVersion.isBlank) {
      StartupConfiguration.defaultPythonVersion
    } else {
      SBOMFormDataExtra.pythonVersion
    }
    optInputProject match {
      case None => None
      case Some(inputProject) =>
        val unzippedFolderPath: String = extractZippedProject(optInputProject)
        Some(StartupConfiguration(
          unzippedFolderPath,
          optInputManifest.map(_.getPath),
          optInputSbom.map(_.getPath),
          None,
          pythonVersion,
          SBOMFormDataExtra.includeStandardLibraries,
          Nil
        ))
    }
  }

  def toStartupScanConfiguration(optInputProject: Option[File],
                                 unsafeFunctions: Seq[UnsafeFunction]): Option[StartupConfiguration] = {
    optInputProject match {
      case None => None
      case Some(inputProject) =>
        val unzippedFolderPath: String = extractZippedProject(optInputProject)
        Some(StartupConfiguration(
          unzippedFolderPath,
          output = None,
          unsafeFunctionsToCheck = unsafeFunctions
        ))
    }
  }

  private def extractZippedProject(optInputProject: Option[File]): String = {
    val zipped: files.File = better.files.File(optInputProject.get.toPath)
    val unzippedFolder: files.File = zipped.unzip((z: ZipEntry) => {
      !z.getName.startsWith("__MACOSX")
    })
    val unzippedFolderPath = unzippedFolder.path.toString
    unzippedFolderPath
  }

  /**
   * Uses a custom FilePartHandler to return a type of "File" rather than
   * using Play's TemporaryFile class.  Deletion must happen explicitly on
   * completion, rather than TemporaryFile (which uses finalization to
   * delete temporary files).
   *
   * @return
   */
  private def handleFilePartAsFile(fileInfo: FileInfo): Accumulator[ByteString, FilePart[File]] = fileInfo match {
    case FileInfo(partName, filename, contentType, _) =>
      val path: Path = Files.createTempFile(filename, "tempFile")
      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(path)
      val accumulator: Accumulator[ByteString, IOResult] = Accumulator(fileSink)
      accumulator.map {
        case IOResult(count, status) =>
          logger.info(s"count = $count, status = $status")
          FilePart(partName, filename, contentType, path.toFile)
      }
  }

  /**
   * Renders a start page.
   */
  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index(form))
  }

  def scan: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.scan(scanForm))
  }

  private def unsafeUsages(unsafeUsages: Set[UnsafeUsage]): Json = {
    val map: Map[String, Vector[Json]] = Map()
    val filledInMap = unsafeUsages.foldLeft(map)((map: Map[String, Vector[Json]], usage: UnsafeUsage) => {
      val name = usage.name
      val optPreviousEntries = map.get(name)
      optPreviousEntries match {
        case None =>
          map + (name -> Set[Json](io.circe.Json.fromString(usage.formatAsString)).toVector)
        case Some(previousVector) =>
          map.updated(name, previousVector :+ io.circe.Json.fromString(usage.formatAsString))
      }
    })
    val convertedToJsonValues = filledInMap.map[String, Json]((tuple: (String, Vector[Json])) => (tuple._1, io.circe.Json.fromValues(tuple._2)))
    io.circe.JsonObject.fromMap(convertedToJsonValues).toJson
  }

  private def unsafeUsagesScanToJson(result: UnsafeUsagesResult): Json = {
    if (result.unsafeCalls.nonEmpty) {
      unsafeUsages(result.unsafeCalls.map((call: UnsafeCallDetected) => call))
    } else {
      if (result.unsafeIdentifiersUsed.nonEmpty) {
        unsafeUsages(result.unsafeIdentifiersUsed.map((call: UnsafeIdentifierDetected) => call))
      } else {
        io.circe.Json.fromValues(Nil)
      }
    }
  }

  private def printResultsOfAugmentation(result: PreciseSBOM): Unit = {
    val sizeDifference = result.prunedComponentsNames.size
    val componentsMessage = if sizeDifference == 1 then "1 unused component" else s"$sizeDifference unused components"
    val formattedComponentNames = result.prunedComponentsNames.mkString(", ")
    println(s"${Console.GREEN}Removed $componentsMessage from SBOM: $formattedComponentNames${Console.RESET}")
    result.optNumberOfAddedComponents match {
      case None =>
      case Some(nrOfComponentsAdded) =>
        println(s"${Console.GREEN}Added $nrOfComponentsAdded component(s)${Console.RESET}")
    }
  }

  private def start(config: StartupConfiguration): Either[Json, String] = {
    try {
      if (config.unsafeFunctionsToCheck.nonEmpty) {
        val maybeResult = do_scan(config.inputProjectPath, config.unsafeFunctionsToCheck)
        maybeResult
      } else {
        val result = Start.generate_sbom(config)
        printResultsOfAugmentation(result)
        val sbom = result.sbom
        Left(sbom.toJson)
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        Right(e.getMessage)
    }
  }

  private def do_scan(inputProjectPath: String, unsafeFunctionsToCheck: Seq[UnsafeFunction]): Either[Json, String] = {
    try {
      val result = Start.scanUnsafeUsages(unsafeFunctionsToCheck, inputProjectPath)
      val asJson = unsafeUsagesScanToJson(result.unsafeUsagesResult)
      Left(asJson)
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        Right(e.getMessage)
    }
  }

  private def removeTempFiles(config: StartupConfiguration): Unit = {
    def removeOptFile(path: String): Unit = {
      try {
        Files.deleteIfExists(Path.of(path))
      } catch {
        case _: Throwable =>
      }
    }
    config.inputManifestPath.foreach(removeOptFile)
    config.inputSbomPath.foreach(removeOptFile)
    removeOptFile(config.inputProjectPath)
  }

  /**
   * Uploads a multipart file as a POST request.
   *
   * @return
   */
  def upload: Action[MultipartFormData[File]] = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit request =>
    val optInputProject: Option[File] = request.body.file("inputProject").map {
      case FilePart(key, filename, contentType, file, fileSize, dispositionType, _) =>
        file
    }
    val optInputManifest: Option[File] = request.body.file("inputManifest").map {
      case FilePart(key, filename, contentType, file, fileSize, dispositionType, _) =>
        file
    }
    val optInputSBOM: Option[File] = request.body.file("inputSBOM").map {
      case FilePart(key, filename, contentType, file, fileSize, dispositionType, _) =>
        file
    }

    val optFormData: Option[SBOMFormDataExtra] = form.bindFromRequest(request.body.dataParts).bindFromRequest().fold(
      errors => {println(s"errors = $errors"); None},
      {
        x => Some(x)
      }
    )
    optFormData match {
      case None =>
        BadRequest
      case Some(formData) =>
        val optStartupConfig = toStartupConfiguration(formData, optInputProject, optInputManifest, optInputSBOM)
        println(s"optStartupConfig = $optStartupConfig")
        optStartupConfig match {
          case None =>
            BadRequest
          case Some(startupConfig) =>
            val maybeSbom = start(startupConfig)
            removeTempFiles(startupConfig)
            maybeSbom match {
              case Left(sbom) =>
                Ok(sbom.spaces2).as("application/json")
              case Right(message) =>
                Ok(s"SBOM could not be generated: $message")
            }
        }
    }
  }

  // Converts the corresponding values for "module_name_#_1" and "function_name_#_1" into a set of UnsafeFunctions
  private def getUnsafeUsagesFromRequest(request: MessagesRequest[MultipartFormData[File]]): Seq[UnsafeFunction] = {
    var setToScan: Seq[UnsafeFunction] = Nil
    for ((key: String, values: Seq[String]) <- request.body.dataParts) yield {
      if (key.startsWith("module_name_#_")) {
        val optModuleName = values.headOption
        optModuleName match {
          case None =>
          case Some(moduleName) =>
            val optCounterOfKey = key.split("_#_").lastOption
            optCounterOfKey match {
              case None =>
              case Some(counterOfKey) =>
                val keyOfFunctionName = "function_name_#_" + counterOfKey
                val optFunctionName = request.body.dataParts(keyOfFunctionName).headOption
                optFunctionName match {
                  case None =>
                  case Some(functionName) =>
                    if (moduleName.strip().nonEmpty && functionName.strip().nonEmpty) {
                      setToScan :+= UnsafeFunction(moduleName = moduleName, functionName = functionName)
                    }
                }
            }
        }
      }
    }
    setToScan
  }

  def upload_scan: Action[MultipartFormData[File]] = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit request =>
    val optInputProject: Option[File] = request.body.file("inputProject").map {
      case FilePart(key, filename, contentType, file, fileSize, dispositionType, _) =>
        file
    }
    val unsafeFunctions: Seq[UnsafeFunction] = getUnsafeUsagesFromRequest(request)
    val optStartupConfig = toStartupScanConfiguration(optInputProject, unsafeFunctions)
    println(s"optStartupConfig = $optStartupConfig")
    optStartupConfig match {
      case None =>
        BadRequest
      case Some(startupConfig) =>
        val maybeResult = do_scan(startupConfig.inputProjectPath, startupConfig.unsafeFunctionsToCheck)
        removeTempFiles(startupConfig)
        maybeResult match {
          case Left(resultAsJson) =>
            Ok(resultAsJson.spaces2).as("application/json")
          case Right(message) =>
            Ok(s"SBOM could not be generated: $message")
        }
    }
  }
}
