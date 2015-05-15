package autok

import autok.authentication.{ Authentication, SimpleAuthentication, StubAuthServerConfig }
import autok.mocks.{ LocalHostTokenServer => Mock }
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class AuthenticationSpec extends Specification with AfterAll {
  implicit val executionEnv = ExecutionEnv.fromGlobalExecutionContext

  val auth = Mock.startAuthServer()

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
      createAuthService("wrong", Mock.PASS).getToken must
        throwAn[IllegalArgumentException]("not authorized").await
    }

    "fail on wrong password" in {
      createAuthService(Mock.USER, "wrong").getToken must
        throwAn[IllegalArgumentException]("not authorized").await
    }

    "return valid token using correct username/password" in {
      val service = createAuthService(Mock.USER, Mock.PASS)
      val token1 = Try(Await.result(service.getToken, 1.second))

      token1.map(Mock.tokenState) must beSuccessfulTry(beSome[Mock.TokenState](Mock.TokenValid))
        .updateMessage("expecting existing and valid token: ".+)

      for (token1 <- token1) {
        service.getToken must beEqualTo(token1).await
          .updateMessage("service returns the same token".+)

        Mock.invalidateToken(token1) must beSome[Mock.TokenState](Mock.TokenValid)
          .updateMessage("simulate token invalidation".+)

        service.tokenExpired("garbage") must not(throwAn[Exception])
          .updateMessage("invalidation always succeeds: ".+)

        service.getToken must beEqualTo(token1).await
          .updateMessage("service returns the same token even after incorrect invalidation".+)

        service.tokenExpired(token1) must not(throwAn[Exception])
          .updateMessage("invalidation always succeeds: ".+)

        val token2 = Try(Await.result(service.getToken, 1.second))

        token2.map(Mock.tokenState) must beSuccessfulTry(beSome[Mock.TokenState](Mock.TokenValid))
          .updateMessage("after invalidation expecting valid token: ".+)

        token2 must beSuccessfulTry(not(beEqualTo(token1)))
          .updateMessage("invalidation must reset token: ".+)
      }

      ok
    }
  }
}
