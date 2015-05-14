package autok.example

import autok.mocks.LocalHostTokenServer

import scala.annotation.tailrec
import scala.io.StdIn
import scala.util.Try

object Main extends App {
  @tailrec
  final def loop(): Unit = {
    loop()
  }

  val auth = LocalHostTokenServer.startAuthServer()
  val datetime = LocalHostTokenServer.startDateTimeService()
  println("Press enter to exit")
  Try(StdIn.readChar())
  auth.shutdown()
  datetime.shutdown()
}
