import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GomokuTest {
    private fun board(vararg xy: Pair<Int, Int>) = IntArray(N * N).also { b -> xy.forEach { (x, y) -> b[y * N + x] = 1 } }

    @Test fun win() {
        assertTrue(isWin(board(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0), 2, 0))          // 가로
        assertTrue(isWin(board(5 to 5, 6 to 6, 7 to 7, 8 to 8, 9 to 9), 9, 9))          // 대각
        assertTrue(isWin(board(4 to 0, 3 to 1, 2 to 2, 1 to 3, 0 to 4), 0, 4))          // 역대각
        assertFalse(isWin(board(0 to 0, 1 to 0, 2 to 0, 3 to 0), 3, 0))                 // 4목
        assertFalse(isWin(board(13 to 0, 14 to 0, 0 to 1, 1 to 1, 2 to 1), 14, 0))      // 줄 넘김 X
    }
}
