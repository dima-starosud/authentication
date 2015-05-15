package autok.authentication

import scala.concurrent.Future

trait Authentication {
  def getToken: Future[Token]
  def tokenExpired(oldToken: Token): Unit
}
