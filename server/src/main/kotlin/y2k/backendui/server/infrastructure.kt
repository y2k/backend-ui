package y2k.backendui.server

import io.undertow.Handlers.path
import io.undertow.Handlers.websocket
import io.undertow.Undertow
import io.undertow.websockets.core.AbstractReceiveListener
import io.undertow.websockets.core.BufferedTextMessage
import io.undertow.websockets.core.WebSocketChannel
import io.undertow.websockets.core.WebSockets
import y2k.backendui.server.TodoList.Model
import y2k.backendui.server.TodoList.Msg
import y2k.tea.Cmd
import y2k.virtual.ui.*
import y2k.virtual.ui.common.editableView
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object Effects {
    fun showToast(s: String): Cmd<Nothing> = TODO()
}

object LinearLayout {
    const val HORIZONTAL = 0
    const val VERTICAL = 1
}

fun <Model> staticListView(items: List<Model>, itemView: (item: Model) -> VirtualNode) {
    scrollView {
        verticalScrollBarEnabled = false
        nodes = {
            linearLayout {
                orientation = LinearLayout.VERTICAL
                nodes = {
                    items.forEach { item ->
                        itemView(item)
                    }
                }
            }
        }
    }
}

sealed class Either<out L, out R> : Serializable {
    class Left<T>(val value: T) : Either<T, Nothing>()
    class Right<T>(val value: T) : Either<Nothing, T>()
}

class UiContext(val density: Float) : Serializable

fun UiContext.pad(
    all: Int? = null,
    start: Int? = null,
    end: Int? = null,
    top: Int? = null,
    bottom: Int? = null,
    horizontal: Int? = null,
    vertical: Int? = null
): Quadruple<Int, Int, Int, Int> = Quadruple(
    (density * (start ?: horizontal ?: all ?: 0)).toInt(),
    (density * (top ?: horizontal ?: all ?: 0)).toInt(),
    (density * (end ?: vertical ?: all ?: 0)).toInt(),
    (density * (start ?: vertical ?: all ?: 0)).toInt()
)

fun textField(
    text: String,
    onTextChanged: (String) -> Unit,
    config: EditText_.() -> Unit
) {
    editableView {
        //        this.onTextChanged = { onTextChanged(it.toString()) }
        this.text = text
        nodes = {
            editText(config)
        }
    }
}

inline var ViewGroup_.nodes: () -> Unit
    get() = error("")
    set(value) {
        value()
    }

inline var View_.onClickMsg: Msg
    get() = error("")
    set(value) {
        contentDescription = MsgPacker.serialize(value)
    }

object MsgPacker {

    fun deserialize(messageText: ByteArray): Msg =
        Base64.getDecoder()
            .decode(messageText)
            .let(::ByteArrayInputStream)
            .let(::ObjectInputStream)
            .readObject() as Msg

    fun deserialize(messageText: String): Msg =
        Base64.getDecoder()
            .decode(messageText)
            .let(::ByteArrayInputStream)
            .let(::ObjectInputStream)
            .readObject() as Msg

    fun serialize(msg: Msg): String {
        val s = ByteArrayOutputStream()
        ObjectOutputStream(s).writeObject(msg)
        return Base64.getEncoder().encodeToString(s.toByteArray())
    }
}

object App {

    private val modelRef = AtomicReference<Model>(
        Model(items = List(2) { "Item #$it" })
    )

    @JvmStatic
    fun main(args: Array<String>) {
        Undertow.builder()
            .addHttpListener(8080, "0.0.0.0")
            .setHandler(path().addPrefixPath("/socket", websocket { _, channel ->
                channel.receiveSetter.set(object : AbstractReceiveListener() {

                    override fun onFullTextMessage(channel: WebSocketChannel?, message: BufferedTextMessage) {
                        val msg = message.data
                        if (msg == ":CONNECT:") {
                            handle(null) {
                                WebSockets.sendBinary(ByteBuffer.wrap(it), channel, null)
                            }
                        } else {
                            handle(msg) {
                                WebSockets.sendBinary(ByteBuffer.wrap(it), channel, null)
                            }
                        }
                    }
                })
                channel.resumeReceives()
            }))
            .build()
            .start()
        Thread.sleep(Long.MAX_VALUE)
    }

    private fun handle(serMsg: String?, send: (ByteArray) -> Unit) {

        if (serMsg != null) {
            val msg = MsgPacker.deserialize(serMsg)

            val (newModel, _) = TodoList.update(modelRef.get(), msg)
            modelRef.set(newModel)
        }

        try {
            val bytes = getPageBytes()
            send(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun getPageBytes(): ByteArray {
        val node: VirtualNode =
            with(TodoList) {
                UiContext(2f).view(modelRef.get()) {}
            }

        val data = ByteArrayOutputStream()
        ObjectOutputStream(data).writeObject(node)
        return data.toByteArray()
    }
}
