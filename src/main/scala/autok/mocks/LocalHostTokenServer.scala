package autok.mocks

import autok.authentication.SimpleAuthentication._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.core.Container
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.{ MultiValue, Request, ResponseDefinition }
import spray.http.DateTime
import spray.json._

import scala.util.{ Random, Try }

object LocalHostTokenServer {
  // use short value for simplicity
  val TOKEN_LENGTH = 16

  sealed trait TokenState
  case object TokenValid extends TokenState
  case object TokenExpired extends TokenState

  private var tokens = Map.empty[String, TokenState]

  def newToken(): String = synchronized {
    val token = Random.alphanumeric.take(TOKEN_LENGTH).mkString
    tokens = tokens.updated(token, TokenValid)
    println(s"New token: $token")
    token
  }

  def invalidateToken(token: String): Option[TokenState] = synchronized {
    val validOpt = tokens.get(token)
    for (TokenValid <- validOpt) {
      tokens = tokens.updated(token, TokenExpired)
      println(s"Expired token: $token")
    }
    validOpt
  }

  def invalidateAllTokens(): Unit = {
    tokens.keySet.foreach(invalidateToken)
  }

  def resetToken(token: String): Option[String] = synchronized {
    val validOpt = tokens.get(token)
    // TODO should we allow resetting valid token?
    for (TokenExpired <- validOpt)
      yield newToken()
  }

  def tokenState(token: String): Option[TokenState] = synchronized {
    tokens.get(token)
  }

  // assume we have single user in our DB
  val USER = "test"
  val PASS = "1111"

  def startAuthServer(): Container = {
    val server = new WireMockServer(
      wireMockConfig()
        .dynamicPort()
        .extensions(
          classOf[NewTokenResponseTransformer].getName,
          classOf[RemoveTokenResponseTransformer].getName))

    server.stubFor(
      post(urlMatching("/token\\?.*"))
        .willReturn(aResponse()
          .withTransformers(classOf[NewTokenResponseTransformer].getName)))

    server.stubFor(
      post(urlMatching("/token/refresh\\?.*"))
        .willReturn(aResponse()
          .withTransformers(classOf[RemoveTokenResponseTransformer].getName)))

    server.start()
    println(s"Token Server mock started at port: ${server.port()}")
    server
  }

  def startDateTimeService(): Container = {
    val server = new WireMockServer(
      wireMockConfig()
        .dynamicPort()
        .extensions(
          classOf[DateTimeResponseTransformer].getName))

    server.stubFor(
      get(urlEqualTo("/datetime"))
        .willReturn(aResponse()
          .withTransformers(classOf[DateTimeResponseTransformer].getName)))

    server.start()
    println(s"DateTime Service mock started at port: ${server.port()}")
    server
  }

}

trait BaseResponseTransformer extends ResponseTransformer {
  final def getSingle(getter: String => MultiValue, key: String): String = {
    val param = getter(key)
    if (param.isSingleValued) param.firstValue()
    else throw new IllegalArgumentException(s"Expecting exactly single value for '$key'")
  }

  def transform(request: Request): (Int, String)

  final override def transform(request: Request, responseDefinition: ResponseDefinition, files: FileSource): ResponseDefinition = {
    val (code, body) = transform(request)
    ResponseDefinitionBuilder
      .like(responseDefinition)
      .but()
      .withStatus(code)
      .withBody(body)
      .build()
  }

  final override def name(): String = getClass.getName

  final override def applyGlobally() = false
}

final class NewTokenResponseTransformer extends BaseResponseTransformer {
  override def transform(request: Request): (Int, String) = {
    val user = getSingle(request.queryParameter, "user")
    val pass = getSingle(request.queryParameter, "pass")
    if ((LocalHostTokenServer.USER, LocalHostTokenServer.PASS) == (user, pass)) {
      200 -> TokenResponse(token = LocalHostTokenServer.newToken()).toJson.compactPrint
    } else {
      401 -> ErrorResponse(error = "not authorized").toJson.compactPrint
    }
  }
}

final class RemoveTokenResponseTransformer extends BaseResponseTransformer {
  override def transform(request: Request): (Int, String) = {
    val token = getSingle(request.queryParameter, "token")
    LocalHostTokenServer.resetToken(token) match {
      case Some(newToken) =>
        200 -> TokenResponse(token = newToken).toJson.compactPrint
      case _ =>
        401 -> ErrorResponse(error = "not authorized").toJson.compactPrint
    }
  }
}

object DateTimeResponseTransformer {
  case class DateTimeResponse(datetime: String)

  implicit val dateTimeResponseFormat = jsonFormat1(DateTimeResponse.apply)
}

final class DateTimeResponseTransformer extends BaseResponseTransformer {
  import DateTimeResponseTransformer._

  override def transform(request: Request): (Int, String) = {
    val validOpt =
      Try(getSingle(request.header, "Authorization"))
        .toOption
        .flatMap(LocalHostTokenServer.tokenState)

    validOpt
      .fold(401 -> ErrorResponse(error = "not authorized").toJson.compactPrint) {
        case LocalHostTokenServer.TokenValid =>
          200 -> DateTimeResponse(datetime = DateTime.now.toIsoDateTimeString).toJson.compactPrint
        case LocalHostTokenServer.TokenExpired =>
          401 -> ErrorResponse(error = "token expired").toJson.compactPrint
      }
  }
}
