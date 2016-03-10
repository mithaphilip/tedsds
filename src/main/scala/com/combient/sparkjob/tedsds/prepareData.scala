package com.combient.sparkjob.tedsds

/**
  * Created by olu on 09/03/16.
  */

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{MinMaxScaler, VectorAssembler}
import org.apache.spark.sql.{DataFrame, Column, SaveMode, GroupedData}
import org.apache.spark.sql.types.{StructType,DoubleType, StructField, StringType, IntegerType}
import scopt.OptionParser
import org.apache.spark.{SparkContext, SparkConf}




object PrepareData {

  case class Params(input: String = null,output: String = null)

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("Prepare data for sds") {
      head("PrepareData")
        arg[String]("<input_tsv>")
        .required()
        .text("hdfs input paths tsv dataset ")
        .action((x, c) => c.copy(input = x.trim))
      arg[String]("<output_parquet>")
        .required()
        .text("hdfs output paths parquet output ")
        .action((x, c) => c.copy(output = x.trim))
      note(
        """
          |For example, the following command runs this app on a  dataset:
          |
          | bin/spark-submit --class com.combient.sparkjob.tedsds.PrepareData \
          |  jarfile.jar \
          |  "/user/zeppelin/pdm001/tmp.83h1YRpXM2/train_FD001.txt" \
          |  "/share/tedsds/scaledd"
        """.stripMargin)
    }


