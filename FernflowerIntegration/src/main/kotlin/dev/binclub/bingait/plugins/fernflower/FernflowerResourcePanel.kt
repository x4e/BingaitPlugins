package dev.binclub.bingait.plugins.fernflower

import dev.binclub.bingait.api.BingaitThreadpool
import dev.binclub.bingait.api.util.readBytes
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.GridLayout
import java.io.DataInput
import javax.swing.JPanel

/**
 * @author cook 16/Nov/2020
 */
class FernflowerResourcePanel(
	classFileName: String,
	byteProvider: () -> DataInput,
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
		BingaitThreadpool.submitTask("Decompiling with Fernflower") {
			try {
				text.text = FernflowerIntegration.decompile(classFileName, byteProvider().readBytes(), classPathProvider)
			} catch (t: Throwable) {
				if (text.text.isBlank()) {
					text.text = t.stackTraceToString()
				}
			}
		}
	}
}
