package dev.binclub.bingait.plugins.cfr

import dev.binclub.bingait.api.BingaitPlugin
import dev.binclub.bingait.api.event.events.ResourcePanelTabsEvent

/**
 * Provide integration with the CFR decompiler
 *
 * @author cook 16/Sep/2020
 */
object CfrIntegration: BingaitPlugin() {
	override val id: String = "cfr_integration"
	override val name: String = "CFR Integration"
	override val description: String = "Adds integration with benf's CFR"
	override val version: String = "1.0"
	
	init {
		register { event: ResourcePanelTabsEvent ->
			event.apply {
				tabs["CFR"] = CfrResourcePanel(resourcePath, byteProvider, classPathProvider)
				tabs["CFR Tree"] = CfrTreePanel(resourcePath, byteProvider, classPathProvider)
			}
		}
	}
}
