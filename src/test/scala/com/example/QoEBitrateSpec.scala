package com.example

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, MessageEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.Date

class QoEBitrateSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val qoeRegistry: ActorRef[QoERegistry.AddParams] = testKit.spawn(QoERegistry())
  lazy val routes: Route = new QoERoutes(qoeRegistry).qoeRoutes


  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "QoEBitrate" should {
    "simulate events for testing more than 2 bitrate changes within 10 seconds" in {

      val event0 = QoEParams("start", isFullscreen = false, 3385172, new Date().getTime)

      val request0 = Post("/qoe").withEntity(Marshal(event0).to[MessageEntity].futureValue)

      request0 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      val event1 = QoEParams("playbackbitratechanged", isFullscreen = false, 3385172, new Date().getTime)

      val request1 = Post("/qoe").withEntity(Marshal(event1).to[MessageEntity].futureValue)

      request1 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      Thread.sleep(2000)
      val event2 = QoEParams("playbackbitratechanged", isFullscreen = false, 3385182, new Date().getTime)

      val request2 = Post("/qoe").withEntity(Marshal(event2).to[MessageEntity].futureValue)

      request2 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      Thread.sleep(2000)
      val event3 = QoEParams("playbackbitratechanged", isFullscreen = false, 3385122, new Date().getTime)

      val request3 = Post("/qoe").withEntity(Marshal(event3).to[MessageEntity].futureValue)

      request3 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      Thread.sleep(2000)
      val event4 = QoEParams("playbackbitratechanged", isFullscreen = false, 3385192, new Date().getTime)

      val request4 = Post("/qoe").withEntity(Marshal(event4).to[MessageEntity].futureValue)

      request4 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":["the number of bitrate switches is higher than 2 every 10 secs."]}""")
      }

      Thread.sleep(12000)
      val event5 = QoEParams("playbackbitratechanged", isFullscreen = true, 3385192, new Date().getTime)

      val request5 = Post("/qoe").withEntity(Marshal(event5).to[MessageEntity].futureValue)

      request5 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
    }
  }
}
