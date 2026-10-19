import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GomokuAiTest {
    private val n = GomokuAi.N

    private fun board(black: List<Pair<Int, Int>>, white: List<Pair<Int, Int>> = emptyList()) = IntArray(n * n).also { b ->
        black.forEach { (x, y) -> b[y * n + x] = 1 }
        white.forEach { (x, y) -> b[y * n + x] = 2 }
    }

    @Test fun firstMoveIsCenter() = assertEquals(7 * n + 7, GomokuAi.bestMove(IntArray(n * n), 1))

    @Test fun blocksOpponentFour() {
        // 흑 (3..6, 5), 왼쪽은 백이 막음 → 백은 (7, 5)를 막아야 한다
        val b = board(black = (3..6).map { it to 5 }, white = listOf(2 to 5, 10 to 10))
        assertEquals(5 * n + 7, GomokuAi.bestMove(b, 2))
    }

    @Test fun prefersWinningOverBlocking() {
        // 흑도 4목 위협, 백도 4목 → 백은 막지 말고 이겨야 한다
        val b = board(black = (3..6).map { it to 5 } + (2 to 9), white = (3..6).map { it to 9 } + (2 to 5))
        assertEquals(9 * n + 7, GomokuAi.bestMove(b, 2))
    }

    @Test fun reviewFlagsMissedBlock() {
        // 흑이 (3..6, 5)로 열린 4를 만들었는데 백이 막지 않고 (0, 0)에 둠 → 마지막 수가 큰 손실
        val moves = intArrayOf(78, 93, 79, 94, 80, 95, 81, 0)
        val r = GomokuAi.review(moves)
        assertEquals(moves.size, r.size)
        assertTrue(r[7].loss >= 10_000, "loss=${r[7].loss}")
        assertTrue(r[7].best == 5 * n + 2 || r[7].best == 5 * n + 7, "best=${r[7].best}")
        assertTrue(r[6].loss < 1_000, "흑의 열린 4 만들기는 실수가 아님: ${r[6].loss}")
    }

    @Test fun fullBoardHasNoMove() {
        val b = IntArray(n * n) { if ((it / n + it % n / 2) % 2 == 0) 1 else 2 }
        assertTrue(GomokuAi.bestMove(b, 1) == -1)
    }
}
