package dev.binclub.bingait.plugins.cfr

import dev.binclub.bingait.api.BingaitThreadpool
import dev.binclub.bingait.api.util.readBytes
import dev.binclub.bingait.api.util.removeBingait
import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.api.ClassFileSource
import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.OutputSinkFactory.SinkClass.EXCEPTION_MESSAGE
import org.benf.cfr.reader.api.OutputSinkFactory.SinkClass.STRING
import org.benf.cfr.reader.api.OutputSinkFactory.SinkType.EXCEPTION
import org.benf.cfr.reader.api.OutputSinkFactory.SinkType.JAVA
import org.benf.cfr.reader.api.SinkReturns.ExceptionMessage
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.GridLayout
import java.io.DataInput
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.StringBuilder
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

/**
 * Simply print CFR output to a text box
 *
 * @author cook 07/Sep/2020
 */
class CfrResourcePanel(
	val classFileName: String,
	val byteProvider: () -> DataInput,
	val classPathProvider: (String) -> DataInput?
): JPanel() {
	init {
		layout = GridLayout()
		
		val text = RSyntaxTextArea(20, 60)
		text.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA
		text.isCodeFoldingEnabled = true
		val sp = RTextScrollPane(text)
		add(sp)
		
		text.isEditable = false
		text.text = "Please Wait..."
		BingaitThreadpool.submitTask("Decompiling with CFR") {
			val excep = StringBuilder()
			val decompiled = try {
				val sb = StringBuilder()
				val sink = object : OutputSinkFactory {
					override fun <T : Any?> getSink(
						sinkType: OutputSinkFactory.SinkType,
						sinkClass: OutputSinkFactory.SinkClass
					): OutputSinkFactory.Sink<T> =
						OutputSinkFactory.Sink {
							if (sinkType == EXCEPTION) {
								if (it is ExceptionMessage) {
									excep.append("Path: ${it.path}\n")
									excep.append("Message: ${it.message}\n")
									val writer = StringWriter()
									it.thrownException.removeBingait()
									it.thrownException.printStackTrace(PrintWriter(writer))
									excep.append(it.thrownException.stackTraceToString())
								} else {
									excep.append(it.toString())
								}
							} else if (sinkType == JAVA) {
								sb.append(it)
							} else {
								// maybe do something with it?
							}
						}
					
					override fun getSupportedSinks(
						sinkType: OutputSinkFactory.SinkType?,
						available: MutableCollection<OutputSinkFactory.SinkClass>?
					): MutableList<OutputSinkFactory.SinkClass> =
						if (sinkType == JAVA) mutableListOf(STRING) else mutableListOf(EXCEPTION_MESSAGE)
				}
				val source = object : ClassFileSource {
					override fun getPossiblyRenamedPath(path: String): String = path
					
					override fun getClassFileContent(path: String): Pair<ByteArray, String> {
						try {
							if (path == classFileName) {
								return Pair(byteProvider().readBytes(), path)
							}
						} catch (t: Throwable) {
						}
						
						try {
							path.removeSuffix("/").let { path ->
								classPathProvider(path)?.let {
									return Pair(it.readBytes(), path)
								}
							}
						} catch (t: Throwable) {
						}
						
						try {
							return Pair(
								ClassLoader.getSystemClassLoader().getResourceAsStream(path)!!.readBytes(),
								path
							)
						} catch (t: Throwable) {
						}
						throw IOException("$path not found")
					}
					
					override fun addJar(jarPath: String?): MutableCollection<String> {
						error("Not Supported")
					}
					
					override fun informAnalysisRelativePathDetail(usePath: String?, classFilePath: String?) {}
				}
				val driver = CfrDriver.Builder()
					.withOutputSink(sink)
					.withClassFileSource(source)
					.build()
				
				driver.analyse(mutableListOf(classFileName))
				
				sb.toString()
			} catch (t: Throwable) {
				t.printStackTrace()
				t.stackTraceToString()
			}
			if (decompiled.isBlank()) {
				text.text = excep.toString()
			} else {
				text.text = decompiled
			}
		}
	}
}
