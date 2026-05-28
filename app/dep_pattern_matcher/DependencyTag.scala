package dep_pattern_matcher

enum DependencyTag:
  case GuardedImport, DynamicImport, CommunityGeneralDeps, GetBinPath, CommunityGeneralCmdRunner

object DependencyTag:
  val TAG_KEY = "PLUGIN_DEPENDENCIES"
