package examples

import java.util.Date

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.Logger

object MyApp extends App {

  val logger = Logger("NEWLOGGER")

  @LogAsync(logger, Error)
  def someMethodAsync(a: Int, b: String, x: Date): Future[Int] = {
    Future(a + 2)
  }

  someMethodAsync(35, "someParam", new Date())
//prints:
//01:25:16.273 [run-main-5] ERROR NEWLOGGER - calling 'someMethodAsync' with params: [a=35, b=someParam, x=Wed Apr 26 01:25:16 CEST 2017],correlationId=-800595665753922164
//01:25:16.277 [ForkJoinPool-1-worker-13] ERROR NEWLOGGER - result of method 'someMethodAsync' with correlationId=-800595665753922164 : Success(37)

  @Log(logger, Error)
  def someMethodSync(a: Int, b: String, x: Date): Int = {
    a + 2
  }

  someMethodSync(35, "someParam", new Date())

//prints:
//01:25:16.277 [run-main-5] ERROR NEWLOGGER - calling 'someMethodSync' with params: [a=35, b=someParam, x=Wed Apr 26 01:25:16 CEST 2017],correlationId=-4819438853255234199
//01:25:16.277 [run-main-5] ERROR NEWLOGGER - result of method 'someMethodSync' with correlationId=-4819438853255234199 : 37

}


