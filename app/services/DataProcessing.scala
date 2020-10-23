package services

import org.apache.spark.SparkConf
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.SparkContext._
import org.apache.spark.sql.cassandra._
import com.datastax.spark.connector._
import org.apache.spark.sql.functions._
import org.apache.spark.ml.Pipeline
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import com.datastax.driver.core.{Session, Cluster, Host, Metadata}
import vegas._
import vegas.render.WindowRenderer._
import vegas.sparkExt._


import com.cloudera.sparkts.models.ARIMA;
import com.cloudera.sparkts.models.ARIMAModel;

object DataProcessing {


   def CO2Emissions : String = {

    val sparkS = SparkSession.builder.master("local[2]").getOrCreate
    import sparkS.implicits._

     val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
     val session = cluster.connect()

     session.execute("CREATE KEYSPACE IF NOT EXISTS environmental_calculations WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
     session.execute("CREATE TABLE IF NOT EXISTS environmental_calculations.co2 (year int PRIMARY KEY, tonnes float);")

    // Create a DataFrame based on the JSON results.
    val fileName = "/home/nanda/allData/co2-api.json"
    val df = sparkS.read.json(fileName)

    val content = df.select(explode($"co2").alias("co2_data"))

    val bat = content.select("co2_data.*")

    bat.printSchema()

     val sam = bat.rdd.map(x => ( x(4).toString.toInt ,x(0).toString.toDouble ))
    
    val dar = sam.reduceByKey(_+_)

    var agg = dar.map(x => (x._1, x._2/365)).sortBy(_._2)

    // Ppm to Metric tons per cubic Kmtr
    var calculation = agg.map(x =>( x._1, x._2 * 1.233))

    var oth = calculation.map(x =>( x._1, (x._2 * 1000000)/4047 ))

    var otherr = oth.map(x =>( x._1, x._2 / 1000 ))

    var inacc = otherr.map(x =>( x._1, x._2 / 27.2 ))

    inacc.saveToCassandra("environmental_calculations", "co2", SomeColumns("year", "tonnes"))


   var modific = inacc.filter(_._1 != 2020)

    modific.take(50).foreach(println)

    val deom = modific.map(_._2.toDouble).collect.toArray

    val ts = Vectors.dense(deom)

    val arimaModel1 = ARIMA.autoFit(ts)

    println("Coefficients: " + arimaModel1.coefficients.mkString(","))

    val forecast1 = arimaModel1.forecast(ts,20)

    session.execute("CREATE TABLE IF NOT EXISTS environmental_calculations.predictioncalculations (year int PRIMARY KEY, co2predicted float, methanepredicted float, nopredicted float);")
   
    var myList = Array(2020,2021,2022,2023,2024,2025,2026,2027,2028,2029,2030,2031,2032,2033,2034,2035,2036,2037,2038,2039)

    val largeRDD = sparkS.sparkContext.parallelize(forecast1.toArray)

    val smallRDD = sparkS.sparkContext.parallelize(myList)

    val befcombin = sparkS.sparkContext.parallelize(largeRDD.top(20).reverse); 

    val combin = smallRDD.zip(befcombin)

    val dffplot = sparkS.createDataFrame(combin).toDF("id", "vals")

    dffplot.printSchema();

    val plot = Vegas("Country Pop").withDataFrame(dffplot)

    combin.take(50).foreach(println)

    combin.saveToCassandra("environmental_calculations", "predictioncalculations", SomeColumns("year", "co2predicted") )

    combin.take(50).foreach(println)

    println("Forecast of next 20 observations: " + forecast1.toArray.mkString(","))
 
    return plot.toJson
  }

 
  def MethaneEmissions : String = {

    val sparkS = SparkSession.builder.master("local[2]").getOrCreate
    import sparkS.implicits._

     val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
     val session = cluster.connect()

     session.execute("CREATE KEYSPACE IF NOT EXISTS environmental_calculations WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
     session.execute("CREATE TABLE IF NOT EXISTS environmental_calculations.methane (year int PRIMARY KEY, tonnes float);")

    // Create a DataFrame based on the JSON results.
    val fileName = "/home/nanda/allData/methane.json"
    val df = sparkS.read.json(fileName)

    val content = df.select(explode($"methane").alias("methane_data"))

    val bat = content.select("methane_data.*")

    val sam = bat.rdd.map(x => ( x(2).toString.split('.')(0).toInt ,x(0).toString.split('.')(0).toInt ))
    
    val dar = sam.reduceByKey(_+_)

    var agg = dar.map(x => (x._1, x._2/12)).sortBy(_._2)

    // Ppm to Metric tons per cubic Kmtr
    var calculation = agg.map(x =>( x._1, x._2 * 1.233))

    var oth = calculation.map(x =>( x._1, (x._2 * 1000000)/4047 ))

    var otherr = oth.map(x =>( x._1, x._2 / 1000 ))

    var inacc = otherr.map(x =>( x._1, x._2 / 27.2 )).sortBy(_._2)


    inacc.saveToCassandra("environmental_calculations", "methane", SomeColumns("year", "tonnes"))



    var modific = inacc.filter(_._1 != 2020)

    modific.take(50).foreach(println)

    val deom = modific.map(_._2.toDouble).collect.toArray

    val ts = Vectors.dense(deom)

    val arimaModel1 = ARIMA.autoFit(ts)

    println("Coefficients: " + arimaModel1.coefficients.mkString(","))

    val forecast1 = arimaModel1.forecast(ts,20)

    session.execute("CREATE TABLE IF NOT EXISTS environmental_calculations.predictioncalculations (year int PRIMARY KEY, co2predicted float, methanepredicted float, nopredicted float);")
   
    var myList = Array(2020,2021,2022,2023,2024,2025,2026,2027,2028,2029,2030,2031,2032,2033,2034,2035,2036,2037,2038,2039)

    val largeRDD = sparkS.sparkContext.parallelize(forecast1.toArray)

    val smallRDD = sparkS.sparkContext.parallelize(myList);

    val befcombin = sparkS.sparkContext.parallelize(largeRDD.top(20).reverse); 

    val combin = smallRDD.zip(befcombin)

    combin.take(50).foreach(println)

    combin.saveToCassandra("environmental_calculations", "predictioncalculations", SomeColumns("year", "methanepredicted") )

    println("Forecast of next 20 observations: " + forecast1.toArray.mkString(","))
 
    return "Methane - Actual and Prediction calculations Completed and Pushed to Cassandra!"

  }

   def NOEmissions : String = {

    val sparkS = SparkSession.builder.master("local[2]").getOrCreate
    import sparkS.implicits._

     val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
     val session = cluster.connect()

     session.execute("CREATE KEYSPACE IF NOT EXISTS environmental_calculations WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
     session.execute("CREATE TABLE IF NOT EXISTS environmental_calculations.noemissions (year int PRIMARY KEY, tonnes float);")

    // Create a DataFrame based on the JSON results.
    val fileName = "/home/nanda/allData/NO.json"
    val df = sparkS.read.json(fileName)  

    val content = df.select(explode($"nitrous").alias("nitrous_oxide_data"))

    val bat = content.select("nitrous_oxide_data.*")

    val sam = bat.rdd.map(x => ( x(2).toString.split('.')(0).toInt ,x(0).toString.split('.')(0).toInt ))
    
    val dar = sam.reduceByKey(_+_)

    var agg = dar.map(x => (x._1, x._2/12)).sortBy(_._2)

    // Ppm to Metric tons per cubic Kmtr
    var calculation = agg.map(x =>( x._1, x._2 * 1.233))

    var oth = calculation.map(x =>( x._1, (x._2 * 1000000)/4047 ))

    var otherr = oth.map(x =>( x._1, x._2 / 1000 ))

    var inacc = otherr.map(x =>( x._1, x._2 / 27.2 )).sortBy(_._2)

    inacc.saveToCassandra("environmental_calculations", "noemissions", SomeColumns("year", "tonnes"))


   
    var modific = inacc.filter(_._1 != 2020)

    modific.take(50).foreach(println)

    val deom = modific.map(_._2.toDouble).collect.toArray

    val ts = Vectors.dense(deom)

    val arimaModel1 = ARIMA.autoFit(ts)

    println("Coefficients: " + arimaModel1.coefficients.mkString(","))

    val forecast1 = arimaModel1.forecast(ts,20)

    session.execute("CREATE TABLE IF NOT EXISTS environmental_calculations.predictioncalculations (year int PRIMARY KEY, co2predicted float, methanepredicted float, nopredicted float);")
   
    var myList = Array(2020,2021,2022,2023,2024,2025,2026,2027,2028,2029,2030,2031,2032,2033,2034,2035,2036,2037,2038,2039)

    val largeRDD = sparkS.sparkContext.parallelize(forecast1.toArray)

    val smallRDD = sparkS.sparkContext.parallelize(myList);

    val befcombin = sparkS.sparkContext.parallelize(largeRDD.top(20).reverse); 

    val combin = smallRDD.zip(befcombin)

    combin.take(50).foreach(println)

    combin.saveToCassandra("environmental_calculations", "predictioncalculations", SomeColumns("year", "nopredicted") )

    println("Forecast of next 20 observations: " + forecast1.toArray.mkString(","))
 
    return "Nitrous Oxide - Actual and Prediction calculations Completed and Pushed to Cassandra!"

  }

  def PolarIce : String = {

    val sparkS = SparkSession.builder.master("local[2]").getOrCreate
    import sparkS.implicits._

    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
    val session = cluster.connect()

     session.execute("CREATE KEYSPACE IF NOT EXISTS environmental_calculations WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
     session.execute("CREATE TABLE IF NOT EXISTS environmental_calculations.polaricevalues (year int PRIMARY KEY, area float, extent float);")

    // Create a DataFrame based on the JSON results.
    val fileName = "/home/nanda/allData/PolarIce.json"
    val df = sparkS.read.json(fileName)  

    val content = df.select(explode($"result").alias("polarice_data"))

    val bat = content.select("polarice_data.*")

    bat.printSchema();

    val sam = bat.rdd.map(x => (x(2) ,x(0) ))

     sam.take(50).foreach(println)

    val sam2 = bat.rdd.map(x => (x(2) ,x(1) ))

     sam2.take(50).foreach(println)

    sam.saveToCassandra("environmental_calculations", "polaricevalues", SomeColumns("year", "area"))

    sam2.saveToCassandra("environmental_calculations", "polaricevalues", SomeColumns("year", "extent" ))

     return "PolarIce - Actual calculations Completed and Pushed to Cassandra!"

  }

  def Temperature : String = {

    val sparkS = SparkSession.builder.master("local[2]").getOrCreate
    import sparkS.implicits._

     val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
     val session = cluster.connect()

     session.execute("CREATE KEYSPACE IF NOT EXISTS environmental_calculations WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
     session.execute("CREATE TABLE IF NOT EXISTS environmental_calculations.temperaturechanges (year int PRIMARY KEY, temperature float);")

    // Create a DataFrame based on the JSON results.
    val fileName = "/home/nanda/allData/Temperature.json"
    val df = sparkS.read.json(fileName)  

    val content = df.select(explode($"result").alias("temperature_data"))

    val bat = content.select("temperature_data.*")

    val sam = bat.rdd.map(x => (x(2).toString.split('.')(0).toInt ,x(0).toString.toDouble ))

     bat.printSchema();

     val dar = sam.reduceByKey(_+_)

     var agg = dar.map(x => (x._1, x._2/12)).sortBy(_._1)
    
    agg.saveToCassandra("environmental_calculations", "temperaturechanges", SomeColumns("year", "temperature"))

    agg.take(150).foreach(println)

     return "Tempterature - Actual calculations Completed and Pushed to Cassandra!"

  }

}