package com.github.lzenczuk.scala.akkaclusterex.cluster

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.Cluster

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by dev on 10/11/16.
  */

object ClusterStatusActor{
  case object ClusterStatusRequest
}

class ClusterStatusActor extends Actor with ActorLogging {
  import ClusterStatusActor._

  val cluster = Cluster(context.system)

  def receive = {
    case ClusterStatusRequest =>
      log.info(s"Number of cluster members: ${cluster.state.members.size}")

      val leader = cluster.state.leader.map(address => {
        if(address==cluster.selfAddress) "Leader: me"
        else s"Leader: $address"
      }).getOrElse("Leader: none")

      log.info(s"${leader}")

  }
}

object ClusterMonitoringMain extends App{
  private val system: ActorSystem = ActorSystem("cluster-monitoring-as")

  private val csar: ActorRef = system.actorOf(Props[ClusterStatusActor])

  system.scheduler.schedule(0 second, 5 seconds){
    csar ! ClusterStatusActor.ClusterStatusRequest
  }
}
