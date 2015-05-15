package autok.example

import java.util.{ Timer, TimerTask }

import autok.authentication.{ Authentication, SimpleAuthentication, StubAuthServerConfig, Token }
import autok.mocks.LocalHostTokenServer
import com.github.tomakehurst.wiremock.core.Container

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, blocking }
import scala.io.StdIn
import scala.util.{ Success, Try }
import scalaj.http.{ Http, HttpResponse }

case class Mock(containers: Seq[Container])

object Main extends App {
  @tailrec
  final def loop(tokenOpt: Option[Token], currentRequest: Option[String]): Unit = {
    val token = tokenOpt.getOrElse {
      blocking {
        Await.result(authService.getToken, Duration.Inf)
      }
    }

    val request = currentRequest.getOrElse {
      println()
      print("URL> ")
      StdIn.readLine().trim
    }

    if (request.nonEmpty) {
      val result = Try(Http(request)
        .header("Authorization", token)
        .asString)
      println(result)

      val tokenExpired = result match {
        case Success(HttpResponse(_, 401, _)) => true
        case _ => false
      }

      if (tokenExpired) {
        authService.tokenExpired(token)
        loop(None, Some(request))
      } else {
        loop(Some(token), None)
      }
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
        new Timer(true).schedule(new TimerTask {
          override def run(): Unit = {
            LocalHostTokenServer.invalidateAllTokens()
          }
        }, 0, 10000)

        (service, mocks)
    }

  println("Enter empty line to exit")
  loop(None, None)

  mocks.foreach(_.shutdown())
}
