import org.w3c.dom.DedicatedWorkerGlobalScope
import kotlin.js.json

external val self: DedicatedWorkerGlobalScope

/**
 * Web Worker 어댑터. 프로토콜 (C++/Python 워커도 같은 형식을 따르면 교체 가능):
 *   요청  { id: Number, board: Int32Array | Number[225], me: 1 | 2 }
 *   응답  { id: 요청과 같은 값, move: board 인덱스 (y * 15 + x), 둘 곳 없으면 -1 }
 */
fun main() {
    self.onmessage = { e ->
        val req = e.data.asDynamic()
        val src = req.board
        val board = IntArray(GomokuAi.N * GomokuAi.N) { (src[it] as Number).toInt() }
        self.postMessage(json("id" to req.id, "move" to GomokuAi.bestMove(board, req.me as Int)))
    }
}
