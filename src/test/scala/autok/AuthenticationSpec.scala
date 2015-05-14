package autok

import autok.authentication.{ Authentication, SimpleAuthentication, StubAuthServerConfig }
import autok.mocks.LocalHostTokenServer
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

class AuthenticationSpec extends Specification with AfterAll {
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
    "calculate correct value for request" in {
      ok
    }

  }
}
