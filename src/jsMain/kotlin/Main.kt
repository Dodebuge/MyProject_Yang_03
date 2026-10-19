import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.Worker
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.js.Date
import kotlin.js.json
import kotlin.math.PI
import kotlin.math.roundToInt

const val CELL = 36.0
const val SIZE = CELL * (N + 1)
const val AI = 2 // AI는 백
const val RECORDS_KEY = "gomoku.records"

val board = IntArray(N * N)
val moves = mutableListOf<Int>() // 기보: 흑부터 번갈아 둔 board 인덱스
var turn = 1
var last = -1
var over = false

// 리플레이 상태. replay가 null이 아니면 리플레이 중
var replay: IntArray? = null
var step = 0
var review: Array<dynamic>? = null // 워커 분석 결과 [{best, loss}], 도착 전엔 null

val canvas = document.getElementById("board") as HTMLCanvasElement
val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
val status = document.getElementById("status")!!
val aiToggle = document.getElementById("ai") as HTMLInputElement
val recordSelect = document.getElementById("records") as HTMLSelectElement
val replayPanel = document.getElementById("replay-panel") as HTMLElement
val stepLabel = document.getElementById("step")!!
val mistakeList = document.getElementById("mistakes")!!
val resumeButton = document.getElementById("resume")!!

// AI는 별도 워커에서 실행. 같은 프로토콜을 따르는 다른 워커(C++/Python 등)로 파일명만 바꿔 교체할 수 있다
val ai = Worker("ai.js")
var requestId = 0 // 다시 시작·리플레이 전환 때 증가시켜, 이전 요청에 대한 늦은 응답을 버린다

fun pos(i: Int) = CELL * (i + 1)

fun color(k: Int) = if (k % 2 == 0) 1 else 2 // k번째(0부터) 수의 색

fun colorName(c: Int) = if (c == 1) "흑" else "백"

/** 바둑판 좌표 표기: 가로 A~O, 세로 1~15 (예: H8) */
fun coord(i: Int) = "${'A' + i % N}${i / N + 1}"

fun aiTurn() = replay == null && aiToggle.checked && turn == AI && !over

/** 추천 대비 잃은 점수로 실수 등급을 매긴다. 기준은 GomokuAi 패턴 점수 (막힌 4·열린 3 ≈ 1,000 / 열린 4 ≈ 10,000) */
fun mistakeLabel(loss: Double) = when {
    loss >= 10_000 -> "악수"
    loss >= 1_000 -> "실수"
    else -> null
}

fun stone(i: Int, radius: Double) {
    ctx.beginPath(); ctx.arc(pos(i % N), pos(i / N), radius, 0.0, 2 * PI)
}

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
        stone(i, CELL * 0.45)
        ctx.fillStyle = if (board[i] == 1) "#111" else "#f8f8f8"
        ctx.fill()
        ctx.strokeStyle = "#000"; ctx.stroke()
    }
    if (last >= 0) {
        ctx.fillStyle = "#e33"
        stone(last, 4.0); ctx.fill()
    }
    status.textContent = if (replay != null) drawReplayMarks() else when {
        over -> "${colorName(turn)} 승리! 🎉"
        aiTurn() -> "AI 생각 중…"
        else -> "${colorName(turn)} 차례"
    }
}

/** 리플레이 중 현재 수가 실수면 둔 곳(빨간 테두리)과 추천 수(초록 원)를 표시하고 상태 문구를 돌려준다 */
fun drawReplayMarks(): String {
    val r = replay!!
    stepLabel.textContent = "${step} / ${r.size} 수"
    resumeButton.textContent = if (atMistake()) "이 수 다시 두기" else "여기서 다시 시작"
    if (step == 0) return "리플레이: 처음"
    val k = step - 1
    val head = "${step}수 ${colorName(color(k))} ${coord(r[k])}"
    val rv = review ?: return "$head (분석 중…)"
    val label = mistakeLabel(rv[k].loss as Double) ?: return head
    val best = rv[k].best as Int
    ctx.lineWidth = 3.0
    ctx.strokeStyle = "#e33"; stone(r[k], CELL * 0.5); ctx.stroke()
    ctx.strokeStyle = "#1a9e4b"; stone(best, CELL * 0.4); ctx.stroke()
    ctx.lineWidth = 1.0
    return "$head — $label! 추천 수 ${coord(best)} (초록 원)"
}

fun askAi() {
    if (aiTurn()) ai.postMessage(json("id" to ++requestId, "board" to board, "me" to AI))
}

fun play(i: Int) {
    board[i] = turn
    moves += i
    last = i
    if (isWin(board, i % N, i / N)) over = true else turn = 3 - turn
    draw()
    askAi()
}

fun reset() {
    exitReplay()
    board.fill(0); moves.clear(); turn = 1; last = -1; over = false
    requestId++
    draw()
}

// ---- 기보 저장 (브라우저 localStorage, 이 브라우저에서만 보인다) ----

fun loadRecords(): Array<dynamic> = try {
    localStorage.getItem(RECORDS_KEY)?.let { JSON.parse<Array<dynamic>>(it) } ?: emptyArray()
} catch (e: Throwable) {
    emptyArray()
}

