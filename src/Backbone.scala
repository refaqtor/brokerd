package com.paulasmuth.fyrehose

import java.util.concurrent._
import java.util.concurrent.atomic._
import scala.actors.Actor
import scala.actors.Actor._

case class MessageBody(raw: Array[Byte])

class Backbone() extends Actor{

  val queries = scala.collection.mutable.Set[Query]()
  var sequence = 0

  var msg_total = new AtomicInteger

  def act() = {
    Actor.loop{ receive{
      case query: Query => execute(query)
      case msg: Message => dispatch(msg)
      case QueryExitSig(query) => finish(query)
    }}
  }

  private def dispatch(msg: Message) = {
    msg_total.incrementAndGet

    if (msg.exists(List("_volatile")) unary_!) {
      sequence += 1
      msg.sequence = sequence

      Fyrehose.message_index ! msg
      Fyrehose.message_cache ! msg
    }

    msg.sequence = sequence

    if (Fyrehose.writer != null)
      Fyrehose.writer ! msg

    queries.foreach(_ ! msg)
  }


  private def execute(query: Query) = {
    query.sequence = sequence
    query.start()

    query ! QueryReadySig

    query.until match {

      case t: FQL_TSTREAM =>
        queries += query

      case _ => ()

    }
  }


  private def finish(query: Query) : Unit = {
    if (query == null)
      return ()

    queries -= query
    query ! HangupSig
  }


  override def exceptionHandler = {
    case e: Exception => Fyrehose.exception(e, true)
  }

}
