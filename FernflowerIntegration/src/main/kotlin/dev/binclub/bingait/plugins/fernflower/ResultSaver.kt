package dev.binclub.bingait.plugins.fernflower

import org.jetbrains.java.decompiler.main.extern.IResultSaver
import java.util.jar.Manifest

/**
 * @author cook 16/Nov/2020
 */
class ResultSaver: IResultSaver {
	var output: String? = null
	
	override fun saveClassFile(path: String?, qualifiedName: String?, entryName: String?, content: String?, mapping: IntArray?) {
		if (this.output == null)
			this.output = content
	}
	
	
	override fun saveFolder(p0: String?) {}
	override fun copyFile(p0: String?, p1: String?, p2: String?) {}
	override fun createArchive(p0: String?, p1: String?, p2: Manifest?) {}
	override fun saveDirEntry(p0: String?, p1: String?, p2: String?) {}
	override fun copyEntry(p0: String?, p1: String?, p2: String?, p3: String?) {}
	override fun saveClassEntry(p0: String?, p1: String?, p2: String?, p3: String?, p4: String?) {}
	override fun closeArchive(p0: String?, p1: String?) {}
}
