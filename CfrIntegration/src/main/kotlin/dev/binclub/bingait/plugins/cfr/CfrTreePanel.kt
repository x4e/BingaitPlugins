package dev.binclub.bingait.plugins.cfr

import dev.binclub.bingait.api.util.tree.BinJTree
import dev.binclub.bingait.api.util.BinTreeNode
import dev.binclub.bingait.api.util.BinTreeRenderer
import dev.binclub.bingait.api.util.BinTreeText
import dev.binclub.bingait.api.util.readBytes
import dev.binclub.bingait.api.util.cast
import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.api.ClassFileSource
import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.OutputSinkFactory.SinkClass.TOKEN_STREAM
import org.benf.cfr.reader.api.OutputSinkFactory.SinkType.JAVA
import org.benf.cfr.reader.api.SinkReturns
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair
import java.awt.Color
import java.awt.GridLayout
import java.io.DataInput
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Stack
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Show CFR output in a tree structure
 *
 * For now use curly brackets to detect tree indentation, hopefully https://github.com/leibnitz27/cfr/issues/208
 * will improve this situation
 *
 * @author cook 08/Sep/2020
 */
class CfrTreePanel(
	val classFileName: String,
	val byteProvider: () -> DataInput,
	val classPathProvider: (String) -> DataInput?
): JPanel() {
	
	val keyWords = hashSetOf(
		"abstract",
		"continue",
		"for",
		"new",
		"switch",
		"assert",
		"default",
		"goto",
		"package",
		"synchronized",
		"boolean",
		"do",
		"if",
		"private",
		"this",
		"break",
		"double",
		"implements",
		"protected",
		"throw",
		"byte",
		"else",
		"import",
		"public",
		"throws",
		"case",
		"enum",
		"instanceof",
		"return",
		"transient",
		"catch",
		"extends",
		"int",
		"short",
		"try",
		"char",
		"final",
		"interface",
		"static",
		"void",
		"class",
		"finally",
		"long",
		"strictfp",
		"volatile",
		"const",
		"float",
		"native",
		"super",
		"while"
	)
	
	init {
		layout = GridLayout()
		
		try {
			val tree = BinJTree()
			add(JScrollPane(tree))
			tree.cellRenderer = BinTreeRenderer()
			tree.model = DefaultTreeModel(DefaultMutableTreeNode())
			val root = tree.model.cast<DefaultTreeModel>().root.cast<DefaultMutableTreeNode>()
			
			tree.isRootVisible = true
			tree.showsRootHandles = true
			
			val tokens = Stack<DefaultMutableTreeNode>()
			tokens.push(root)
			val currentLine = ArrayList<BinTreeText>()
			fun ArrayList<BinTreeText>.isNotBlank() = this.isNotEmpty() && this.any { it.text.isNotBlank() }
			fun ArrayList<BinTreeText>.copy() = this.clone().cast<ArrayList<BinTreeText>>()
			
			val sink = object: OutputSinkFactory {
				override fun <T: Any?> getSink(
					sinkType: OutputSinkFactory.SinkType,
					sinkClass: OutputSinkFactory.SinkClass
				): OutputSinkFactory.Sink<T> = OutputSinkFactory.Sink<T> {
					if (sinkType == JAVA) {
						it as SinkReturns.Token
						
						when (it.tokenType) {
							SinkReturns.TokenType.SEPARATOR -> {
								when (val txt = it.text) {
									"{" -> {
										val newLine = BinTreeNode(currentLine.copy())
										tokens.peek().add(newLine)
										tokens.push(newLine)
										currentLine.clear()
									}
									"}" -> {
										if (currentLine.isNotBlank()) {
											tokens.peek().add(BinTreeNode(currentLine.copy()))
										}
										tokens.pop()
										currentLine.clear()
									}
									else -> currentLine += BinTreeText(txt)
								}
							}
							SinkReturns.TokenType.COMMENT -> {
								when (val txt = it.text) {
									"/* " -> {
										val newLine = BinTreeNode(currentLine.copy())
										tokens.peek().add(newLine)
										tokens.push(newLine)
										currentLine.clear()
									}
									" */" -> {
										if (currentLine.isNotBlank()) {
											tokens.peek().add(BinTreeNode(currentLine.copy()))
										}
										tokens.pop()
										currentLine.clear()
									}
									else -> currentLine += BinTreeText(txt)
								}
							}
							SinkReturns.TokenType.NEWLINE, SinkReturns.TokenType.EOF -> {
								if (currentLine.isNotBlank()) {
									tokens.peek().add(BinTreeNode(currentLine.copy()))
								}
								currentLine.clear()
							}
							SinkReturns.TokenType.KEYWORD -> {
								currentLine += BinTreeText(it.text, Color(0xB092EA))
							}
							SinkReturns.TokenType.IDENTIFIER -> {
								currentLine += if (it.text == "this") {
									BinTreeText(it.text, Color(0xB092EA))
								} else {
									BinTreeText(it.text)
								}
							}
							SinkReturns.TokenType.LITERAL -> {
								currentLine += BinTreeText(it.text, Color(0xBDE88D))
							}
							SinkReturns.TokenType.OPERATOR -> {
								currentLine += BinTreeText(it.text, Color(0x89DDFF))
							}
							SinkReturns.TokenType.FIELD, SinkReturns.TokenType.METHOD -> {
								currentLine += BinTreeText(it.text, Color(0x62A9C6))
							}
							else -> {
								currentLine += if (it.text != null && it.text.trim() in keyWords) {
									BinTreeText(it.text, Color(0xB092EA))
								} else {
									BinTreeText(it.text)
								}
							}
						}
					}
				}
				
				override fun getSupportedSinks(
					sinkType: OutputSinkFactory.SinkType?,
					available: MutableCollection<OutputSinkFactory.SinkClass>?
				): MutableList<OutputSinkFactory.SinkClass> = mutableListOf(TOKEN_STREAM)
			}
			val source = object: ClassFileSource {
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
						return Pair(ClassLoader.getSystemClassLoader().getResourceAsStream(path)!!.readBytes(), path)
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
			
			tree.expandRow(0)
		} catch (t: Throwable) {
			t.printStackTrace()
			
			val sw = StringWriter()
			t.printStackTrace(PrintWriter(sw))
			
			val text = JTextArea()
			add(JScrollPane(text))
			
			text.isEditable = false
			text.text = sw.toString()
		}
	}
}
