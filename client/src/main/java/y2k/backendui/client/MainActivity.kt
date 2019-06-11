package y2k.backendui.client

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import y2k.virtual.ui.VirtualHostView
import y2k.virtual.ui.VirtualNode
import y2k.virtual.ui.frameLayout
import y2k.virtual.ui.mkNode
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.ObjectInputStream
import java.net.URL
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var virtualHostView: VirtualHostView
    private lateinit var server: Closeable

//    private val runtime = TeaRuntime(
//        TodoList, this,
//        { f -> GlobalScope.launch(Dispatchers.Main) { f() } },
//        true
//    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        virtualHostView = VirtualHostView(this)
        setContentView(virtualHostView)
    }

    override fun onStart() {
        super.onStart()

        val task = thread {
            val bytes = URL("http://10.5.101.184:8080/").readBytes()

            val node =
                ByteArrayInputStream(bytes)
                    .let(::ObjectInputStream)
                    .readObject() as VirtualNode

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

        server = Closeable {
            task.interrupt()
        }
    }

    override fun onStop() {
        super.onStop()
        server.close()
    }
}
