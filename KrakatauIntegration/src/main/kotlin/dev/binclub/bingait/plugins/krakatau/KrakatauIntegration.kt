package dev.binclub.bingait.plugins.krakatau

import dev.binclub.bingait.api.BingaitPlugin
import dev.binclub.bingait.api.event.events.ResourcePanelTabsEvent
import dev.binclub.bingait.api.settings.StringSetting
import dev.binclub.bingait.api.util.recursivelyDelete
import dev.binclub.bingait.api.util.traverseDeepFiles
import dev.binclub.bingait.api.util.tree.ArchiveEntryTreeCell
import dev.binclub.bingait.api.util.tree.FileTreeCell
import java.io.DataInput
import java.io.File

/**
 * @author cook 17/Nov/2020
 */
object KrakatauIntegration: BingaitPlugin() {
	override val id: String = "krakatau_integration"
	override val name: String = "Krakatau Integration"
	override val description: String = "Integration with the Krakatau decompiler"
	override val version: String = "1.0"
	
	private val assumedRtPath = System.getProperty("java.home")?.let {
		val f = File(it, "lib/rt.jar")
		if (f.exists()) f.path
		else null
	} ?: ""
	val rt by StringSetting("rt.jar path", assumedRtPath) { old, new -> }
	
	private val assumedPythonPath = System.getenv("PATH")?.let { path ->
		path.split(File.pathSeparator).forEach { dir ->
			File(dir).listFiles()?.forEach {
				if (it.isFile && it.name == "python") {
					return@let it.absolutePath
				}
			}
		}
		null
	} ?: ""
	val pythonPath by StringSetting("Python Path", assumedPythonPath) { old, new -> }
	
	init {
		register { event: ResourcePanelTabsEvent ->
			event.apply {
				tabs["Krakatau"] = KrakatauResourcePanel(treeItem, resourcePath, byteProvider, classPathProvider)
			}
		}
	}
	
	fun decompile(treeItem: Any, classFileName: String, byteArray: ByteArray, classPathProvider: (String) -> DataInput?): String {
		if (!File(rt).exists()) {
			error("Invalid rt path \"$rt\"")
		}
		
		val tmpFile = File.createTempFile("bingait_krakatau_marker", "")
		val outputDir = File(tmpFile.parentFile, "bingait_krakatau_output")
		outputDir.recursivelyDelete()
		outputDir.mkdirs()
		tmpFile.delete()
		
		return a@when (treeItem) {
			is FileTreeCell -> {
				val className = treeItem.file.nameWithoutExtension
				val classpath = arrayListOf(treeItem.file.parentFile.absolutePath, rt)
				val classes = arrayListOf(className)
				PythonManager.decompileClass(pythonPath, classpath, classes, outputDir.absolutePath)
				var outputFile: File? = null
				outputDir.traverseDeepFiles { f ->
					outputFile = f
				}
				return outputFile?.readText() ?: error("No output file")
			}
			is ArchiveEntryTreeCell -> {
				val className = classFileName.removeSuffix(".class")
				val classpath = arrayListOf(treeItem.owningFile.absolutePath, rt)
				val classes = arrayListOf(className)
				PythonManager.decompileClass(pythonPath, classpath, classes, outputDir.absolutePath)
				File(outputDir, "$className.java").readText()
			}
			else -> "Unsupported tree type $treeItem"
		}
	}
}
