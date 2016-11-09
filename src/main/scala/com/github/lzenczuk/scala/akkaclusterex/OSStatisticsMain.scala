package com.github.lzenczuk.scala.akkaclusterex

import java.lang.management.ManagementFactory

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.sun.management.OperatingSystemMXBean

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by dev on 09/11/16.
  */

object OSStatsActor {
  case object CpuUsageRequest
  case object MemoryUsageRequest

  /**
    * Cpu usage. Values are between 0 and 1 or -1 when not implemented.
    * @param app - amount of computing power used by application it self
    * @param system - total usage of all cpu in system
    */
  case class CpuUsage(app:Double, system:Double)

  /**
    * Memory in bytes.
    * @param totalMemory - available physical memory
    * @param freeMemory - free physical memory
    */
  case class MemoryUsage(totalMemory:Long, freeMemory:Long)
}

class OSStatsActor extends Actor with ActorLogging {

  val osmxBean:OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean().asInstanceOf[OperatingSystemMXBean]

  import OSStatsActor._

  def receive = {
    case CpuUsageRequest =>
      log.info(s"Cpu: ${CpuUsage(osmxBean.getProcessCpuLoad, osmxBean.getSystemCpuLoad)}")
    case MemoryUsageRequest =>
      log.info(s"Memory: ${MemoryUsage(osmxBean.getTotalPhysicalMemorySize, osmxBean.getFreePhysicalMemorySize)}")
  }
}

object OSStatisticsMain extends App{

  private val system: ActorSystem = ActorSystem("os-statistics-as")

  private val osStatsActorRef: ActorRef = system.actorOf(Props[OSStatsActor])

  system.scheduler.schedule(0 seconds, 1 second){
    osStatsActorRef ! OSStatsActor.CpuUsageRequest
    osStatsActorRef ! OSStatsActor.MemoryUsageRequest
  }

  system.scheduler.scheduleOnce(10 seconds){
    system.terminate()
  }

}
