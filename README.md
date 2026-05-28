# SBOM Augmentor
The SBOM Augmentor is a tool for enhancing software-bills-of-materials (SBOMs) of Python applications by:
- Pruning unused dependencies from the SBOM
- Adding standard libraries used in the code to the SBOM
- Scanning the application for usages of unsafe functions found via the SBOM

## Requirements
- [SBT](https://www.scala-sbt.org/) version 1.11 or higher, with Scala 3.8
- [Python 3.10](https://www.python.org/) or higher
- [CycloneDX Python](https://github.com/CycloneDX/cyclonedx-python)


## Installation
1. Download the SBOM Augmentor
2. Compile it into a JAR file by executing `sbt assembly`

Alternatibely, a Docker image is also available: [mvdcamme/sbom_augmentor](https://hub.docker.com/repository/docker/mvdcamme/sbom_augmentor)

## Usage
The SBOM Augmentor can be run both as a command-line tool and as a REST API.

### CLI
To run the tool via the command-line:

```
./augmentor
  --help                   Prints the help message
  <project>                Path to the project to be analysed
  --manifest <value>       Path to the manifest file of the project to be analysed
  --sbom <value>
  -o, --output <value>     Output path where the augmented SBOM will be saved [default: "sbom_out"]
  --include_standard_library
                           Include imported Python standard libraries in the SBOM document (for example, when also shipping the Python runtime)
  --python_version <value>
                           The Python version to include for the application's standard libraries [default: "3.13.6"]
  --unsafe <module_name1>:<function_name1>,<module_name2>:<function_name2>...
                           Check whether the listed functions from their respective modules are called anywhere in the application.
```

Users can either provide an existing SBOM, which will then be augmented, or they can list the path to the application's manifest file (e.g., `requirements.txt`) if no SBOM has been created yet. In case of the latter, the SBOM Augmentor first invokes CycloneDX Python to generate a default SBOM, and then enhances it.

When passing `--unsafe`, no augmented SBOM will be generated.

### REST API
Run `./augmentor_rest_api` and the SBOM Augmentor will be available on `localhost:9000`.
