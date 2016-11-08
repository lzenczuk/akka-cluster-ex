package com.github.lzenczuk.scala.akkaclusterex

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
/**
  * Created by dev on 08/11/16.
  */

object HttpCrawler {
  case object HCStatusRequest
  case object HCExceptionRequest
  case object HCShutdownRequest
  case class HCReadyStatus(id:Long)
  case class HCStopStatus(id:Long)
}

class HttpCrawler extends Actor with ActorLogging {

  val id = System.nanoTime()

  val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit = {
    log.info(s"HttpCrawler $id started")
    mediator ! Subscribe("http-crawler", self)
    mediator ! Publish("crawler-requests-dispatcher", HttpCrawler.HCReadyStatus(id))
  }


  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    mediator ! Publish("crawler-requests-dispatcher", HttpCrawler.HCStopStatus(id))
  }

  @scala.throws[Exception](classOf[Exception])
  override def postRestart(reason: Throwable): Unit = {
    mediator ! Publish("crawler-requests-dispatcher", HttpCrawler.HCReadyStatus(id))
  }


  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    mediator ! Publish("crawler-requests-dispatcher", HttpCrawler.HCStopStatus(id))
  }

  def receive = {
    case HttpCrawler.HCStatusRequest =>
      log.info("HttpCrawler receive status request.")
      mediator ! Publish("crawler-requests-dispatcher", HttpCrawler.HCReadyStatus(id))
    case HttpCrawler.HCExceptionRequest =>
      log.info("HttpCrawler receive exception request.")
      throw new RuntimeException("HttpCrawler exception")
    case HttpCrawler.HCShutdownRequest =>
      log.info("HttpCrawler receive shutdown request.")
      context.stop(self)
  }
}

object CrawlerRequestDispatcher {
  case object CRDStatusRequest
  case object CRDExceptionRequest
}

class CrawlerRequestDispatcher extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator

  var crawlers = List[Long]()

  override def preStart(): Unit = {
    log.info(s"CrawlerRequestDispatcher started")
    mediator ! Subscribe("crawler-requests-dispatcher", self)
    mediator ! Publish("http-crawler", HttpCrawler.HCStatusRequest)
  }


  @scala.throws[Exception](classOf[Exception])
  override def postRestart(reason: Throwable): Unit = {
    mediator ! Publish("http-crawler", HttpCrawler.HCStatusRequest)
  }

  import HttpCrawler._
  import CrawlerRequestDispatcher._

  def receive = {
    case HCReadyStatus(id) =>
      crawlers = id::crawlers
      log.info(s"Crawlers: $crawlers")
    case HCStopStatus(id) =>
      crawlers = crawlers diff List(id)
      log.info(s"Crawlers: $crawlers")
    case CRDStatusRequest =>
      log.info(s"Crawlers: $crawlers")
    case CRDExceptionRequest =>
      throw new RuntimeException("CrawlerRequestDispatcher exception")
  }
}

object PubSubMain extends App{

  private val system: ActorSystem = ActorSystem("pubsub-as")

  var dispatcher: ActorRef = null
  var hc1:ActorRef = null
  var hc2:ActorRef = null
  var hc3:ActorRef = null

  system.scheduler.scheduleOnce(2 seconds){ hc1 = system.actorOf(Props[HttpCrawler]) }
  system.scheduler.scheduleOnce(4 seconds){ hc2 = system.actorOf(Props[HttpCrawler]) }
  system.scheduler.scheduleOnce(5 seconds){ dispatcher = system.actorOf(Props[CrawlerRequestDispatcher]) }
  system.scheduler.scheduleOnce(6 seconds){ hc3 = system.actorOf(Props[HttpCrawler]) }
  system.scheduler.scheduleOnce(8 seconds){ hc1 ! HttpCrawler.HCExceptionRequest }
  system.scheduler.scheduleOnce(10 seconds){ hc1 ! HttpCrawler.HCShutdownRequest }
  system.scheduler.scheduleOnce(12 seconds){ dispatcher ! CrawlerRequestDispatcher.CRDExceptionRequest }

  system.scheduler.scheduleOnce(14 seconds){ system.terminate() }
}
