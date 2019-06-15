package y2k.backendui.client

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okio.ByteString
import y2k.virtual.ui.*
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.util.concurrent.CountDownLatch

class MainActivity : AppCompatActivity() {

    private lateinit var virtualHostView: VirtualHostView

    private lateinit var socket: WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        virtualHostView = VirtualHostView(this)
        setContentView(virtualHostView)
    }

    override fun onStart() {
        super.onStart()
        startSocket()
    }

    private fun startSocket() {
        val request = Request.Builder()
            .url("ws://192.168.0.100:8080/socket")
            .build()

        socket = OkHttpClient.Builder()
            .build()
            .newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(":CONNECT:")
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    bytes
                        .toByteArray()
                        .let(::ByteArrayInputStream)
                        .let(::ObjectInputStream)
                        .readObject()
                        .let { it as VirtualNode }
                        .let { attachClickListeners(it, webSocket::send) }
                        .let(::sendNodeToUI)
                }
            })
    }

    private fun attachClickListeners(node: VirtualNode, f: (String) -> Any?): VirtualNode {
        if (node !is View_) return node

        val msgProp = node.props.find { it.propId == 17 }

        if (msgProp != null) {
            val p = Property(52, View.OnClickListener { f(msgProp.value as String) })
            node.props += p

            node.onClickListener = View.OnClickListener {
                f(msgProp.value as String)
            }
        }

        node.props.removeAll { it.propId == 17 }

        node.children.forEach {
            attachClickListeners(it, f)
        }

        return node
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

    override fun onStop() {
        super.onStop()
        socket.cancel()
    }
}
