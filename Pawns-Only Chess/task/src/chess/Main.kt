package chess

import kotlin.math.absoluteValue
import kotlin.math.max

object Logger {
    val name = "Logger"
    private val DEBUGGING = false
    fun log(text: String) {
        if (DEBUGGING) print("$name: $text")
    }

    fun logln(text: String) {
        if (DEBUGGING) {
            log("$text\n")

        }
    }
}

fun main() {
    println("Pawns-Only Chess")
    println("First Player's name: ")
    val firstName = readln()
    println("Second Player's name: ")
    val secondName = readln()


    val game = PawnsOnlyGame(firstName, secondName)
    game.draw()
    while (game.isPlaying) {

        game.printTurn()
        if (game.input(readln())) {
            game.draw()
        }
    }
    game.printGoodBy()
}

class ReturnMessage(val status: Int, val message: String) {
    companion object {

        val statusOK = 0
        val statusERROR = -1
        val OKMessage = ReturnMessage(statusOK, "")
    }


}

class PawnsOnlyGame(val firstPlayerName: String, val secondPlayerName: String) {
    private val movePattern = """[a-h][1-8][a-h][1-8]"""
    private val cmdRegex = "(exit)|($movePattern)".toRegex()
    private val gameLog = mutableListOf<String>()

    var isPlaying = true
        private set
    private var winner: Player? = null

    var isStalemate = false
        private set

    init {
        player1.playerName = firstPlayerName
        player2.playerName = secondPlayerName
    }

    private data class Player(
        var playerName: String,
        val playerSymbol: String,
        val playerColor: String,
        val dir: Directions,
        val initialPawnsRow: Int
    ) {
        val enPassantRow = initialPawnsRow - 3 * dir.dir
        val winningRow = initialPawnsRow + dir.dir
        var takenPawns = 0

        //var lastMove=""
        init {
            Logger.logln(this.toString() + " winningRow: $winningRow")
        }
    }

    private companion object {

        enum class Directions(val dir: Int, val angle: Int) {
            UP(1, 0),
            UP_RIGHT_DIAGONAL(1, 1),
            UP_LEFT_DIAGONAL(1, -1),
            DOWN(-1, 0),
            DOWN_RIGHT_DIAGONAL(-1, 1),
            DOWN_LEFT_DIAGONAL(-1, -1),
            UNKNOWN(0, 0)
        }

        val board = ChessBoard()
        val player1 = Player("", "W", "white", Directions.UP, 6)
        val player2 = Player("", "B", "black", Directions.DOWN, 1)


        var currentPlayer = player1
        var opponentPlayer = player2
        fun switchPlayers() {
            currentPlayer = if (currentPlayer === player1) player2 else player1
            opponentPlayer = if (opponentPlayer === player1) player2 else player1
        }
    }

    fun draw() {
        board.draw()
    }

    fun checkMove(move: String) = cmdRegex.matches(move)

    private fun play(move: String): ReturnMessage {
        require(cmdRegex.matches(move))
        var msg = checkPlayerPawn(move)
        if (msg.status != ReturnMessage.statusOK) return msg

        msg = checkPlayerPawn(move)
        if (msg.status != ReturnMessage.statusOK) return msg

        msg = chekPlayerMoveDirection(move)
        if (msg.status != ReturnMessage.statusOK) return msg

        msg = checkMoveSteps(move)
        if (msg.status != ReturnMessage.statusOK) return msg

        msg = pawnCanMove(move, currentPlayer)
        if (msg.status != ReturnMessage.statusOK) return msg

        applyMove(move)
        checkGameOver(move)
        switchPlayers()
        return ReturnMessage.OKMessage


    }

    private fun checkGameOver(move: String) {
        val row = move.last().digitToInt() - 1
        if (row == currentPlayer.winningRow ||
            currentPlayer.takenPawns == 8
        ) {
            isPlaying = false
            winner = currentPlayer
        } else {

            if (checkOponentStaleMate()) {
                isStalemate = true
                isPlaying = false
            }
        }





    }

