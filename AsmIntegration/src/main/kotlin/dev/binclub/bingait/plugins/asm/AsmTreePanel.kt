package dev.binclub.bingait.plugins.asm

import dev.binclub.bingait.api.BingaitThreadpool
import dev.binclub.bingait.api.util.BinTreeRenderer
import dev.binclub.bingait.api.util.cast
import dev.binclub.bingait.api.util.readBytes
import dev.binclub.bingait.api.util.tree.BinJTree
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.awt.GridLayout
import java.io.DataInput
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * @author cook 19/Nov/2020
 */
class AsmTreePanel(
	treeItem: Any,
	classFileName: String,
	byteProvider: () -> DataInput,
	val classPathProvider: (String) -> DataInput?
): JPanel() {
	init {
		layout = GridLayout()
		
		BingaitThreadpool.submitTask("Dissasembling with ASM") {
			try {
				val tree = BinJTree()
				add(JScrollPane(tree))
				tree.cellRenderer = BinTreeRenderer()
				tree.model = DefaultTreeModel(DefaultMutableTreeNode())
				val root = tree.model.cast<DefaultTreeModel>().root.cast<DefaultMutableTreeNode>()
				
				tree.showsRootHandles = true
				tree.isRootVisible = false
				
				val cn = ClassNode()
				val cr = ClassReader(byteProvider().readBytes())
				cr.accept(cn, ClassReader.SKIP_FRAMES)
				
				classTree(root, cn)
				
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
	
	fun classTree(root: DefaultMutableTreeNode, cn: ClassNode) {
		root.add(DefaultMutableTreeNode("Name ${cn.name}"))
		root.add(DefaultMutableTreeNode("Signature ${cn.signature}"))
		root.add(DefaultMutableTreeNode("SuperClass ${cn.superName}"))
		val interfaces = DefaultMutableTreeNode("Interfaces")
		cn.interfaces?.forEach {
			interfaces.add(DefaultMutableTreeNode(it))
		}
		root.add(interfaces)
		root.add(DefaultMutableTreeNode("SourceFile ${cn.sourceFile}"))
		root.add(DefaultMutableTreeNode("SourceDebug ${cn.sourceDebug}"))
		root.add(DefaultMutableTreeNode("Module ${cn.module}"))
		root.add(DefaultMutableTreeNode("OuterClass ${cn.outerClass}"))
		root.add(DefaultMutableTreeNode("OuterMethod ${cn.module}"))
		root.add(DefaultMutableTreeNode("OuterMethodDesc ${cn.outerMethodDesc}"))
		val visibleAnnotations = DefaultMutableTreeNode("VisibleAnnotations")
		cn.visibleAnnotations?.forEach {
			visibleAnnotations.add(DefaultMutableTreeNode(it))
		}
		root.add(visibleAnnotations)
		val invisibleAnnotations = DefaultMutableTreeNode("InvisibleAnnotations")
		cn.visibleAnnotations?.forEach {
			invisibleAnnotations.add(DefaultMutableTreeNode(it))
		}
		root.add(invisibleAnnotations)
		val visibleTypeAnnotations = DefaultMutableTreeNode("VisibleTypeAnnotations")
		cn.visibleTypeAnnotations?.forEach {
			visibleTypeAnnotations.add(DefaultMutableTreeNode(it))
		}
		root.add(visibleTypeAnnotations)
		val invisibleTypeAnnotations = DefaultMutableTreeNode("InvisibleTypeAnnotations")
		cn.invisibleTypeAnnotations?.forEach {
			invisibleTypeAnnotations.add(DefaultMutableTreeNode(it))
		}
		root.add(invisibleTypeAnnotations)
		val attributes = DefaultMutableTreeNode("Attributes")
		cn.attrs?.forEach {
			attributes.add(DefaultMutableTreeNode(it.type))
		}
		root.add(attributes)
		val innerClasses = DefaultMutableTreeNode("InnerClasses")
		cn.innerClasses?.forEachIndexed { index, innerClassNode ->
			val inner = DefaultMutableTreeNode(index)
			inner
			innerClasses.add(inner)
		}
		root.add(innerClasses)
	}
}
