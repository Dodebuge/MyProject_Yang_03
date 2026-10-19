const val N = 15

/** board[y * N + x]: 0 = 빈칸, 1 = 흑, 2 = 백. (x, y)에 방금 둔 돌로 5목 이상이면 true */
fun isWin(board: IntArray, x: Int, y: Int): Boolean {
    val me = board[y * N + x]
    if (me == 0) return false
    for ((dx, dy) in listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)) {
        var count = 1
        for (sign in intArrayOf(1, -1)) {
            var cx = x + dx * sign
            var cy = y + dy * sign
            while (cx in 0 until N && cy in 0 until N && board[cy * N + cx] == me) {
                count++; cx += dx * sign; cy += dy * sign
            }
        }
        if (count >= 5) return true
    }
    return false
}