    private fun checkOponentStaleMate(): Boolean {
        Logger.logln("checkOponentStaleMate")
        for (row in 0..7)
        {
            for ( col in 'a'..'h'){
                val cell = board.getCell(col,"${row+1}".first())

                if (cell.hasContent(opponentPlayer.playerSymbol)){
                    Logger.logln("current pawnt @ $col${row+1}")

                    val nextRow = row + opponentPlayer.dir.dir
                    if (nextRow in 1..6){
                        for (nextCol in (col-1)..(col+1)){
                            if (nextCol in 'a'..'h') {
                                val move = "$col${row + 1}${nextCol}${nextRow + 1}"
                                Logger.logln("move:$move")
                                if (pawnCanMove(move, opponentPlayer) === ReturnMessage.OKMessage) {
                                    return false
                                }
                            }
                        }
                    }
                }
            }

        }
        return true
    }

    private fun applyMove(move: String) {
        val initialCell = board.getCell(move[0], move[1])
        val secondCell = board.getCell(move[2], move[3])

        val dir = getMoveDirection(move)
        if (secondCell.isEmpty() && dir.angle != 0) {
            val enpassantCell = board.getCell(move[0] + dir.angle, move[1])
            enpassantCell.clear()
            currentPlayer.takenPawns++
        } else if (!secondCell.isEmpty()) {
            currentPlayer.takenPawns++
        }
        secondCell.setContent(initialCell.content)
        initialCell.clear()
        gameLog.add(move)

    }

    private fun pawnCanMove(move: String,player:Player): ReturnMessage {
        Logger.logln("pawnCanMove")
        val secondCell = board.getCell(move[2], move[3])

        if (secondCell.hasContent(player.playerSymbol))
            return ReturnMessage(ReturnMessage.statusERROR, "Invalid Input")
        val dir = getMoveDirection(move)
        if (secondCell.isEmpty() && dir.angle != 0) {
            val pssCell = (move[0] + dir.angle).toString() + move[1]
            val enpassantCell = board.getCell(move[0] + dir.angle, move[1])
            if (player.enPassantRow == 8 - move[1].digitToInt() && !enpassantCell.isEmpty())
                if (gameLog.last().substring(2) == pssCell) {
                    return ReturnMessage.OKMessage
                } else {
                    return ReturnMessage(ReturnMessage.statusERROR, "Invalid Input")
                }
            else
                return ReturnMessage(ReturnMessage.statusERROR, "Invalid Input")
        }
        if (!secondCell.isEmpty() && (dir.angle == 0))
            return ReturnMessage(
                ReturnMessage.statusERROR,
                "Invalid Input"
            )
//        if (secondCell.hasContent(player.playerSymbol) && dir.angle != 0) return ReturnMessage(
//            ReturnMessage.statusERROR,
//            "Invalid Input"
//        )
//        if (secondCell.isEmpty())
        return ReturnMessage.OKMessage

    }

    private fun checkMoveSteps(move: String): ReturnMessage {
        Logger.logln("checkMoveSteps")
        val row = 8 - move[1].digitToInt()
        Logger.logln("$row")
        val steps = getMoveSteps(move)
        val direction = getMoveDirection(move)

        Logger.logln("steps: $steps")
        Logger.logln("currentPlayer.initialPawnsRow: ${currentPlayer.initialPawnsRow}")
        if (steps > 2) return ReturnMessage(ReturnMessage.statusERROR, "Invalid Input")
        if (direction.angle != 0 && steps >= 2) return ReturnMessage(ReturnMessage.statusERROR, "Invalid Input")
        if (row == currentPlayer.initialPawnsRow && steps == 2) return ReturnMessage.OKMessage
        if (steps == 1) return ReturnMessage.OKMessage
        return ReturnMessage(ReturnMessage.statusERROR, "Invalid Input")

    }

