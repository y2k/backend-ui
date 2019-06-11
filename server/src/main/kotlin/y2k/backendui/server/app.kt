package y2k.backendui.server

import io.undertow.Undertow
import io.undertow.util.Headers
import y2k.backendui.server.TodoList.Model
import y2k.backendui.server.TodoList.Msg
import y2k.tea.Cmd
import y2k.tea.Sub
import y2k.tea.TeaComponent
import y2k.tea.map
import y2k.virtual.ui.*
import y2k.virtual.ui.common.editableView
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.ByteBuffer


object Effects {
    fun showToast(s: String): Cmd<Nothing> = TODO()
    fun remove(s: String, any: (Any) -> Unit): Cmd<Nothing> = TODO()
    fun add(s: String, mapOf: Map<String, String>): Cmd<Nothing> = TODO()
}

object LinearLayout {
    const val HORIZONTAL = 0
    const val VERTICAL = 1
}

object TodoList : TeaComponent<Model, Msg> {

    data class Model(val items: List<String> = emptyList(), val newItem: String = "") : Serializable
    sealed class Msg {
        class NewItemTextChanged(val text: String) : Msg()
        class TodoChanged(val items: List<String>) : Msg()
        class RemoveClicked(val text: String) : Msg()
        object AddClicked : Msg()
        class TodoAddResult(val result: Either<Exception, Unit>) : Msg()
    }

    override fun initialize() = Model() to Cmd.none<Msg>()

    override fun update(model: Model, msg: Msg) = when (msg) {
        Msg.AddClicked -> model.copy(newItem = "") to
                Effects.add("todo-list", mapOf("text" to model.newItem)).map { Msg.TodoAddResult(it) }
        is Msg.RemoveClicked -> model to Effects.remove("todo-list") {
            //            it.whereEqualTo("text", msg.text)
        }
        is Msg.TodoChanged -> model.copy(items = msg.items) to Cmd.none()
        is Msg.NewItemTextChanged -> model.copy(newItem = msg.text) to Cmd.none()
        is Msg.TodoAddResult -> when (msg.result) {
            is Either.Left -> model to Effects.showToast("Не удалось добавить элемент")
            is Either.Right -> model to Cmd.none()
        }
    }

    override fun sub() = Sub.empty<Msg>()
//        Effects.subscribeCollections("todo-list")
//            .map { list -> Msg.TodoChanged(list.map { it.getString("text")!! }) }

    @JvmStatic
    fun UiContext.view(model: Model, dispatch: (Msg) -> Unit) =
        linearLayout {
            padding = pad(all = 8)
            orientation = LinearLayout.VERTICAL
            nodes = {
                textField(
                    text = model.newItem,
                    onTextChanged = { dispatch(Msg.NewItemTextChanged(it)) }) {
                    singleLine = true
                    hintCharSequence = "Enter text..."
                }
                button {
                    //                    onClickListener = View.OnClickListener { dispatch(Msg.AddClicked) }
                    textCharSequence = "Add"
                }
                staticListView(model.items) {
                    itemView(it, dispatch)
                }
            }
        }

    private fun UiContext.itemView(item: String, dispatch: (Msg) -> Unit) =
        linearLayout {
            nodes = {
                textView {
                    //                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    padding = pad(vertical = 20)
                    textSizeFloat = 16f
                    textCharSequence = item
                }
                button {
                    //                    onClickListener = View.OnClickListener { dispatch(Msg.RemoveClicked(item)) }
                    textCharSequence = "Delete"
                }
            }
        }
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

sealed class Either<out L, out R> {
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

object App {

    @JvmStatic
    fun main(args: Array<String>) {

        Undertow.builder()
            .addHttpListener(8080, "0.0.0.0")
            .setHandler { exchange ->
                exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/octet-stream")
                try {
                    val bytes = getPageBytes()
                    exchange.responseSender.send(ByteBuffer.wrap(bytes))
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
            .build()
            .start()

//        Undertow
//            .builder()
//            .addHttpListener(8080, "0.0.0.0")
//            .setHandler { exchange ->
//
//                val bytes = getPageBytes()
//
//                exchange.requestHeaders
//
//                exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/octet-stream")
//                exchange.responseHeaders.put(Headers.CONTENT_LENGTH, bytes.size.toLong())
////                exchange.responseSender.send(ByteBuffer.wrap(bytes))
//
//                exchange.outputStream.write(bytes)
//
//            }
//            .build()
//            .start()
    }

    private fun getPageBytes(): ByteArray {
        val node: VirtualNode =
            with(TodoList) {
                UiContext(2f).view(
                    Model(
                        items = List(20) { "Item #$it" },
                        newItem = "XXX"
                    ),
                    {}
                )
            }

        val data = ByteArrayOutputStream()
        ObjectOutputStream(data).writeObject(node)
        val bytes = data.toByteArray()
        return bytes
    }
}