package autok

import autok.authentication.{ Authentication, SimpleAuthentication, StubAuthServerConfig }
import autok.mocks.LocalHostTokenServer
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class AuthenticationSpec extends Specification with AfterAll {
  implicit val executionEnv = ExecutionEnv.fromGlobalExecutionContext

  val auth = LocalHostTokenServer.startAuthServer()

  def createAuthService(user: String, pass: String): Authentication =
    new SimpleAuthentication(
      StubAuthServerConfig(
        port = auth.port(),
        username = user,
        password = pass))

  override def afterAll(): Unit = {
    auth.shutdown()
  }

  "Authentication" should {
    "fail on wrong username/password" in {
      createAuthService("wrong", "wrong").getToken must
        throwAn[IllegalArgumentException]("not authorized").await
    }

    "fail on wrong username" in {
      createAuthService("wrong", LocalHostTokenServer.PASS).getToken must
        throwAn[IllegalArgumentException]("not authorized").await
    }

    "fail on wrong password" in {
      createAuthService(LocalHostTokenServer.USER, "wrong").getToken must
        throwAn[IllegalArgumentException]("not authorized").await
    }

    "return valid token using correct username/password" in {
      createAuthService(LocalHostTokenServer.USER, LocalHostTokenServer.PASS)
        .getToken
        .map(LocalHostTokenServer.tokenState) must
        beSome[LocalHostTokenServer.TokenState](LocalHostTokenServer.TokenValid).await
    }

    "refresh token just once" in {
      val service = createAuthService(LocalHostTokenServer.USER, LocalHostTokenServer.PASS)

      val token = Try(Await.result(service.getToken, 1.second))
      token must beSuccessfulTry
        .updateMessage("expecting token for this case: ".+)

      for (token <- token) {
        service.tokenExpired(token) must not(throwAn[Exception])
          .updateMessage("first time invalidation should succeed: ".+)

        service.tokenExpired(token) must throwAn[IllegalArgumentException]("not authorized")
          .updateMessage("second time invalidation should fail: ".+)
      }

      ok
    }
  }
}
