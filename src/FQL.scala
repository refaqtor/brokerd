package com.paulasmuth.fyrehose;

trait FQL_TOKEN {
  def buffer(cur: Char, buf: String) : String
  def ready : Boolean
  def next(cur: Char, buf: String) : FQL_TOKEN
}

trait FQL_MTOKEN extends FQL_TOKEN {
  var cur : Char = 0
  var buf : String = null
  def buffer(cur: Char, buf: String) : String =
    if (ready) "" else buf + cur
  def next(_cur: Char, _buf: String) : FQL_TOKEN =
    { cur=_cur; buf = _buf; next }
  def next : FQL_TOKEN
}

class FQL_ATOM extends FQL_MTOKEN {
  def ready =
    (cur == ' ') || (cur == '(')
  def next =
    if ((cur != ' ') && (cur != '('))
      this
    else buf match {
      case "stream"    => new FQL_STREAM
      case "where"     => new FQL_WHERE(true)
      case "where_not" => new FQL_WHERE(false)
    }
}

class FQL_STREAM extends FQL_MTOKEN {
  def ready = true
  def next = this
}

class FQL_WHERE(negated: Boolean) extends FQL_MTOKEN {
  var key : String = null
  var value : String = null

  def ready = false

  def next =
    if (key == null){ key = "fooabr"; this }
    else this
}