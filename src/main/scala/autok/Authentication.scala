package autok

import scala.concurrent.Future

trait Authentication {
  def getToken: Future[Token]
  def tokenExpired(oldToken: Token): Unit
}


/*
Here is the test project description:



In order to access the REST services in your company's intranet, your appli=
cation needs to provide authorization token in the 'Authorization' header i=
n all its http requests.

To obtain the token, you'll need to implement a service which will automati=
cally get/refresh the token and confirm with the following contract:



type Token =3D String



trait Authentication {

    def getToken():Future[Token] // or throw exception if not authorized

    def tokenExpired(oldToken:Token):Unit // inform the service that the to=
ken has expired }



your service should:

1. Get the user/pass and auth-server host:port from config
2. Not block the calling thread
3. Allow concurrent access from different threads/actors
4. Keep only single token per application
5. Issue no more than one authentication request to the auth-server at a time



The auth-server is a REST service which implements this rather simplistic api:



1. To authorize at the first time:

POST /token?user=username&pass=password

     status:200, body: {"token":"716348726348"}

     status:401, body: {"error":"not authorized"}



2. When you call tokenExpired("oldToken"), your service should refresh the token:

POST /token/refresh?token=oldToken

     status:200, body: {"token":"some new token"}

     status:401, body: {"error":"not authorized"}



3. Your requests to other service (let's name it "datetime-service") will return:



curl -X GET "localhost:8021/datetime" -H "Authorization: mytoken"



     status: 200, body {"datetime":"2015-05-13 12:00:00"}

     status: 401, body {"error" : "token expired"} -- when the token has expired

     status:401, body: {"error":"not authorized"} -- when no token provided, or the token is invalid or not authorized





Your solution should:



1. Be managed with some project build tool: mvn, sbt, gradle etc
2. Contain sample app which uses the the Authentication trait.
3. Mock the auth-server and datetime-service
4. Contain all unit and integration tests to cover all happy/unhappy cases



Any frameworks are allowed but consider that your application already uses =
Scala, Akka, Spray, Typesafe Config, scalaj.http, WireMock, Specs2/JUnit, J=
ackson/SprayJson/PlayJson etc



In result we expect zipped project or link to github with source code.
*/