    parser.parse(args, defaultParams).map { params =>
      run(params)
    } getOrElse {
      System.exit(1)
    }

  }

  def run(params: Params) {
    val conf = new SparkConf().setAppName("PrepareData")

    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)
    import sqlContext.implicits._

    //todo: generate the select columns

    val customSchema = StructType(Seq(
      StructField("id",      IntegerType,nullable = true),
      StructField("cykle",   IntegerType,nullable =true),
      StructField("setting1",DoubleType,nullable =true),
      StructField("setting2",DoubleType,nullable =true),
      StructField("setting3",DoubleType,nullable =true),
      StructField("s1",      DoubleType,nullable =true),
      StructField("s2",      DoubleType,nullable =true),
      StructField("s3",      DoubleType,nullable =true),
      StructField("s4",      DoubleType,nullable =true),
      StructField("s5",      DoubleType,nullable =true),
      StructField("s6",      DoubleType,nullable =true),
      StructField("s7",      DoubleType,nullable =true),
      StructField("s8",      DoubleType,nullable =true),
      StructField("s9",      DoubleType,nullable =true),
      StructField("s10",     DoubleType,nullable =true),
      StructField("s11",     DoubleType,nullable =true),
      StructField("s12",     DoubleType,nullable =true),
      StructField("s13",     DoubleType,nullable =true),
      StructField("s14",     DoubleType,nullable =true),
      StructField("s15",     DoubleType,nullable =true),
      StructField("s16",     DoubleType,nullable =true),
      StructField("s17",     DoubleType,nullable =true),
      StructField("s18",     DoubleType,nullable =true),
      StructField("s19",     DoubleType,nullable =true),
      StructField("s20",     DoubleType,nullable =true),
      StructField("s21",     DoubleType,nullable =true)))

    val df: DataFrame = sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "false")
      .option("delimiter"," ")
      .schema(customSchema)
      .load(params.input)

    df.persist()

    // calculate max cykle per id
    val maxCyclePerId = df.groupBy($"id").agg(max($"cykle").alias("maxcykle"))

    //maxCyclePerId.show

    // windows for classifcation

    val w1:Int = 30
    val w0:Int = 15

    val withrul = df.join(maxCyclePerId,"id")
      .withColumn("rul",maxCyclePerId("maxcykle")-df("cykle")) // Add RUL as maxcycle-currentcycle per row
      .withColumn("label1",when($"rul" <= w1, 1).otherwise(0)) // add label 1
      .withColumn("label2",when($"rul" <= w0, 2).otherwise(when($"rul" <= w1, 1).otherwise(0))) // add label 2 (1 if under w1, 2 if under w0, zero otherwize)

    val windowRange = 5
    // see https://databricks.com/blog/2015/07/15/introducing-window-functions-in-spark-sql.html
    //     http://spark.apache.org/docs/latest/sql-programming-guide.html
    // PARTITION BY id  ORDER BY cykle ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING (5)
    val w = Window.partitionBy("id").orderBy("cykle").rowsBetween(0, windowRange)

    //todo: generate the select columns

    // see http://spark.apache.org/docs/latest/sql-programming-guide.html
    val x = withrul.select('*,
      mean($"s1").over(w).as("a1"),
      sqrt( sum(pow($"s1" -  mean($"s1").over(w),2)).over(w) / 5).as("sd1"),

      mean($"s2").over(w).as("a2"),
      sqrt( sum(pow($"s2" -  mean($"s2").over(w),2)).over(w) / 5).as("sd2"),

      mean($"s3").over(w).as("a3"),
      sqrt( sum(pow($"s3" -  mean($"s3").over(w),2)).over(w) / 5).as("sd3"),

      mean($"s4").over(w).as("a4"),
      sqrt( sum(pow($"s4" -  mean($"s4").over(w),2)).over(w) / 5).as("sd4"),

      mean($"s5").over(w).as("a5"),
      sqrt( sum(pow($"s5" -  mean($"s5").over(w),2)).over(w) / 5).as("sd5"),

      mean($"s6").over(w).as("a6"),
      sqrt( sum(pow($"s6" -  mean($"s6").over(w),2)).over(w) / 5).as("sd6"),

      mean($"s7").over(w).as("a7"),
      sqrt( sum(pow($"s7" -  mean($"s7").over(w),2)).over(w) / 5).as("sd7"),

      mean($"s8").over(w).as("a8"),
      sqrt( sum(pow($"s8" -  mean($"s8").over(w),2)).over(w) / 5).as("sd8"),

      mean($"s9").over(w).as("a9"),
      sqrt( sum(pow($"s9" -  mean($"s9").over(w),2)).over(w) / 5).as("sd9"),

      mean($"s10").over(w).as("a10"),
      sqrt( sum(pow($"s10" -  mean($"s10").over(w),2)).over(w) / 5).as("sd10"),

      mean($"s11").over(w).as("a11"),
      sqrt( sum(pow($"s11" -  mean($"s11").over(w),2)).over(w) / 5).as("sd11"),

      mean($"s12").over(w).as("a12"),
      sqrt( sum(pow($"s12" -  mean($"s12").over(w),2)).over(w) / 5).as("sd12"),

      mean($"s13").over(w).as("a13"),
      sqrt( sum(pow($"s13" -  mean($"s13").over(w),2)).over(w) / 5).as("sd13"),

      mean($"s14").over(w).as("a14"),
      sqrt( sum(pow($"s14" -  mean($"s14").over(w),2)).over(w) / 5).as("sd14"),

      mean($"s15").over(w).as("a15"),
      sqrt( sum(pow($"s15" -  mean($"s15").over(w),2)).over(w) / 5).as("sd15"),

      mean($"s16").over(w).as("a16"),
      sqrt( sum(pow($"s16" -  mean($"s16").over(w),2)).over(w) / 5).as("sd16"),

      mean($"s17").over(w).as("a17"),
      sqrt( sum(pow($"s17" -  mean($"s17").over(w),2)).over(w) / 5).as("sd17"),

      mean($"s18").over(w).as("a18"),
      sqrt( sum(pow($"s18" -  mean($"s18").over(w),2)).over(w) / 5).as("sd18"),

      mean($"s19").over(w).as("a19"),
      sqrt( sum(pow($"s19" -  mean($"s19").over(w),2)).over(w) / 5).as("sd19"),

      mean($"s20").over(w).as("a20"),
      sqrt( sum(pow($"s20" -  mean($"s20").over(w),2)).over(w) / 5).as("sd20"),

      mean($"s21").over(w).as("a21"),
      sqrt( sum(pow($"s21" -  mean($"s21").over(w),2)).over(w) / 5).as("sd21")
    )

    val columns = x.columns.diff(Seq("id","maxcykle","rul","label1", "label2"))

    print(s"Filter away these columns for the features ${columns}")

    //see https://spark.apache.org/docs/latest/ml-features.html
    // columns to feature vector
    val assembler = new VectorAssembler()
      .setInputCols(columns.toArray)
      .setOutputCol("features")

    // scale the features
    val scaler = new MinMaxScaler()
      .setInputCol("features")
      .setOutputCol("scaledFeatures")

    val withFeatures = assembler.transform(x)

    //drop featurescolumn
    withFeatures.drop("features")

    val scaledDF =  scaler.fit(withFeatures).transform(withFeatures)

    scaledDF.write.mode(SaveMode.Overwrite).parquet(params.output)

    sc.stop()
  }
}