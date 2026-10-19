import org.w3c.dom.DedicatedWorkerGlobalScope
import kotlin.js.json

external val self: DedicatedWorkerGlobalScope

/**
 * Web Worker 어댑터. 프로토콜 (C++/Python 워커도 같은 형식을 따르면 교체 가능):
 *   착수  요청 { id, type: "move"(생략 가능), board: Int32Array | Number[225], me: 1 | 2 }
 *         응답 { id, move: board 인덱스 (y * 15 + x), 둘 곳 없으면 -1 }
 *   분석  요청 { id, type: "review", moves: Number[] (흑부터 번갈아 둔 인덱스) }
 *         응답 { id, review: [{ best: 추천 인덱스, loss: 추천 대비 잃은 점수 }, ...] }
 */
fun main() {
    self.onmessage = { e ->
        val req = e.data.asDynamic()
        val res = if (req.type == "review") {
            val src = req.moves
            val moves = IntArray(src.length as Int) { (src[it] as Number).toInt() }
            val review = GomokuAi.review(moves).map { json("best" to it.best, "loss" to it.loss) }
            json("id" to req.id, "review" to review.toTypedArray())
        } else {
            val src = req.board
            val board = IntArray(GomokuAi.N * GomokuAi.N) { (src[it] as Number).toInt() }
            json("id" to req.id, "move" to GomokuAi.bestMove(board, req.me as Int))
        }
        self.postMessage(res)
    }
}
