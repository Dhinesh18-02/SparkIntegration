package sparkIntegration

import common.carsSchema
import org.apache.spark.sql.functions.{col, expr, struct, to_json}
import org.apache.spark.sql.{DataFrame, SparkSession}

object IntegratingKafka {

  val spark = SparkSession.builder()
    .appName("Integrating Kafka")
    .master("local[2]")
    .getOrCreate()

  def readFromKafka() = {
    // https://spark.apache.org/docs/latest/structured-streaming-kafka-integration.html
    val kafkaDF: DataFrame = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "dhinesh")
      .load()

    kafkaDF
     .select(col("topic"), expr("cast(value as string) as actualValue"))
      .writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  def writeToKafka() = {
    val carsDF = spark.readStream
      .schema(carsSchema)
      .json("src/main/resources/data/cars")

    val carsKafkaDF = carsDF.selectExpr("upper(Name) as key", "Name as value")

    carsKafkaDF.writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", "dhinesh")
      .option("checkpointLocation", "checkpoints") // without checkpoints the writing to Kafka will fail
      .start()
      .awaitTermination()
  }

  def writeCarsToKafka() = {
    val carsDF = spark.readStream
      .schema(carsSchema)
      .json("src/main/resources/data/cars")

    val carsJsonKafkaDF = carsDF.select(
      col("Name").as("key"),
      to_json(struct(col("Name"), col("Horsepower"), col("Origin"))).cast("String").as("value")
    )

    carsJsonKafkaDF.writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", "dhinesh")
      .option("checkpointLocation", "checkpoints")
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    writeCarsToKafka()
  }

}
