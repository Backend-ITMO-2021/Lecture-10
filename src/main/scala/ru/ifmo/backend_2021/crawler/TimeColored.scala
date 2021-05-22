package ru.ifmo.backend_2021.crawler

object TimeColored {
  def apply[R](f: => R): R = {
    val tagColored = PrintColored.makeColored("y")("time")
    val timeStart = System.nanoTime()
    val result = f
    val endTime = (System.nanoTime() - timeStart) / 1000
    println(s"[$tagColored] ${endTime / 1000},${(endTime % 1000 + 1000).toString.drop(1)}ms")
    result
  }
}

object PrintColored {
  def just[R](color: String = "", bgColor: String = "")(f: => R): Unit = {
    val result = f
    println(makeColored(color.toLowerCase, bgColor.toLowerCase)(result))
  }

  def apply[R](color: String = "", bgColor: String = "")(f: => R): R = {
    val result = f
    println(makeColored(color.toLowerCase, bgColor.toLowerCase)(result))
    result
  }

  def makeColored[R](color: String, bgColor: String = "")(f: R): String = {
    val colorAnsi = color match {
      case "y" => Console.YELLOW
      case "g" => Console.GREEN
      case "b" => Console.BLUE
      case "r" => Console.RED
      case "c" => Console.CYAN
      case "m" => Console.MAGENTA
      case "" => Console.BLACK
    }
    val bgColorAnsi = bgColor match {
      case "w" => Console.WHITE_B
      case "" => Console.BLACK_B
    }
    s"$colorAnsi$bgColorAnsi$f${Console.RESET}"
  }
}
