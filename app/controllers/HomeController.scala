package controllers

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

// Spark
import spark.SparkTest

class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  //  Methane Emissions end point
  def Methane = Action { implicit request =>
  	val sum = SparkTest.MethaneEmissions
    Ok(views.html.test_args(s"A call to Spark, with result: $sum"))
  }

  // NO Emissions end point
  def NOEmissions = Action { implicit request =>
  	val sum = SparkTest.NOEmissions
    Ok(views.html.test_args(s"A call to Spark, with result: $sum"))
  }

  // PolarIce end point
  def PolarIce = Action { implicit request =>
  	val sum = SparkTest.PolarIce
    Ok(views.html.test_args(s"A call to Spark, with result: $sum"))
  }

  // Temperature end point
  def Temperature = Action { implicit request =>
  	val sum = SparkTest.Temperature
    Ok(views.html.test_args(s"A call to Spark, with result: $sum"))
  }

  // A non-blocking call to Apache Spark 
  def testAsync = Action.async{
  	val futureSum = Future{SparkTest.MethaneEmissions}
    futureSum.map{ s => Ok(views.html.test_args(s"A non-blocking call to Spark with result: ${s + 1000}"))}
  }

}
