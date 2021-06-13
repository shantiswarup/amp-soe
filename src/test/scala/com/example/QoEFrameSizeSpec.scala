package com.example

//#user-routes-spec
//#test-top

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.Date

//#set-up
class QoEFrameSizeSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val qoeRegistry: ActorRef[QoERegistry.AddParams] = testKit.spawn(QoERegistry())
  lazy val routes: Route = new QoERoutes(qoeRegistry).qoeRoutes


  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "QoEFrameSize" should {
    "return warnings for low bitrate in full screen mode" in {
      val event0 = QoEParams("start", isFullscreen = false, 3385172, new Date().getTime)

      val request0 = Post("/qoe").withEntity(Marshal(event0).to[MessageEntity].futureValue)

      request0 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      val event = QoEParams("playbackbitratechanged", isFullscreen = true, 100, new Date().getTime)

      val request = Post("/qoe").withEntity(Marshal(event).to[MessageEntity].futureValue)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":["bitrate chosen by the player is meant for a smaller player frame size"]}""")
      }
    }

    "return no warning for high bitrate in full screen" in {
      val event = QoEParams("playbackbitratechanged", isFullscreen = true, 3385172, new Date().getTime)

      val request = Post("/qoe").withEntity(Marshal(event).to[MessageEntity].futureValue)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
    }
  }
}