fun refreshRecordSelect() {
    recordSelect.innerHTML = ""
    val records = loadRecords()
    if (records.isEmpty()) recordSelect.appendChild(document.createElement("option").also { it.textContent = "저장된 기보 없음" })
    for (i in records.indices.reversed()) { // 최신 기보가 위로
        val option = document.createElement("option")
        option.setAttribute("value", i.toString())
        option.textContent = records[i].name as String
        recordSelect.appendChild(option)
    }
}

fun saveRecord() {
    if (moves.isEmpty()) return
    val result = if (over) "${colorName(turn)} 승" else "진행 중"
    val name = "${Date().toLocaleString()} · ${moves.size}수 · $result"
    val records = loadRecords() + json("name" to name, "moves" to moves.toTypedArray())
    try {
        localStorage.setItem(RECORDS_KEY, JSON.stringify(records))
        refreshRecordSelect()
        status.textContent = "기보 저장됨: $name"
    } catch (e: Throwable) {
        status.textContent = "기보를 저장하지 못했습니다 (브라우저 저장소 사용 불가)"
    }
}

// ---- 리플레이 ----

fun startReplay() {
    val record = loadRecords().getOrNull(recordSelect.value.toIntOrNull() ?: return) ?: return
    val src = record.moves
    val r = IntArray(src.length as Int) { (src[it] as Number).toInt() }
    replay = r
    review = null
    replayPanel.hidden = false
    mistakeList.innerHTML = "<li>분석 중…</li>"
    ai.postMessage(json("id" to ++requestId, "type" to "review", "moves" to r))
    showStep(r.size)
}

fun showStep(s: Int) {
    val r = replay ?: return
    step = s.coerceIn(0, r.size)
    board.fill(0)
    for (k in 0 until step) board[r[k]] = color(k)
    last = if (step > 0) r[step - 1] else -1
    draw()
}

fun showReview(result: Array<dynamic>) {
    val r = replay ?: return
    review = result
    mistakeList.innerHTML = ""
    for (k in r.indices) {
        val label = mistakeLabel(result[k].loss as Double) ?: continue
        val li = document.createElement("li")
        li.textContent = "${k + 1}수 ${colorName(color(k))} ${coord(r[k])} — $label (추천 ${coord(result[k].best as Int)})"
        li.addEventListener("click", { showStep(k + 1) })
        mistakeList.appendChild(li)
    }
    if (mistakeList.childElementCount == 0) mistakeList.innerHTML = "<li>실수 없음 👍</li>"
    draw()
}

/** 지금 보고 있는 수(step번째)가 실수로 분석됐는지 */
fun atMistake(): Boolean {
    val rv = review ?: return false
    return step > 0 && mistakeLabel(rv[step - 1].loss as Double) != null
}

/**
 * 리플레이 중인 시점에서 일반 게임으로 이어서 둔다.
 * 보고 있는 수가 실수면 그 수 직전으로 돌아가, 실수한 차례부터 다시 둔다.
 */
fun resumeHere() {
    val r = replay ?: return
    if (atMistake()) showStep(step - 1)
    val s = step
    exitReplay()
    moves.clear(); moves.addAll(r.take(s))
    // board와 last는 showStep이 이미 그 시점으로 맞춰 둠
    over = s > 0 && isWin(board, last % N, last / N)
    turn = if (over) color(s - 1) else color(s)
    requestId++
    draw()
    askAi()
}

fun exitReplay() {
    replay = null
    review = null
    replayPanel.hidden = true
}

fun button(id: String, onClick: () -> Unit) =
    (document.getElementById(id) as HTMLButtonElement).addEventListener("click", { onClick() })

fun main() {
    canvas.width = SIZE.toInt()
    canvas.height = SIZE.toInt()
    canvas.addEventListener("click", { e ->
        e as MouseEvent
        if (replay != null || over || aiTurn()) return@addEventListener
        val rect = canvas.getBoundingClientRect()
        val scale = SIZE / rect.width
        val x = ((e.clientX - rect.left) * scale / CELL).roundToInt() - 1
        val y = ((e.clientY - rect.top) * scale / CELL).roundToInt() - 1
        if (x !in 0 until N || y !in 0 until N || board[y * N + x] != 0) return@addEventListener
        play(y * N + x)
    })
    ai.onmessage = { e ->
        val res = e.data.asDynamic()
        if (res.id == requestId) {
            if (res.review != undefined) {
                showReview(res.review as Array<dynamic>)
            } else {
                val move = res.move as Int
                if (aiTurn() && move in board.indices && board[move] == 0) play(move)
            }
        }
    }
    aiToggle.addEventListener("change", { draw(); askAi() })
    button("reset") { reset() }
    button("save") { saveRecord() }
    button("replay") { startReplay() }
    button("first") { showStep(0) }
    button("prev") { showStep(step - 1) }
    button("next") { showStep(step + 1) }
    button("last") { showStep(replay?.size ?: 0) }
    button("resume") { resumeHere() }
    button("exit") { reset() }
    window.addEventListener("keydown", { e ->
        e as KeyboardEvent
        if (replay == null) return@addEventListener
        when (e.key) {
            "ArrowLeft" -> showStep(step - 1)
            "ArrowRight" -> showStep(step + 1)
        }
    })
    refreshRecordSelect()
    draw()
}
