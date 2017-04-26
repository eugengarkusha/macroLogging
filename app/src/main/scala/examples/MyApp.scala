package examples

import java.util.Date

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.Logger

object MyApp extends App {

  val logger = Logger("NEWLOGGER")


  @LogAsync(logger, LogLevel.WARN)
  def someMethodAsync(a: Int, b: String, x: Date): Future[Int] = {
    Future(a + 2)
  }

  someMethodAsync(35, "someParam", new Date())

//prints:
//10:54:26.600 [run-main-b] WARN NEWLOGGER - calling 'someMethodAsync' with params: [a=35, b=someParam, x=Wed Apr 26 10:54:26 CEST 2017], correlationId=965055641972199212
//10:54:26.603 [ForkJoinPool-1-worker-13] WARN NEWLOGGER - result of method 'someMethodAsync'with correlationId=965055641972199212 : Success(37)

  import LogLevel._

  //checking that logger is created only once
  def getLogger = {
    println("creating logger")
    Logger("NEWLOGGER")
  }

  @Log(getLogger, DEBUG)
  def someMethodSync(a: Int, b: String, x: Date): Int = {
    a + 2
  }

  someMethodSync(35, "someParam", new Date())

//prints:
//creating logger
//10:54:26.603 [run-main-b] DEBUG NEWLOGGER - calling 'someMethodSync' with params: [a=35, b=someParam, x=Wed Apr 26 10:54:26 CEST 2017], correlationId=7420713298135813737
//10:54:26.603 [run-main-b] DEBUG NEWLOGGER - result of method 'someMethodSync' with correlationId=7420713298135813737 : 37


}


