package com.example

//#user-registry-actor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import java.util.Date
import java.util.concurrent.TimeUnit

//#user-case-classes
final case class Buffer(start: Int, end: Int)

final case class QoEParams(eventType: String, isFullscreen: Boolean, bitrate: Int, timestamp: Long)

final case class VideoState()

//#user-case-classes
case class QoERegistry()

object QoERegistry {
  // actor protocol
  final val WAITING = "waiting"
  final val RESUME = "resume"
  final val ENDED = "ended"
  final val START = "start"
  final val PLAYBACK_BITRATE_CHANGED = "playbackbitratechanged"


  final case class QoEResponse(warnings: List[String])

  final case class AddParams(qoEParams: QoEParams, replyTo: ActorRef[QoEResponse])

  def apply(): Behavior[AddParams] = registry(Nil)

  private def timeBetweenEventsInMillis(e1: QoEParams, e2: QoEParams): Long = {
    val d1 = new Date(e1.timestamp)
    val d2 = new Date(e2.timestamp)
    val diff = d1.getTime - d2.getTime
    TimeUnit.MILLISECONDS.toMillis(diff)
  }

  private def isEventsInSeconds(curr: QoEParams, events: List[QoEParams], seconds: Long = 10000): Boolean = {
    events.forall(timeBetweenEventsInMillis(curr, _) < seconds)
  }


  private def registry(states: List[QoEParams]): Behavior[AddParams] = {
    def eval(qoEParams: QoEParams): List[String] = {
      def evalLongBuffering: Option[String] = {
        if (qoEParams.eventType == RESUME) {
          val lastWaiting: QoEParams = states.find(_.eventType == WAITING).get
          if (timeBetweenEventsInMillis(qoEParams, lastWaiting) > 1000) {
            Some("buffering event longer than 1s")
          } else None
        } else None
      }

      def evalFrequentBuffering: Option[String] = {
        if (qoEParams.eventType == RESUME) {
          val lastWaiting = states.takeWhile(_.eventType == WAITING).last
          if (timeBetweenEventsInMillis(qoEParams, lastWaiting) > 500) {
            val bufferEvents: List[QoEParams] = states
              .filter(state => state.eventType == WAITING || state.eventType == RESUME).take(5).sortBy(_.timestamp).reverse
            if (bufferEvents.length > 4) {
              val timeBetween: Iterator[Long] = bufferEvents.drop(1).grouped(2).map(x => timeBetweenEventsInMillis(x.head, x.last))
              if (timeBetween.forall(_ > 500) && timeBetweenEventsInMillis(qoEParams, bufferEvents.last) <= 30000) {
                Some("number of buffering events longer than 500ms is higher than 3 per 30 secs")
              } else None
            } else None
          } else None
        } else None
      }

      def evalBitrateChange: Option[String] = {
        val bitrateChangeEvents: List[QoEParams] = states
          .filter(_.eventType == PLAYBACK_BITRATE_CHANGED)
          .sortBy(_.timestamp).reverse
          .take(3)
        if (bitrateChangeEvents.nonEmpty &&
          bitrateChangeEvents.length > 2 &&
          isEventsInSeconds(qoEParams, bitrateChangeEvents))
          Some("the number of bitrate switches is higher than 2 every 10 secs.")
        else None
      }

      def evalFrameBitrateMismatch: Option[String] = {
        if (qoEParams.isFullscreen && qoEParams.bitrate <= 3385171) {
          Some("bitrate chosen by the player is meant for a smaller player frame size")
        } else None
      }

      List(evalFrequentBuffering, evalLongBuffering, evalFrameBitrateMismatch, evalBitrateChange).flatten
    }

    Behaviors.receiveMessage {
      case AddParams(qoEParams, replyTo) =>
        if (qoEParams.eventType == ENDED || qoEParams.eventType == START) {
          replyTo ! QoEResponse(Nil)
          registry(Nil)
        } else {
          val warnings = eval(qoEParams)
          replyTo ! QoEResponse(warnings)
          registry(qoEParams :: states)
        }
    }
  }


}
//#user-registry-actor
