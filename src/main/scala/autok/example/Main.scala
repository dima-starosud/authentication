package autok.example

import java.util.{ TimerTask, Timer }

import autok.authentication.{ Authentication, SimpleAuthentication, StubAuthServerConfig, Token }
import autok.mocks.LocalHostTokenServer
import com.github.tomakehurst.wiremock.core.Container

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, blocking }
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.Try
import scalaj.http.Http

case class Mock(containers: Seq[Container])

object Main extends App {
  @tailrec
  final def loop(token: Token): Unit = {
    println()
    print("URL> ")
    val url = StdIn.readLine().trim
    if (url.nonEmpty) {
      val result = Try(Http(url)
        .header("Authorization", token)
        .asString)
      println(result)
      loop(token)
    }
  }

  val (authService, mocks): (Authentication, Seq[Container]) =
    args match {
      case Array() =>
        (new SimpleAuthentication, Nil)
      case Array("--mock") =>
        val auth = LocalHostTokenServer.startAuthServer()
        val datetime = LocalHostTokenServer.startDateTimeService()
        val mocks = Seq(auth, datetime)
        val service = new SimpleAuthentication(
          StubAuthServerConfig(
            port = auth.port(),
            username = LocalHostTokenServer.USER,
            password = LocalHostTokenServer.PASS))

        // invalidate tokens every 10 seconds
        (new Timer).schedule(new TimerTask {
          override def run(): Unit = {
            LocalHostTokenServer.invalidateAllTokens()
          }
        }, 10000)

        (service, mocks)
    }

  val tokenFuture = authService.getToken
  tokenFuture.failed.foreach(println)

  println("Enter empty line to exit")
  blocking {
    Await.ready(tokenFuture.map(loop), Duration.Inf)
  }

  mocks.foreach(_.shutdown())
}
