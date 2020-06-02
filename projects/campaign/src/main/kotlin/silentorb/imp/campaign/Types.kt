package silentorb.imp.campaign

import silentorb.imp.core.Dependency
import silentorb.imp.core.Dungeon
import silentorb.imp.parsing.general.ParsingErrors
import java.nio.file.Path
import java.nio.file.Paths

typealias ModuleId = String
typealias DungeonId = String

data class Module(
    val path: Path,
    val dungeons: Map<DungeonId, Dungeon>,
    val fileNamespaces: Boolean
)

typealias ModuleDependency = Dependency<ModuleId>

data class Workspace(
    val path: Path,
    val modules: Map<ModuleId, Module>,
    val dependencies: Set<ModuleDependency>
)

val emptyWorkspace = Workspace(
    path = Paths.get(""),
    modules = mapOf(),
    dependencies = setOf()
)

data class ModuleConfig(
    val dependencies: List<String> = listOf(),
    val fileNamespaces: Boolean = false
)

data class WorkspaceConfig(
    val modules: List<String> = listOf() // Module directory patterns
)

data class CampaignError(
    val message: Any,
    val arguments: List<Any> = listOf()
)

typealias CampaignErrors = List<CampaignError>

data class CampaignResponse<T>(
    val value: T,
    val campaignErrors: CampaignErrors,
    val parsingErrors: ParsingErrors
)

data class ModuleInfo(
    val path: Path,
    val config: ModuleConfig,
    val sourceFiles: List<Path>
)
