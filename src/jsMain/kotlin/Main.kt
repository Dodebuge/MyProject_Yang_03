import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import kotlin.math.PI
import kotlin.math.roundToInt

const val CELL = 36.0
const val SIZE = CELL * (N + 1)

val board = IntArray(N * N)
var turn = 1
var last = -1
var over = false

val canvas = document.getElementById("board") as HTMLCanvasElement
val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
val status = document.getElementById("status")!!

fun pos(i: Int) = CELL * (i + 1)

fun draw() {
    ctx.fillStyle = "#dcb35c"
    ctx.fillRect(0.0, 0.0, SIZE, SIZE)
    ctx.strokeStyle = "#333"
    ctx.lineWidth = 1.0
    for (i in 0 until N) {
        ctx.beginPath()
        ctx.moveTo(pos(0), pos(i)); ctx.lineTo(pos(N - 1), pos(i))
        ctx.moveTo(pos(i), pos(0)); ctx.lineTo(pos(i), pos(N - 1))
        ctx.stroke()
    }
    ctx.fillStyle = "#333"
    for (sx in listOf(3, 7, 11)) for (sy in listOf(3, 7, 11)) {
        ctx.beginPath(); ctx.arc(pos(sx), pos(sy), 3.5, 0.0, 2 * PI); ctx.fill()
    }
    for (i in board.indices) {
        if (board[i] == 0) continue
        ctx.beginPath()
        ctx.arc(pos(i % N), pos(i / N), CELL * 0.45, 0.0, 2 * PI)
        ctx.fillStyle = if (board[i] == 1) "#111" else "#f8f8f8"
        ctx.fill()
        ctx.strokeStyle = "#000"; ctx.stroke()
    }
    if (last >= 0) {
        ctx.fillStyle = "#e33"
        ctx.beginPath(); ctx.arc(pos(last % N), pos(last / N), 4.0, 0.0, 2 * PI); ctx.fill()
    }
    val name = if (turn == 1) "흑" else "백"
    status.textContent = if (over) "$name 승리! 🎉" else "$name 차례"
}

fun reset() {
    board.fill(0); turn = 1; last = -1; over = false
    draw()
}

fun main() {
    canvas.width = SIZE.toInt()
    canvas.height = SIZE.toInt()
    canvas.addEventListener("click", { e ->
        e as MouseEvent
        if (over) return@addEventListener
        val rect = canvas.getBoundingClientRect()
        val scale = SIZE / rect.width
        val x = ((e.clientX - rect.left) * scale / CELL).roundToInt() - 1
        val y = ((e.clientY - rect.top) * scale / CELL).roundToInt() - 1
        if (x !in 0 until N || y !in 0 until N || board[y * N + x] != 0) return@addEventListener
        board[y * N + x] = turn
        last = y * N + x
        if (isWin(board, x, y)) over = true else turn = 3 - turn
        draw()
    })
    document.getElementById("reset")!!.addEventListener("click", { reset() })
    draw()
}
