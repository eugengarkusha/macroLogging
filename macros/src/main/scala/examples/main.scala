package examples

import scala.meta._
import scala.annotation.compileTimeOnly
import scala.collection.immutable
import scala.util.Random.nextLong
import com.typesafe.scalalogging.Logger
import scala.util.Try

object LogLevel extends Enumeration{
  val ERROR, WARN, INFO ,DEBUG = Value
}

object LogCommon {

  private def methodParams(d: Defn.Def): Term = d.paramss.flatten.foldRight[Term](q"Nil")(
    (param, agr) => q"""(${param.name.syntax }  + "=" +  ${Term.Name(param.name.value) }) :: $agr"""
  )

  private def getIsEnabledAndLog(self: Tree): (Term, Term) = {

    //getting annotation constructor params
    val (logger, level) = {

      def getTerm(arg: Term.Arg): Term = arg match {
        case Term.Arg.Repeated(v) => v
        case Term.Arg.Named(n, v) => getTerm(v)
        case v: Term => v
        case v => abort(s"@LogAsync unexpected Term.Arg: ${v.getClass }")
      }

      self match {
        case q"new $_($v, $v1)" => getTerm(v) -> getTerm(v1)
        case x => abort(s"@LogAsync unexpected params: got $x")
      }
    }

    def _abort = abort(s"@LogAsync cannot read X.Value from ${level.syntax}. " +
      s"Use full or relative path as an argument to this annotation: (X.<value>) or  import X._; <value>")

    def toTerms(s: String) = {
      Try(LogLevel.withName(s)).getOrElse(_abort) match {
        case LogLevel.DEBUG => q"""$logger.underlying.isDebugEnabled""" -> q"""$logger.debug"""
        case LogLevel.INFO => q"""$logger.underlying.isInfoEnabled""" -> q"""$logger.info"""
        case LogLevel.WARN => q"""$logger.underlying.isWarnEnabled""" -> q"""$logger.warn"""
        case LogLevel.ERROR => q"""$logger.underlying.isErrorEnabled""" -> q"""$logger.error"""
      }
    }

    level match {
      case Term.Name(n) => toTerms(n)
      case Term.Select(x, n) => toTerms(n.syntax)
      case _ => _abort
    }
  }

  //TODO: figure out how to concatenate multi statement quasiquotes so the results are in the same block(..${someBlock.stats} does not work as expected)
  def apply(defn: Defn.Def, self: Tree)(logAfter: Term => Term): Term = {
    val (enabled, log) = getIsEnabledAndLog(self)
      q"""
     if($enabled){
         val methodName = ${defn.name.syntax }
         val cid = _root_.scala.util.Random.nextLong()
         val params = ${methodParams(defn) }.mkString(", ")
         $log(s"calling '$$methodName' with params: [$$params], correlationId=$$cid")
         val result = ${defn.body }
         ${logAfter(log)}
         result
     } else {${defn.body }}
  """
  }

}

class LogAsync(l: Logger, ll: LogLevel.Value) extends scala.annotation.StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    defn match {
      case defn: Defn.Def =>

        val body = LogCommon(defn, this) { log =>
          q"""result.onComplete{ r => $log(s"result of method '$$methodName'with correlationId=$$cid : $$r")}"""
        }
        defn.copy(body = body)

      case _ => abort("@LogAsync must annotate a def")
    }
  }
}

class Log(l: Logger, ll: LogLevel.Value) extends scala.annotation.StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    defn match {
      case defn: Defn.Def =>

        val body = LogCommon(defn, this) { log =>
          q"""$log(s"result of method '$$methodName' with correlationId=$$cid : $$result")"""
        }

        defn.copy(body = body)

      case _ => abort("@Log must annotate a def")
    }
  }
}