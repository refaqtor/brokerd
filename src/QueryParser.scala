package com.paulasmuth.fyrehose

class QueryParser {

  var query  : Query = null
  val lexer  = new QueryLexer(this)

  def parse(bdy: QueryBody) = {

    for (pos <- new Range(0, bdy.raw.size - 1, 1))
      lexer.next(bdy.raw(pos).toChar)

    lexer.finish

    if (query == null)
      throw new ParseException("query must contain one of stream, info, etc.")

    query
  }


  def next(token: FQL_TOKEN) : Unit = token match {

    case t: FQL_STREAM  =>
      if (query == null)
        query = eval_query(t)
      else
        unexpected_token(t, "query to only contain one of stream, info, etc.")

    case t: FQL_TOKEN =>
      if (query == null)
        unexpected_token(t, "query to start with stream, info, etc.")
      else
        eval_token(t)

  }


  private def eval_query(token: FQL_TOKEN) : Query = token match {

    case t: FQL_STREAM =>
      new StreamQuery()

  }


  private def eval_token(token: FQL_TOKEN) = token match {

    case t: FQL_OR =>
      query.fstack = query.fstack.or()

    case t: FQL_AND =>
      query.fstack = query.fstack.and()

    case t: FQL_SINCE =>
      query.since = eval_time(t.get)

    case t: FQL_UNTIL =>
      query.until = eval_time(t.get)

    case t: FQL_WHERE => t.left match {

      case k: FQL_KEY =>
        query.fstack.push(k)(
          if (t.not unary_!) eval_where(k, t)
          else negate(eval_where(k, t)))

      case _ =>
        unexpected_token(t.left.asInstanceOf[FQL_TOKEN],
          "left hand operator of a where clause to be a FQL_KEY")

    }

    case _ => try { query.eval(token) } catch {

      case e: ParseException =>
        unexpected_token(token, e.toString)

    }

  }


  private def eval_where(key: FQL_KEY, token: FQL_WHERE) = token.op match {

    case o: FQL_OPERATOR_EQUALS => token.right match {

      case v: FQL_STRING =>
        (m: Message) => m.getAsString(key) == v.get

      case v: FQL_KEY =>
        (m: Message) => m.getAsString(key) == m.getAsString(v)

      case v: FQL_INTEGER =>
        (m: Message) => m.getAsInteger(key) == v.get

      case v: FQL_FLOAT =>
        (m: Message) => m.getAsDouble(key) == v.get

      case v: FQL_BOOL =>
        (m: Message) => m.getAsBoolean(key) == v.get

      case _ =>
        unexpected_token(token.right.asInstanceOf[FQL_TOKEN], "FQL_VAL")

    }

    case _ =>
      unexpected_token(token.op.asInstanceOf[FQL_TOKEN], "FQL_OPERATOR")

  }


  private def eval_time(t: FQL_TOKEN) : FQL_TVALUE = t match {
    case tv: FQL_TVALUE => tv
    case _ => throw new ParseException("invalid time: " + t.buf)
  }


  private def negate(lambda: Message => Boolean) =
    (m: Message) => lambda(m) unary_!


  private def unexpected_token(found: FQL_TOKEN, expected: String) =
    throw new ParseException("unexpected token: " +  found.getClass.getName
      .replaceAll("[^A-Z_]", "") + ", expected: " + expected)


}