    private fun checkPlayerPawn(move: String): ReturnMessage {
        Logger.logln("checkPlayerPawn")
        val initalCell = board.getCell(move[0], move[1])
//        val nextCell = board.getCell(move[2], move[3])

        if (initalCell.isEmpty())
            return ReturnMessage(
                ReturnMessage.statusERROR,
                "No ${currentPlayer.playerColor} pawn at ${move[0]}${move[1]}"
            )
        Logger.logln("initalCell is not empty")
        Logger.logln("initalCell content: ${initalCell.content}")
        if (!initalCell.isEmpty() && !initalCell.hasContent(currentPlayer.playerSymbol))
            return ReturnMessage(
                ReturnMessage.statusERROR,
                "No ${currentPlayer.playerColor} pawn at ${move[0]}${move[1]}"
            )

        return ReturnMessage.OKMessage
    }


    private fun chekPlayerMoveDirection(move: String): ReturnMessage {
        Logger.logln("chekPlayerMoveDirection")
        val dir = getMoveDirection(move)
        Logger.logln(dir.toString())
        return if (dir.dir == currentPlayer.dir.dir) ReturnMessage.OKMessage else ReturnMessage(
            ReturnMessage.statusERROR, "Invalid Input"
        )
    }

    private fun getMoveDirection(move: String): Directions {
        val initRow = 8 - move[1].digitToInt()
        val nextRow = 8 - move[3].digitToInt()
        val initCol = move[0]
        val nextCol = move[2]

        if (initRow - nextRow < 0)
            return if (initCol == nextCol) Directions.DOWN
            else if (initCol < nextCol) Directions.DOWN_RIGHT_DIAGONAL
            else Directions.DOWN_LEFT_DIAGONAL

        if (initRow - nextRow > 0)
            return if (initCol == nextCol) Directions.UP
            else if (initCol < nextCol) Directions.UP_RIGHT_DIAGONAL
            else Directions.UP_LEFT_DIAGONAL

        return Directions.UNKNOWN

    }

    private fun getMoveSteps(move: String): Int {
        val initRow = 8 - move[1].digitToInt()
        val nextRow = 8 - move[3].digitToInt()

        val initCol = move[0]
        val nextCol = move[2]


        return max((initRow - nextRow).absoluteValue, (initCol - nextCol).absoluteValue)

    }

    fun input(cmd: String): Boolean {

        if (cmdRegex.matches(cmd)) {
//            println("cmd is ok")
            val c = cmdRegex.find(cmd)?.groupValues?.get(0)!!
            if (c == "exit") {
                exitGame()
                return false
            } else {
                val rm = play(c)
                return if (rm === ReturnMessage.OKMessage) true
                else {
                    println(rm.message)
                    false
                }
            }
        } else {
            println("Invalid Input")
            return false
        }
    }

    private fun exitGame() {
        isPlaying = false
    }

    fun printGoodBy() {
        if (isStalemate)
            println("Stalemate!")
        winner?.let { println("${it.playerColor} Wins!") }

        println("Bye!")

    }

    fun printTurn() {
        println("${currentPlayer.playerName}'s turn:")
        print("> ")
    }
}

class ChessBoard() {
    class Cell(content: String = " ") {
        var content = content
            private set

        override fun toString(): String {
            return " $content "
        }

        fun isEmpty() = content == " "
        fun hasContent(c: String) = content == c
        fun setContent(c: String) {
            content = c
        }

        fun clear() {
            content = " "
        }

    }

    private val cells = List(8) { row -> List(8) { col -> Cell(if (row == 6) "W" else if (row == 1) "B" else " ") } }

    fun draw() {
        repeat(8)
        {
            printLine()
            printBars(it)
        }

        printLine()
        print("  ")
        ('a'..'h').forEach { print("  $it ") }
        println()
    }

    private fun printBars(rowNum: Int) {
        print("${8 - rowNum} ")
        repeat(8) {
            print("|${cells[rowNum][it]}")
        }
        println("|")
    }

    private fun printLine() {
        print("  ")
        repeat(8) {
            print("+---")
        }
        println("+")
    }

    fun getCell(colLetter: Char, rowLetter: Char): Cell {

        val row = 8 - rowLetter.digitToInt()
        val col = colLetter - 'a'
        return cells[row][col]
    }
}