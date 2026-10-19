/**
 * 패턴 점수 기반 오목 AI 라이브러리.
 * 보드 형식은 게임과 같다: board[y * N + x], 0 = 빈칸, 1 = 흑, 2 = 백.
 */
// ponytail: 한 수 앞만 보는 휴리스틱이고 띈 삼(X_XX)은 못 본다. 더 강하게 하려면 이 점수 위에 알파-베타 탐색을 얹을 것
object GomokuAi {
    const val N = 15
    private val DIRS = arrayOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

    /** me(1 또는 2) 차례의 최선 수를 board 인덱스로 돌려준다. 둘 곳이 없으면 -1 */
    fun bestMove(board: IntArray, me: Int): Int {
        require(board.size == N * N) { "board 크기는 ${N * N}이어야 합니다" }
        require(me == 1 || me == 2) { "me는 1 또는 2여야 합니다" }
        if (board.all { it == 0 }) return N * N / 2 // 첫 수는 천원
        var best = -1
        var bestScore = -1.0
        for (i in board.indices) {
            if (board[i] != 0 || !nearStone(board, i)) continue
            // 공격에 가중치를 조금 더 줘서, 내가 이길 수 있으면 막기보다 이기기를 고른다
            val s = score(board, i, me) * 1.1 + score(board, i, 3 - me)
            if (s > bestScore) { bestScore = s; best = i }
        }
        return best
    }

    /** 주변 2칸 안에 돌이 있는 칸만 후보로 본다 */
    private fun nearStone(board: IntArray, i: Int): Boolean {
        val x = i % N
        val y = i / N
        for (dy in -2..2) for (dx in -2..2) {
            val cx = x + dx
            val cy = y + dy
            if (cx in 0 until N && cy in 0 until N && board[cy * N + cx] != 0) return true
        }
        return false
    }

    /** p가 i에 뒀을 때 4방향 패턴 점수의 합 */
    private fun score(board: IntArray, i: Int, p: Int): Int {
        val x = i % N
        val y = i / N
        var total = 0
        for ((dx, dy) in DIRS) {
            var count = 1
            var open = 0
            for (sign in intArrayOf(1, -1)) {
                var cx = x + dx * sign
                var cy = y + dy * sign
                while (cx in 0 until N && cy in 0 until N && board[cy * N + cx] == p) {
                    count++; cx += dx * sign; cy += dy * sign
                }
                if (cx in 0 until N && cy in 0 until N && board[cy * N + cx] == 0) open++
            }
            total += value(count, open)
        }
        return total
    }

    private fun value(count: Int, open: Int) = when {
        count >= 5 -> 100_000
        open == 0 -> 0
        count == 4 -> if (open == 2) 10_000 else 1_000
        count == 3 -> if (open == 2) 1_000 else 100
        count == 2 -> if (open == 2) 100 else 10
        else -> open
    }
}
