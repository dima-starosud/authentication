package autok

import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}
import scalaj.http.{Http, HttpResponse}

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

  private val authUrl =
    s"http://${authServerConfig.host}/token?user=${authServerConfig.username}&pass=${authServerConfig.password}"

  private def resetUrl(oldToken: String) =
    s"http://${authServerConfig.host}/token/refresh?token=$oldToken"

  override def getToken: Future[Token] = {
    Future {
      blocking {
        synchronized {
          perform(authUrl)
        }
      }
    }
  }

  override def tokenExpired(oldToken: Token): Unit = {
    perform(resetUrl(oldToken))
  }

  private def perform(url: String): Token = {
    Http(url).postData("").asString match {
      case HttpResponse(body, 200, _) =>
        body.parseJson.convertTo[TokenResponse].token
      case HttpResponse(body, 401, _) =>
        throw new IllegalArgumentException(body.parseJson.convertTo[ErrorResponse].error)
    }
  }
}

object DefaultAuthServerConfig extends AuthServerConfig {
  private lazy val config = ConfigFactory.load().getConfig("auth-server")

  override lazy val host: String = config.getString("host")
  override lazy val port: Int = config.getInt("port")
  override lazy val username: String = config.getString("username")
  override lazy val password: String = config.getString("password")
}
