/**
 * .dat 기보 포맷: 둔 순서대로 한 줄에 한 수씩 "(x,y)".
 * x = 가로(왼쪽부터), y = 세로(위부터), 둘 다 0~14. 첫 줄이 흑, 이후 번갈아 백·흑.
 *   (7,7)
 *   (8,7)
 *   ...
 */
fun toDat(moves: List<Int>) = moves.joinToString("\n", postfix = "\n") { "(${it % N},${it / N})" }

private val DAT_LINE = Regex("""^\(?\s*(\d+)\s*,\s*(\d+)\s*\)?$""")

/** .dat 텍스트를 board 인덱스 목록으로 바꾼다. 형식이 틀리면 IllegalArgumentException (메시지에 줄 번호) */
fun parseDat(text: String): IntArray {
    val seen = mutableSetOf<Int>()
    val moves = text.lines().withIndex().filter { it.value.isNotBlank() }.map { (n, line) ->
        val m = DAT_LINE.find(line.trim()) ?: throw IllegalArgumentException("${n + 1}번째 줄 형식이 (x,y)가 아닙니다: $line")
        val x = m.groupValues[1].toInt()
        val y = m.groupValues[2].toInt()
        require(x in 0 until N && y in 0 until N) { "${n + 1}번째 줄 좌표가 0~${N - 1} 범위를 벗어났습니다: $line" }
        val i = y * N + x
        require(seen.add(i)) { "${n + 1}번째 줄 ($x,$y)에 이미 돌이 있습니다" }
        i
    }
    require(moves.isNotEmpty()) { "수가 하나도 없습니다" }
    return moves.toIntArray()
}
