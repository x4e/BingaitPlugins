package dev.binclub.bingait.plugins.fernflower

import dev.binclub.bingait.api.BingaitPlugin
import dev.binclub.bingait.api.event.events.ResourcePanelTabsEvent
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler
import java.io.DataInput
import java.io.File
import java.util.*


/**
 * @author cook 09/Sep/2020
 */
object FernflowerIntegration : BingaitPlugin() {
	override val id: String = "fernflower_integration"
	override val name: String = "Fernflower Integration"
	override val description: String = "Show your status on discord"
	override val version: String = "1.0"
	
	init {
		register { event: ResourcePanelTabsEvent ->
			event.apply {
				tabs["Fernflower"] = FernflowerResourcePanel(resourcePath, byteProvider, classPathProvider)
			}
		}
	}
	
	val FFoptions = Collections.emptyMap<String, Any>()
	
	fun decompile(classFileName: String, byteArray: ByteArray, classPathProvider: (String) -> DataInput?): String {
		val resultSaver = ResultSaver()
		
		val decompiler = BaseDecompiler(
			{ externalPath, internalPath ->
				byteArray
			},
			resultSaver,
			FFoptions,
			FernflowerLogger()
		)
		decompiler.addSource(File("_.class"))
		decompiler.decompileContext()
		
		return resultSaver.output ?: error("Fernflower did not provide an output")
	}
}
