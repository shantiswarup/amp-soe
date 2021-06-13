package com.example

import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import com.example.QoERegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

//#import-json-formats
//#user-routes-class
class QoERoutes(qoERegistry: ActorRef[QoERegistry.AddParams])(implicit val system: ActorSystem[_]) {

  //#user-routes-class

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def addParams(qoEParams: QoEParams): Future[QoERegistry.QoEResponse] = {
    system.log.info(s"Got event $qoEParams")
    qoERegistry.ask(AddParams(qoEParams, _))
  }

  val settings = CorsSettings.defaultSettings.withAllowGenericHttpRequests(true)
  val qoeRoutes: Route =
    cors(settings) {
      pathPrefix("qoe") {
        pathEnd {
          post {
            entity(as[QoEParams]) {
              qoeParams =>
                onSuccess(addParams(qoeParams)) {
                  resp => complete(StatusCodes.OK, resp)
                }
            }
          }
        }
      }
    }
}
