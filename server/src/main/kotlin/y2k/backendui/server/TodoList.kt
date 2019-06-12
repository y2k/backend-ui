package y2k.backendui.server

import y2k.backendui.server.TodoList.Model
import y2k.backendui.server.TodoList.Msg
import y2k.tea.Cmd
import y2k.tea.Sub
import y2k.tea.TeaComponent
import y2k.virtual.ui.button
import y2k.virtual.ui.linearLayout
import y2k.virtual.ui.textView
import java.io.Serializable

object TodoList : TeaComponent<Model, Msg> {

    data class Model(val items: List<String> = emptyList(), val newItem: String = "") : Serializable
    sealed class Msg : Serializable {
        class NewItemTextChanged(val text: String) : Msg()
        class TodoChanged(val items: List<String>) : Msg()
        class RemoveClicked(val text: String) : Msg()
        class AddClicked : Msg()
        class TodoAddResult(val result: Either<Exception, Unit>) : Msg() }

    override fun initialize() = Model() to Cmd.none<Msg>()

    override fun update(model: Model, msg: Msg) = when (msg) {
        is Msg.AddClicked -> model.copy(newItem = "", items = model.items + "Item #?") to Cmd.none()
        is Msg.RemoveClicked -> model.copy(items = model.items.filter { it != msg.text }) to Cmd.none()
        is Msg.TodoChanged -> model.copy(items = msg.items) to Cmd.none()
        is Msg.NewItemTextChanged -> model.copy(newItem = msg.text) to Cmd.none()
        is Msg.TodoAddResult -> when (msg.result) {
            is Either.Left -> model to Effects.showToast("Не удалось добавить элемент")
            is Either.Right -> model to Cmd.none() } }

    override fun sub() = Sub.empty<Msg>()

    fun UiContext.view(model: Model, dispatch: (Msg) -> Unit) =
        linearLayout {
            padding = pad(all = 8)
            orientation = LinearLayout.VERTICAL
            nodes = {
                textField(
                    text = model.newItem,
                    onTextChanged = { dispatch(Msg.NewItemTextChanged(it)) }) {
                    singleLine = true
                    hintCharSequence = "Enter text..." }
                button {
                    textCharSequence = "Add"
                    onClickMsg = Msg.AddClicked() }
                staticListView(model.items) {
                    itemView(it) } } }

    private fun UiContext.itemView(item: String) =
        linearLayout {
            nodes = {
                textView {
                    //                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    padding = pad(vertical = 20)
                    textSizeFloat = 16f
                    textCharSequence = item }
                button {
                    textCharSequence = "Delete"
                    onClickMsg = Msg.RemoveClicked(item) } } } }
