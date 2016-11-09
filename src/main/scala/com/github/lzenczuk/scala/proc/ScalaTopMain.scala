package com.github.lzenczuk.scala.proc

import scala.sys.process._

/**
  * Created by dev on 09/11/16.
  */

case class OsProc(pid:Long, user:String, vMem:Long, rMem:Long, sMem:Long, cpu:Double, mem:Double, command:String)

object ScalaTopMain extends App {

  def stringToMem(s:String):Long = {
    s.last match {
      case 'k' => (s.dropRight(1).toDouble *1024).toLong
      case 'm' => (s.dropRight(1).toDouble*1048576).toLong
      case 'g' => (s.dropRight(1).toDouble*1073741824).toLong
      case 't' => (s.dropRight(1).toDouble*1099511627776L).toLong
      case _ => s.toLong
    }
  }

  private val lineStream: Stream[String] = "top -b".lineStream

  lineStream.map(_.trim).filter(l => l.matches("\\d+.*")).map(_.split("\\s+")).map(a => {
    OsProc(a(0).toLong, a(1), stringToMem(a(4)), stringToMem(a(5)), stringToMem(a(6)), a(8).toDouble, a(9).toDouble, a(11))
  }).foreach(println(_))
}
