import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KifuTest {
    @Test fun roundTrip() {
        val moves = listOf(7 * N + 7, 7 * N + 8, 0, N * N - 1)
        val dat = toDat(moves)
        assertEquals("(7,7)\n(8,7)\n(0,0)\n(14,14)\n", dat)
        assertContentEquals(moves.toIntArray(), parseDat(dat))
    }

    @Test fun toleratesSpacesBlankLinesAndCrlf() =
        assertContentEquals(intArrayOf(7 * N + 7, 8 * N + 7), parseDat("( 7 , 7 )\r\n\r\n7,8\r\n"))

    @Test fun rejectsBadInput() {
        assertFailsWith<IllegalArgumentException> { parseDat("") }
        assertFailsWith<IllegalArgumentException> { parseDat("(7,7)\nhello\n") }
        assertFailsWith<IllegalArgumentException> { parseDat("(15,0)\n") }
        assertFailsWith<IllegalArgumentException> { parseDat("(7,7)\n(7,7)\n") }
    }
}
