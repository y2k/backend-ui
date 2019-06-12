package y2k.backendui.client

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import y2k.virtual.ui.*
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var virtualHostView: VirtualHostView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        virtualHostView = VirtualHostView(this)
        setContentView(virtualHostView)
    }

    override fun onStart() {
        super.onStart()

        fun reloadNode(msg: String?) {
            thread {
                val node = attachClickListeners(downloadNode(msg), ::reloadNode)
                sendNodeToUI(node)
            }
        }

        reloadNode(null)
    }

    private fun attachClickListeners(node: VirtualNode, f: (String) -> Unit): VirtualNode {
        if (node !is View_) return node

        val msgProp = node.props.find { it.name == "contentDescription" }

        if (msgProp != null) {

            val p = Property<View.OnClickListener?, View>(false, "onClickListener", null, View::setOnClickListener)

            val f = generateSequence<Class<*>>(node.javaClass, { it.superclass })
                .first { it.simpleName == "View_" }
                .getDeclaredField("_onClickListener")
            f.isAccessible = true
            f.set(node, p)

            node.onClickListener = View.OnClickListener {
                f(msgProp.value as String)
            }
        }

        node.props.removeAll { it.name == "contentDescription" }

        node.children.forEach {
            attachClickListeners(it, f)
        }

        return node
    }

    private fun downloadNode(msg: String?): VirtualNode {
        val url = URL("http://192.168.0.100:8080/")

        val conn = url.openConnection() as HttpURLConnection

        if (msg != null)
            conn.addRequestProperty("RemoteUI-Msg", msg)

        return conn.inputStream
            .readBytes()
            .let(::ByteArrayInputStream)
            .let(::ObjectInputStream)
            .readObject() as VirtualNode
    }

    private fun sendNodeToUI(node: VirtualNode) {
        val cdl = CountDownLatch(1)
        runOnUiThread {

            virtualHostView.update(mkNode {
                frameLayout {
                    children += node
                }
            })

            cdl.countDown()
        }
        cdl.await()
    }
}
