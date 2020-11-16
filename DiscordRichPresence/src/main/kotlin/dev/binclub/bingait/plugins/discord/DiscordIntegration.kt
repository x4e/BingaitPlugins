package dev.binclub.bingait.plugins.discord

import club.minnced.discord.rpc.DiscordEventHandlers
import club.minnced.discord.rpc.DiscordEventHandlers.*
import club.minnced.discord.rpc.DiscordRPC
import club.minnced.discord.rpc.DiscordRichPresence
import dev.binclub.bingait.api.BingaitPlugin
import dev.binclub.bingait.api.event.events.ActivityState
import dev.binclub.bingait.api.event.events.StatusUpdateEvent
import kotlin.concurrent.thread

/**
 * @author cook 09/Sep/2020
 */
class DiscordIntegration: BingaitPlugin() {
	override val id: String = "discord_integration"
	override val name: String = "Discord Rich Presence"
	override val description: String = "Show your status on discord"
	override val version: String = "1.0"
	
	private val appId = "753159691714035772"
	private var currentPresence: DiscordRichPresence? = null
	
	init {
		Runtime.getRuntime()
			.addShutdownHook(
				thread(
					name = "Discord Shutdown",
					isDaemon = false,
					start = false,
					block = DiscordRPC.INSTANCE::Discord_Shutdown
				)
			)
		
		register { event: StatusUpdateEvent ->
			val newPresence = getPresence(event)
			val detailsMatch = newPresence.details == currentPresence?.details
			val statesMatch = newPresence.state == currentPresence?.state
			if (!detailsMatch || !statesMatch) {
				if (detailsMatch) {
					// keep old time if details haven't changed
					newPresence.startTimestamp = currentPresence?.startTimestamp ?: newPresence.startTimestamp
				}
				currentPresence = newPresence
				DiscordRPC.INSTANCE.Discord_UpdatePresence(newPresence)
				DiscordRPC.INSTANCE.Discord_RunCallbacks()
			}
		}
	}
	
	override fun onEnabled() {
		discord {
			val handlers = handlers {
				ready = OnReady { println("Bingait RPC Ready") }
				errored = OnStatus { errorCode, message -> println("Bingait RPC Err $errorCode: $message") }
			}
			
			Discord_Initialize(appId, handlers, true, null)
			
			val startPresence = presence {
				state = "Idle"
				largeImageKey = "binclub"
				startTimestamp = System.currentTimeMillis() / 1000
			}
			Discord_UpdatePresence(startPresence)
		}
		super.onEnabled()
	}
	
	override fun onDisabled() {
		DiscordRPC.INSTANCE.Discord_Shutdown()
		super.onDisabled()
	}
	
	private fun getPresence(event: StatusUpdateEvent): DiscordRichPresence = presence {
		details = event.message
		state = when (event.activity) {
			ActivityState.CLASSTREE -> "Browsing classes"
			ActivityState.BINCODE_AST -> "In BinCode AST"
			ActivityState.CFRTEXT -> "In CFR Text"
			ActivityState.CFRTREE -> "In CFR Tree"
			ActivityState.HEX -> "Viewing hex disassembly"
			else -> null
		}
		
		largeImageKey = "binclub"
		startTimestamp = System.currentTimeMillis() / 1000
	}
}

inline fun discord(block: DiscordRPC.() -> Unit): DiscordRPC = DiscordRPC.INSTANCE.apply(block)
inline fun handlers(block: DiscordEventHandlers.() -> Unit): DiscordEventHandlers = DiscordEventHandlers().apply(block)
inline fun presence(block: DiscordRichPresence.() -> Unit): DiscordRichPresence = DiscordRichPresence().apply(block)
