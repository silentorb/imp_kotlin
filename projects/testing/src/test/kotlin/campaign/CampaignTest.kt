package campaign

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import silentorb.imp.campaign.codeFromFile
import silentorb.imp.campaign.getModulesExecutionArtifacts
import silentorb.imp.campaign.loadModules
import silentorb.imp.campaign.loadWorkspace
import silentorb.imp.core.PathKey
import silentorb.imp.core.defaultImpNamespace
import silentorb.imp.core.getGraphOutputNodes
import silentorb.imp.core.mergeNamespaces
import silentorb.imp.execution.executeToSingleValue
import silentorb.imp.library.standard.standardLibrary
import silentorb.imp.testing.errored
import java.nio.file.Paths

class CampaignTest {
  @Test
  fun canLoadAndExecuteWorkspaces() {
    val initialContext = listOf(defaultImpNamespace(), standardLibrary())
    val workspaceUrl = Thread.currentThread().contextClassLoader.getResource("project1/workspace.yaml")!!
    val (workspace, errors) = loadWorkspace(Paths.get(workspaceUrl.toURI()).parent)
    assertTrue(errors.none()) { errors.first().message.toString() }
    errored(errors)
    val modulesResponse = loadModules(workspace, initialContext, codeFromFile)
    val modules = modulesResponse.value
    assertEquals(2, modules.size)
    assertEquals(1, modules["assets"]!!.dungeons.size)
    assertEquals(1, modules["lib"]!!.dungeons.size)
    val context= getModulesExecutionArtifacts(initialContext, modules)
    val outputs = getGraphOutputNodes(mergeNamespaces(context))
        .filter { it.path == "assets" }

    assertEquals(2, outputs.size)
    val mouseValue = executeToSingleValue(context, PathKey("assets", "mouse"))
    val ravenValue = executeToSingleValue(context, PathKey("assets", "raven"))
    assertEquals(11, mouseValue)
    assertEquals(21, ravenValue)
  }
}
