package dev.binclub.bingait.plugins.asm

import dev.binclub.bingait.api.BingaitPlugin
import dev.binclub.bingait.api.event.events.ResourcePanelTabsEvent

/**
 * @author cook 19/Nov/2020
 */
object AsmIntegration: BingaitPlugin() {
	override val id: String = "asm_integration"
	override val name: String = "ASM Integration"
	override val description: String = "Provide integration for the ASM framework"
	override val version: String = "1.0"
	
	init {
		register { event: ResourcePanelTabsEvent ->
			event.apply {
				tabs["ASM"] = AsmTreePanel(treeItem, resourcePath, byteProvider, classPathProvider)
			}
		}
	}
}
