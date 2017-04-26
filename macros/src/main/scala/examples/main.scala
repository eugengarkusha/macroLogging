package examples

import scala.meta._
import scala.annotation.compileTimeOnly
import scala.collection.immutable
import scala.util.Random.nextLong
import com.typesafe.scalalogging.Logger
import scala.util.Try

object LogLevel extends Enumeration {
  val ERROR, WARN, INFO, DEBUG = Value
}

object LogCommon {

  private def methodParams(d: Defn.Def): Term = d.paramss.flatten.foldRight[Term](q"Nil")(
    (param, agr) => q"""(${param.name.syntax }  + "=" +  ${Term.Name(param.name.value) }) :: $agr"""
  )

  //returns (loggerVal, ( enabled, log)) Terms
  private def getIsEnabledAndLog(self: Tree): (Defn.Val, (Term, Term)) = {

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

    def _abort = abort(s"@LogAsync cannot read X.Value from ${level.syntax }. " +
      s"Use full or relative path as an argument to this annotation: (X.<value>) or  import X._; <value>")

    def toTerms(s: String): (Term, Term) = {
      Try(LogLevel.withName(s)).getOrElse(_abort) match {
        case LogLevel.DEBUG => q"logger_.underlying.isDebugEnabled" -> q"logger_.debug"
        case LogLevel.INFO => q"logger_.underlying.isInfoEnabled" -> q"logger_.info"
        case LogLevel.WARN => q"logger_.underlying.isWarnEnabled" -> q"logger_.warn"
        case LogLevel.ERROR => q"logger_.underlying.isErrorEnabled" -> q"logger_.error"
      }
    }

    //saving the $logger so in case if it is represented with builder function it is called only once
    q"val logger_ = $logger" -> (level match {
      case Term.Name(n) => toTerms(n)
      case Term.Select(x, n) => toTerms(n.syntax)
      case _ => _abort
    })
  }

  //TODO: figure out how to concatenate multi statement quasiquotes so the results are in the same block(..${someBlock.stats} does not work as expected)
  def apply(defn: Defn.Def, self: Tree)(logAfter: Term => Term): Term = {
    val (logVal, (enabled, log)) = getIsEnabledAndLog(self)
    q"""
     $logVal
     if($enabled){
         val methodName = ${defn.name.syntax }
         val cid = _root_.scala.util.Random.nextLong()
         val params = ${methodParams(defn) }.mkString(", ")
         $log(s"calling '$$methodName' with params: [$$params], correlationId=$$cid")
         val result = ${defn.body }
         ${logAfter(log) }
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