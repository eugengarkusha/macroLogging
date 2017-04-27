package examples

import java.util.Date

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.Logger


object MyApp extends App {

  val logger = Logger("NEWLOGGER")

  @LogAsync(logger, LogLevel.ERROR)
  def someMethodAsync(a: Int, b: String, x: Date): Future[Int] = {
    Future(a + 2)
  }

  someMethodAsync(35, "someParam", new Date())

//prints:
//00:17:13.627 [run-main-10] ERROR NEWLOGGER - calling 'someMethodAsync' with params: [a=35, b=someParam, x=Thu Apr 27 00:17:13 CEST 2017], correlationId=-1638205168221967782
//00:17:13.630 [ForkJoinPool-1-worker-13] ERROR NEWLOGGER - result of method 'someMethodAsync'with correlationId=-1638205168221967782 : Success(37)

  import LogLevel._

  def getLogger = {
   println("creating logger")
   val l =  Logger("NEWLOGGER")
   l
  }



  @Log(getLogger, DEBUG)
  def someMethodSync(a: Int, b: String, x: Date): Int = {
    a + 2
  }

  someMethodSync(35, "someParam", new Date())
  someMethodSync(36, "someParam1", new Date())
  someMethodSync(37, "someParam2", new Date())

//prints(logger is created only once):
//creating logger
//00:17:13.630 [run-main-10] DEBUG NEWLOGGER - calling 'someMethodSync' with params: [a=35, b=someParam, x=Thu Apr 27 00:17:13 CEST 2017], correlationId=-3909619315420821164
//00:17:13.630 [run-main-10] DEBUG NEWLOGGER - result of method 'someMethodSync' with correlationId=-3909619315420821164 : 37
//00:17:13.630 [run-main-10] DEBUG NEWLOGGER - calling 'someMethodSync' with params: [a=36, b=someParam1, x=Thu Apr 27 00:17:13 CEST 2017], correlationId=5218714625582540140
//00:17:13.630 [run-main-10] DEBUG NEWLOGGER - result of method 'someMethodSync' with correlationId=5218714625582540140 : 38
//00:17:13.630 [run-main-10] DEBUG NEWLOGGER - calling 'someMethodSync' with params: [a=37, b=someParam2, x=Thu Apr 27 00:17:13 CEST 2017], correlationId=5808946234425929974
//00:17:13.630 [run-main-10] DEBUG NEWLOGGER - result of method 'someMethodSync' with correlationId=5808946234425929974 : 39



}


