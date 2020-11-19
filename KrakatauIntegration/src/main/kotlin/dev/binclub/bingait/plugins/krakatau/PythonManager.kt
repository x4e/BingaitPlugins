package dev.binclub.bingait.plugins.krakatau

import dev.binclub.bingait.api.util.wait
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * @author cook 18/Nov/2020
 */
object PythonManager {
	val krakatauFolder = kotlin.run {
		// extract krakatau
		val zip = ZipInputStream(this::class.java.getResourceAsStream("/Krakatau-master.zip"))
		
		val marker = File.createTempFile("bingait_krakatau_marker", "")
		val outFolder = File(marker.parentFile, "bingait_krakatau")
		outFolder.mkdirs()
		
		var entry: ZipEntry? = zip.nextEntry
		while (entry != null) {
			if (!entry.isDirectory) {
				// lets hope storyyeller doesn't decide to do some path traversal
				val outFile = File(outFolder, entry.name)
				outFile.parentFile.mkdirs()
				outFile.createNewFile()
				outFile.outputStream().buffered().use(zip::copyTo)
			}
			
			entry = zip.nextEntry
		}
		
		outFolder
	}
	
	private fun toPythonList(list: List<String>): String {
		return "[${list.joinToString(", ", transform = { "\"$it\"" })}]"
	}
	
	fun getPythonVersion(path: String): String? {
		return try {
			val p = ProcessBuilder(path, "--version")
				.start()
			
			val err = p.errorStream
				.readBytes()
				.toString(Charsets.UTF_8)
				.trim()
			
			val out = p.inputStream
				.readBytes()
				.toString(Charsets.UTF_8)
				.trim()
			
			out + err
		} catch (t: Throwable) {
			t.printStackTrace()
			null
		}
	}
	
	fun Boolean.toPythonStr(): String = if (this) "True" else "False"
	
	fun decompileClass(
		pythonPath: String,
		path: List<String>,
		targets: List<String>,
		outpath: String,
		skip_errors: Boolean = false,
		add_throws: Boolean = false,
		magic_throw: Boolean = false
	) {
		val version = getPythonVersion(pythonPath)
		if (version?.startsWith("Python 2.") != true) {
			error("Invalid Python Version '$version'. Please provide a valid Python 2 executable.")
		}
		
		val code = """
			import decompile
			path = ${toPythonList(path)}
			targets = ${toPythonList(targets)}
			decompile.decompileClass(path, targets, "$outpath", ${skip_errors.toPythonStr()}, ${add_throws.toPythonStr()}, ${magic_throw.toPythonStr()})
		""".trimIndent()
		val codeFile = File(krakatauFolder, "Krakatau-master/bingait_krakatau_script.py")
		codeFile.writeText(code)
		
		if (pythonPath.isBlank() || !File(pythonPath).exists()) {
			error("Invalid python path")
		}
		
		val pb = ProcessBuilder(pythonPath, codeFile.absolutePath)
		val proc = pb.start()
		proc.waitFor()
	}
}
