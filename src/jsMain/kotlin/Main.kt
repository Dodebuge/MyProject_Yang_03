import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Worker
import org.w3c.dom.events.MouseEvent
import kotlin.js.json
import kotlin.math.PI
import kotlin.math.roundToInt

const val CELL = 36.0
const val SIZE = CELL * (N + 1)
const val AI = 2 // AI는 백

val board = IntArray(N * N)
var turn = 1
var last = -1
var over = false

val canvas = document.getElementById("board") as HTMLCanvasElement
val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
val status = document.getElementById("status")!!
val aiToggle = document.getElementById("ai") as HTMLInputElement

// AI는 별도 워커에서 실행. 같은 프로토콜을 따르는 다른 워커(C++/Python 등)로 파일명만 바꿔 교체할 수 있다
val ai = Worker("ai.js")
var requestId = 0 // 다시 시작하면 증가시켜, 이전 판에 대한 늦은 응답을 버린다

fun pos(i: Int) = CELL * (i + 1)

fun aiTurn() = aiToggle.checked && turn == AI && !over

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
    status.textContent = when {
        over -> "$name 승리! 🎉"
        aiTurn() -> "AI 생각 중…"
        else -> "$name 차례"
    }
}

fun askAi() {
    if (aiTurn()) ai.postMessage(json("id" to ++requestId, "board" to board, "me" to AI))
}

fun play(i: Int) {
    board[i] = turn
    last = i
    if (isWin(board, i % N, i / N)) over = true else turn = 3 - turn
    draw()
    askAi()
}

fun reset() {
    board.fill(0); turn = 1; last = -1; over = false
    requestId++
    draw()
}

fun main() {
    canvas.width = SIZE.toInt()
    canvas.height = SIZE.toInt()
    canvas.addEventListener("click", { e ->
        e as MouseEvent
        if (over || aiTurn()) return@addEventListener
        val rect = canvas.getBoundingClientRect()
        val scale = SIZE / rect.width
        val x = ((e.clientX - rect.left) * scale / CELL).roundToInt() - 1
        val y = ((e.clientY - rect.top) * scale / CELL).roundToInt() - 1
        if (x !in 0 until N || y !in 0 until N || board[y * N + x] != 0) return@addEventListener
        play(y * N + x)
    })
    ai.onmessage = { e ->
        val res = e.data.asDynamic()
        val move = res.move as Int
        if (res.id == requestId && aiTurn() && move in board.indices && board[move] == 0) play(move)
    }
    aiToggle.addEventListener("change", { draw(); askAi() })
    document.getElementById("reset")!!.addEventListener("click", { reset() })
    draw()
}
