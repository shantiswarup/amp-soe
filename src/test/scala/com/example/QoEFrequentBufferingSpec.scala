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

class QoEFrequentBufferingSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest{
  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val qoeRegistry: ActorRef[QoERegistry.AddParams] = testKit.spawn(QoERegistry())
  lazy val routes: Route = new QoERoutes(qoeRegistry).qoeRoutes


  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "QoEFrequentBuffering" should {
    "simulate events for testing more than 3 buffering greater than 500 ms within 30 seconds" in {

      val event0 = QoEParams("start", isFullscreen = false, 3385172, new Date().getTime)

      val request0 = Post("/qoe").withEntity(Marshal(event0).to[MessageEntity].futureValue)

      request0 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }

      val event1 = QoEParams("waiting", isFullscreen = false, 3385172, new Date().getTime)

      val request1 = Post("/qoe").withEntity(Marshal(event1).to[MessageEntity].futureValue)

      request1 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      Thread.sleep(900)
      val event2 = QoEParams("resume", isFullscreen = false, 3385172, new Date().getTime)

      val request2 = Post("/qoe").withEntity(Marshal(event2).to[MessageEntity].futureValue)

      request2 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      Thread.sleep(900)
      val event3 = QoEParams("waiting", isFullscreen = false, 3385172, new Date().getTime)

      val request3 = Post("/qoe").withEntity(Marshal(event3).to[MessageEntity].futureValue)

      request3 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      Thread.sleep(900)
      val event4 = QoEParams("resume", isFullscreen = false, 3385172, new Date().getTime)

      val request4 = Post("/qoe").withEntity(Marshal(event4).to[MessageEntity].futureValue)

      request4 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }

      Thread.sleep(900)
      val event5 = QoEParams("waiting", isFullscreen = true, 3385192, new Date().getTime)

      val request5 = Post("/qoe").withEntity(Marshal(event5).to[MessageEntity].futureValue)

      request5 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
      Thread.sleep(900)
      val event6 = QoEParams("resume", isFullscreen = true, 3385192, new Date().getTime)

      val request6= Post("/qoe").withEntity(Marshal(event6).to[MessageEntity].futureValue)

      request6 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":["number of buffering events longer than 500ms is higher than 3 per 30 secs"]}""")
      }
    }
  }

}
