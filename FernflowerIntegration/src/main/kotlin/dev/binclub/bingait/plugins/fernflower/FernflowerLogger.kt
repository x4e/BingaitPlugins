package dev.binclub.bingait.plugins.fernflower

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger

/**
 * @author cook 16/Nov/2020
 */
class FernflowerLogger: IFernflowerLogger() {
	override fun writeMessage(p0: String?, p1: Severity?) {}
	override fun writeMessage(p0: String?, p1: Severity?, p2: Throwable?) {}
}
