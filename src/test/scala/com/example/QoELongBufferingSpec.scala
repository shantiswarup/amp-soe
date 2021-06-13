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

class QoELongBufferingSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val qoeRegistry: ActorRef[QoERegistry.AddParams] = testKit.spawn(QoERegistry())
  lazy val routes: Route = new QoERoutes(qoeRegistry).qoeRoutes


  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "QoELongBuffer" should {
    "return no warning for short buffering" in {

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

      Thread.sleep(200)

      val event2 = QoEParams("resume", isFullscreen = false, 3385172, new Date().getTime)

      val request2 = Post("/qoe").withEntity(Marshal(event2).to[MessageEntity].futureValue)

      request2 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }
    }

    "return warning for long buffering" in {
      val event0 = QoEParams("start", isFullscreen = false, 3385172, new Date().getTime)

      val request0 = Post("/qoe").withEntity(Marshal(event0).to[MessageEntity].futureValue)

      request0 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }

      val event = QoEParams("waiting", isFullscreen = false, 3385172, new Date().getTime)

      val request = Post("/qoe").withEntity(Marshal(event).to[MessageEntity].futureValue)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":[]}""")
      }

      Thread.sleep(1100)

      val event2 = QoEParams("resume", isFullscreen = false, 3385172, new Date().getTime)

      val request2 = Post("/qoe").withEntity(Marshal(event2).to[MessageEntity].futureValue)

      request2 ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"warnings":["buffering event longer than 1s"]}""")
      }
    }
  }
}
