package com.github.lzenczuk.scala.akkaclusterex

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by dev on 08/11/16.
  */

class ActorA extends Actor with ActorLogging{

  val id = System.nanoTime()

  def receive = {
    case 0 => log.info(s"ActorA $id receive 0. Do nothing.")
    case 1 => log.info(s"ActorA $id receive 1. Throw exception.")
      throw new RuntimeException(s"ActorA $id exception")
    case 2 => log.info(s"ActorA $id receive 2. Terminate.")
      context.stop(self)
  }

  override def preStart(): Unit = {
    log.info(s"ActorA $id preStart")
  }

  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info(s"ActorA $id preRestart. Exception: $reason. Message: $message")
  }

  @scala.throws[Exception](classOf[Exception])
  override def postRestart(reason: Throwable): Unit = {
    log.info(s"ActorA $id postRestart. Exception: $reason")
  }
}

object ActorB {
  case class ActorToWatch(ref: ActorRef)
}

class ActorB extends Actor with ActorLogging {

  import ActorB._

  def receive = {
    case ActorToWatch(ref) =>
      log.info(s"ActorB receive ref to ActorA to watch: $ref")
      context.watch(ref)
    case n => log.info(s"ActorB receive message $n")
  }
}

object LifecycleMain extends App{

  private val system: ActorSystem = ActorSystem("lifecycle-as")

  private val actorA0Ref: ActorRef = system.actorOf(Props[ActorA], "actorA0")
  private val actorB0Ref: ActorRef = system.actorOf(Props[ActorB], "actorB0")

  actorB0Ref ! ActorB.ActorToWatch(actorA0Ref)

  system.scheduler.scheduleOnce(50 millis, actorA0Ref, 0)
  system.scheduler.scheduleOnce(100 millis, actorA0Ref, 1)
  system.scheduler.scheduleOnce(150 millis, actorA0Ref, 0)
  system.scheduler.scheduleOnce(200 millis, actorA0Ref, 2)
  system.scheduler.scheduleOnce(250 millis, actorA0Ref, 0)

}
