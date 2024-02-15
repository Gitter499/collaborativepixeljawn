import io.javalin.Javalin

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.concurrent.timer


import kotlin.time.Duration.Companion.minutes


data class Pixel(val x: Int, val y: Int, val color: String)

typealias Board = MutableList<MutableList<Pixel>>

val timeout = 1.minutes

object BoardController {

    private fun createBoard(width: Int, height: Int): Board {
        val board = mutableListOf<MutableList<Pixel>>()
        for (x in 0..<width) {
            val row = mutableListOf<Pixel>()
            for (y in 0..<height) {
                row.add(Pixel(x, y, "white"))
            }
            board.add(row)
        }
        return board
    }

    val board = createBoard(20, 20)

    fun setPixel(pixel: Pixel) {
        board[pixel.y][pixel.x] = pixel
    }

    fun getPixel(x: Int, y: Int): Pixel {
        return board[y][x]
    }

}

fun main(args: Array<String>) {

    val sessions = mutableMapOf<String, LocalDateTime>()

    val port = 8000

    println("Starting server... on port $port")


    val app = Javalin.create { it ->


    }.start(port)

    app.post("/userland/board") { context ->
        val pixel = context.bodyAsClass(Pixel::class.java)


        println(sessions)

        if (sessions[context.ip()] == null) {
            println("Creating session for ${context.ip()}")
            sessions[context.ip()] = LocalDateTime.now().minusMinutes(1)
        }

        val canPlace = ChronoUnit.MINUTES.between(sessions[context.ip()], LocalDateTime.now()) >= timeout.inWholeMinutes
        println(ChronoUnit.MINUTES.between(sessions[context.ip()], LocalDateTime.now()))
        if (canPlace) {
            sessions[context.ip()] = LocalDateTime.now()
            BoardController.setPixel(pixel)
            context.status(200)
        } else {
            context.status(400).json(object {
                val session = sessions[context.ip()].toString()
            })
        }

    }

    app.ws("board") { wsConfig ->
        wsConfig.onConnect { handler ->

            println("Connected!")
            handler.enableAutomaticPings()


            timer(period = 1000) {
                handler.send(BoardController.board)
            }


        }


    }


}