package autok.authentication

import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, blocking }
import scalaj.http.{ Http, HttpResponse }

trait AuthServerConfig {
  def host: String
  def port: Int
  def username: String
  def password: String
}

object SimpleAuthentication extends DefaultJsonProtocol {
  case class ErrorResponse(error: String)
  case class TokenResponse(token: String)

  implicit val errorResponseFormat = jsonFormat1(ErrorResponse.apply)
  implicit val tokenResponseFormat = jsonFormat1(TokenResponse.apply)
}

class SimpleAuthentication(authServerConfig: AuthServerConfig = DefaultAuthServerConfig) extends Authentication {

  import SimpleAuthentication._
  import authServerConfig._

  private var token = Option.empty[Token]

  private val authUrl =
    s"http://$host:$port/token?user=$username&pass=$password"

  private def resetUrl(oldToken: String) =
    s"http://$host:$port/token/refresh?token=$oldToken"

  override def getToken: Future[Token] = {
    syncFuture {
      token.getOrElse(resetToken(authUrl))
    }
  }

  override def tokenExpired(oldToken: Token): Unit = {
    val resetFuture = syncFuture {
      if (token.contains(oldToken)) {
        token = None
        resetToken(resetUrl(oldToken))
      }
    }

    for (error <- resetFuture.failed) {
      println(s"Token refresh failed: $error")
    }
  }

  private def resetToken(url: String): Token = {
    Http(url).postData("").asString match {
      case HttpResponse(body, 200, _) =>
        val newToken = body.parseJson.convertTo[TokenResponse].token
        token = Some(newToken)
        newToken
      case HttpResponse(body, 401, _) =>
        throw new IllegalArgumentException(body.parseJson.convertTo[ErrorResponse].error)
    }
  }

  private def syncFuture[T](block: => T): Future[T] = {
    Future(blocking(synchronized(block)))
  }
}

object DefaultAuthServerConfig extends AuthServerConfig {
  private lazy val config = ConfigFactory.load().getConfig("auth-server")

  override lazy val host: String = config.getString("host")
  override lazy val port: Int = config.getInt("port")
  override lazy val username: String = config.getString("username")
  override lazy val password: String = config.getString("password")
}

case class StubAuthServerConfig(
  host: String = "localhost",
  username: String,
  password: String,
  port: Int) extends AuthServerConfig
