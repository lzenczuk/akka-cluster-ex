package com.github.lzenczuk.scala.akkaclusterex.cluster

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Address, Props}
import akka.pattern.ask
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by dev on 11/11/16.
  */

// not used here. Based on MemberStatus, example of enum.
sealed abstract class SomeState

object SomeState{
  case object Starting extends SomeState
  case object Running extends SomeState
  case object Stopping extends SomeState
  case object Stoped extends SomeState
}

object NodeManagerActor {

  case class JoinCluster(systemName: String, host: String, port: Int)

  case object LeaveCluster

  case object NodeStatusRequest

  case class NodeStatus(nodeAddress: Address, clusterLeader: Option[Address], numberOfMembers: Int)

}

class NodeManagerActor extends Actor with ActorLogging {

  import NodeManagerActor._

  private val cluster: Cluster = Cluster(context.system)

  def receive = {
    case JoinCluster(systemName, host, port) =>
      val clusterAddress: Address = Address("akka.tcp", systemName, host, port)
      cluster.join(clusterAddress)
      log.info(s"Send request to join cluster: $clusterAddress")
    case NodeStatusRequest =>
      sender ! NodeStatus(cluster.selfAddress, cluster.state.leader, cluster.state.members.size)
  }
}

object ClusterManualJoinMain {

  def getNodeManagerActor(system: ActorSystem): ActorRef = {
    system.actorOf(Props[NodeManagerActor])
  }

  def getRestEndpoint(port:Int, nodeManagerActorRef: ActorRef, system: ActorSystem, materializer: ActorMaterializer) = {

    // TODO - It is ugly. Check implicit method parameters.
    implicit val s = system
    implicit val m = materializer

    val route =
      path("clusterStatus") {
        get {
          complete {
            implicit val timeout = Timeout(5 seconds)

            (nodeManagerActorRef ? NodeManagerActor.NodeStatusRequest)
              .mapTo[NodeManagerActor.NodeStatus]
              .map(ns => s"Node address: ${ns.nodeAddress}, leader address: ${ns.clusterLeader}, number of cluster members: ${ns.numberOfMembers}")
          }
        }
      } ~
    path("joinCluster"){
      get{
        parameters('system.as[String], 'host.as[String], 'port.as[Int]) {
          (clusterSystem, address, port) => {
            nodeManagerActorRef ! NodeManagerActor.JoinCluster(clusterSystem, address, port)
            complete("OK")
          }
        }
      }
    }

    Http().bindAndHandle(route, "localhost", port)
  }

  /**
    * This class will start cluster node with http endpoints: clusterStatus and joinCluster
    *
    * To create cluster from two nodes:
    * 1. Check status of first node : http://localhost:8081/clusterStatus
    * 2. Join first node to it self, to initialize cluster: http://localhost:8081/joinCluster?system=cluster-manual-join-as&host=127.0.0.1&port=2551
    * 3. Check status of second node: http://localhost:8082/clusterStatus
    * 4. Join second node to first: http://localhost:8082/joinCluster?system=cluster-manual-join-as&host=127.0.0.1&port=2551
    * 5. Check status of third node: http://localhost:8083/clusterStatus
    * 6. Join third node to first or second to join cluster:
    *    http://localhost:8083/joinCluster?system=cluster-manual-join-as&host=127.0.0.1&port=2551
    *    http://localhost:8083/joinCluster?system=cluster-manual-join-as&host=127.0.0.1&port=2552
    *
    */
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("cluster-manual-join-as")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val config = ConfigFactory.load()

    val nodeManagerActor: ActorRef = getNodeManagerActor(system)
    val future: Future[ServerBinding] = getRestEndpoint(config.getInt("http.endpoint.port"), nodeManagerActor, system, materializer)
  }

}
