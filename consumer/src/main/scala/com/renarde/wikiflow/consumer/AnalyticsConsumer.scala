package com.renarde.wikiflow.consumer

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.types.{BooleanType, IntegerType, StringType, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}

object AnalyticsConsumer extends App with LazyLogging {

  val appName: String = "analytics-consumer-example"

  val spark: SparkSession = SparkSession.builder()
    .appName(appName)
    .config("spark.driver.memory", "5g")
    .master("local[2]")
    .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")

  logger.info("Initializing Structured consumer")


  val inputStream = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "kafka:9092")
    .option("subscribe", "wikiflow-topic")
    .option("startingOffsets", "earliest")
    .load()

  // please edit the code below  DataFrame
//  Old column names (7): key, value, topic, partition, offset, timestamp, timestampType
//  | key|               value|         topic|partition|offset|           timestamp|timestampType|
//  | |null|  [7B 22 24 73 63 6...|   wikiflow-topic|   0|  420867|   2019-09-15 08:57:...|            0|
  import spark.implicits._
  import org.apache.spark.sql.functions._


  val schema = (new StructType)
    .add("$schema", StringType)
//    .add("meta", StringType)
//  "meta": {
//    "uri": "https://www.wikidata.org/wiki/Q67127528",
//    "request_id": "XX4G4gpAIDgAADhjdnoAAABR",
//    "id": "40c3b0d5-4e25-47c5-9307-a5462f7ff5fb",
//    "dt": "2019-09-15T09:39:46Z",
//    "domain": "www.wikidata.org",
//    "stream": "mediawiki.recentchange",
//    "topic": "eqiad.mediawiki.recentchange",
//    "partition": 0,
//    "offset": 1832262458
//  },
    .add("id", IntegerType)
    .add("type", StringType)
    .add("namespace", IntegerType)
    .add("title", StringType)
    .add("comment", StringType)
    .add("timestamp", IntegerType)
    .add("user", StringType)
    .add("bot", BooleanType)
    .add("minor", BooleanType)
    .add("patrolled", BooleanType)
    .add("length", StringType)
//  "length": {
//    "old": 5514,
//    "new": 5514
//  },
    .add("revision", StringType)
//  "revision": {
//    "old": 1009875096,
//    "new": 1013782984
//  },
    .add("server_url", StringType)
    .add("server_name", StringType)
    .add("server_script_path", StringType)
    .add("wiki", StringType)
    .add("parsedcomment", StringType)
//  import org.apache.spark.sql.functions.array_contains
//  val c = array_contains(column = $"json_wiki.bot", value = true)
  val transformedStream: DataFrame = inputStream
 . toDF()
          .filter($"value".isNotNull)
    .select(
      inputStream.col("key").cast("string"),
      inputStream.col("value").cast("string"))
    .select(from_json($"value", schema).as("json_wiki"))
    .selectExpr("json_wiki.type", "json_wiki.user", "json_wiki.bot", "json_wiki.timestamp")
    .filter($"json_wiki.bot" =!= true)
//    .withWatermark("timestamp", "10 minutes")
//  .withWatermark("timestamp", "3 hours")
    .groupBy("type", "timestamp")
    .count()

//        .filter("value isNotNull")
//      .collect()
  //      .map(K,V -> V.)

//  // movie struct
//  val struct = new StructType()
//    .add("title", DataTypes.StringType)
//    .add("year", DataTypes.IntegerType)
//    .add("cast", ArrayType(DataTypes.StringType))
//    .add("genres", ArrayType(DataTypes.StringType))
//
//  val moviesNestedDf = moviesJsonDf.select(from_json($"value", struct).as("movie"))
//  // json flatten
//  val movieFlattenedDf = moviesNestedDf.selectExpr("movie.title", "movie.year", "movie.cast","movie.genres")
//
//
//  // convert to parquet and save to hdfs
//  val query = movieFlattenedDf
//    .writeStream
//    .outputMode("append")
//    .format("parquet")
//    .queryName("movies")
//    .option("checkpointLocation", "src/main/resources/chkpoint_dir")
//    .start("src/main/resources/output")
//    .awaitTermination()
//}

  transformedStream.writeStream
    // write stream to delta
//    .outputMode("append")
//    .format("delta")
//    .option("checkpointLocation", "/storage/analytics-consumer/checkpoints")
//    .start("/storage/analytics-consumer/output")

    .outputMode("complete")
//    .outputMode("Update")


    .format("console")

    .start()

//    .queryName("count_customer")
    //.format("console")
//    .outputMode("append")
//    .format("json")
//    .partitionBy("date")
//    .option("path", "~./output/")
//    .option("checkpointLocation", "src/main/resources/chkpoint_dir")
//    .start()

//  transformedStream.show(false)

//  cd /var/lib/docker/volumes/


  //- из прилетающего стрима выбрать только ключи и значения,
  // провести преобразование типов к кортежу (String,String)
  //
  //- отфильтровать строки с пустыми значениями (value.isNotNull)
  //
  //- провести преобразование входного json-объекта (которым и является value)
  // в структуру через функцию from_json
  //
  //-  удалить всю активность ботов из входящего потока (bot !=true)
  //
  //- сгруппировать по полю "type", посчитать каунты, добавить текущий timestamp
  // и записать иx в выходной стрим (объект transformedStream)

 // transformedStream.show()
  //  robberyStatsWithBroadcast.show()
  //    .filter($"NAME".startsWith("ROBBERY"))
  //    .select(  avg($"Lat"), avg($"long"))
  //    .groupBy($"DISTRICT")
  //     .count()
  //   .agg(avg($"Lat"), avg($"long"))
  //   .orderBy($"count".desc)
  //






  spark.streams.awaitAnyTermination()
}
