package examples

import scala.meta._
import scala.annotation.compileTimeOnly
import scala.collection.immutable
import scala.util.Random.nextLong
import com.typesafe.scalalogging.Logger
import examples.LogCommon.methodParams

sealed trait LogLevel {
  def apply(l: Logger, msg: String): Unit
}

case object Error extends LogLevel {def apply(l: Logger, msg: String): Unit = l.error(msg)}

case object Info extends LogLevel {def apply(l: Logger, msg: String): Unit = l.info(msg)}

case object Debug extends LogLevel {def apply(l: Logger, msg: String): Unit = l.debug(msg)}

object LogCommon {

  private def getTerm(arg: Term.Arg): Term = arg match {
    case Term.Arg.Repeated(v) => v
    case Term.Arg.Named(n, v) => getTerm(v)
    case v: Term => v
    case v => abort(s"@LogAsync unexpected Term.Arg: ${v.getClass }")
  }

  def thisParams(t: Tree): (Term, Term) = t match {
    case q"new $_($v, $v1)" => getTerm(v) -> getTerm(v1)
    case x => abort(s"@LogAsync shiot got $x")
  }

  private def methodParams(d: Defn.Def): Term = d.paramss.flatten.foldRight[Term](q"Nil")(
    (param, agr) => q"""(${param.name.syntax }  + "=" +  ${Term.Name(param.name.value) }) :: $agr"""
  )

  //TODO: figure out how to concatenate multi statement quasiquotes so the results are in the same block(..${someBlock.stats} does not work as expected)
  def apply(defn: Defn.Def, logLevel: Term, _logger: Term)(logAfter: Term): Term = {
    val q =
      q"""
    val methodName = ${defn.name.syntax }
    val cid = _root_.scala.util.Random.nextLong()
    val params = ${methodParams(defn) }.mkString(", ")
    $logLevel.apply(${_logger }, s"calling '$$methodName' with params: [$$params], correlationId=$$cid")
    val result = ${defn.body }
    $logAfter
    result
  """
    q
  }

}

class LogAsync(l: Logger, ll: LogLevel) extends scala.annotation.StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    defn match {
      case defn: Defn.Def =>
        val (_logger, logLevel) = LogCommon.thisParams(this)

        val body = LogCommon(defn, logLevel, _logger) {
          q"""result.onComplete{ r => $logLevel(${_logger }, s"result of method '$$methodName'with correlationId=$$cid : $$r")}"""
        }

        defn.copy(body = body)

      case _ => abort("@LogAsync must annotate a def")
    }
  }
}

class Log(l: Logger, ll: LogLevel) extends scala.annotation.StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    defn match {
      case defn: Defn.Def =>
        val (_logger, logLevel) = LogCommon.thisParams(this)

        val body = LogCommon(defn, logLevel, _logger) {
          q"""$logLevel(${_logger }, s"result of method '$$methodName' with correlationId=$$cid : $$result")"""
        }

        defn.copy(body = body)

      case _ => abort("@Log must annotate a def")
    }
  }
}