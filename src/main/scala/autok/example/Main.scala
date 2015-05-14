package autok.example

import autok.authentication.{ Authentication, SimpleAuthentication, StubAuthServerConfig, Token }
import autok.mocks.LocalHostTokenServer
import com.github.tomakehurst.wiremock.core.Container

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, blocking }
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scalaj.http.Http

object Main extends App {
  @tailrec
  final def loop(token: Token): Unit = {
    val url = StdIn.readLine().trim
    if (url.nonEmpty) {
      val result = Http(url)
        .header("Authorization", token)
        .asString
      println(s"Exit code: ${result.code}")
      println(result.body)
      loop(token)
    }
  }

  var mocks: Seq[Container] = Nil

  val authService: Authentication =
    args match {
      case Array() =>
        new SimpleAuthentication
      case Array("--mock") =>
        val auth = LocalHostTokenServer.startAuthServer()
        val datetime = LocalHostTokenServer.startDateTimeService()
        mocks = Seq(auth, datetime)
        new SimpleAuthentication(
          StubAuthServerConfig(
            port = auth.port(),
            username = LocalHostTokenServer.USER,
            password = LocalHostTokenServer.PASS))
    }

  val tokenFuture = authService.getToken
  tokenFuture.failed.foreach(println)

  blocking {
    Await.ready(tokenFuture.map(loop), Duration.Inf)
  }

  mocks.foreach(_.shutdown())
}
