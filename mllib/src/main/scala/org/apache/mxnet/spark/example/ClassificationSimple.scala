/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mxnet.spark.example

import org.apache.mxnet.spark.MXNet
import org.apache.mxnet.{Context, NDArray, Shape, Symbol}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.kohsuke.args4j.{CmdLineParser, Option}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

class ClassificationSimple
object ClassificationSimple {
  private val logger: Logger = LoggerFactory.getLogger(classOf[ClassificationExample])
  def main(args: Array[String]): Unit = {
    try {
    val spark = SparkSession
      .builder()
      .appName("MXNET")
      .config("spark.some.config.option", "some-value")
      .master("local[4]")
      .getOrCreate()

    val mxnet = new MXNet()
      .setBatchSize(128)
      .setLabelName("softmax_label")
      .setContext(Context.cpu(4))
      .setDimension(Shape(1, 28, 28))
      .setNetwork(getLenet)
      .setExecutorJars("/home/leihui/IdeaProjects/spark-mxnet/mllib/target/scala-2.11/mllib.jar")

    val sc = spark.sparkContext

    val trainData = parseRawData(sc, "/home/leihui/IdeaProjects/spark-mxnet/mllib/datasets/train.txt")
    trainData.foreach(f =>println(f))
    val start = System.currentTimeMillis
    val model = mxnet.fit(trainData)
    val timeCost = System.currentTimeMillis - start
    logger.info("Training cost {} milli seconds", timeCost)
    model.save(sc, "/home/leihui/IdeaProjects/spark-mxnet/mllib/model")

    logger.info("Now do validation")
    val valData = parseRawData(sc, "/home/leihui/IdeaProjects/spark-mxnet/mllib/datasets/val.txt")

    val brModel = sc.broadcast(model)
    val res = valData.mapPartitions { data =>
      // get real labels
      import org.apache.spark.mllib.linalg.Vector
      val points = ArrayBuffer.empty[Vector]
      val y = ArrayBuffer.empty[Float]
      while (data.hasNext) {
        val evalData = data.next()
        y += evalData.label.toFloat
        points += evalData.features
      }

      // get predicted labels
      val probArrays = brModel.value.predict(points.toIterator)
      require(probArrays.length == 1)
      val prob = probArrays(0)
      val py = NDArray.argmax_channel(prob.get)
      require(y.length == py.size, s"${y.length} mismatch ${py.size}")

      // I'm too lazy to calculate the accuracy
      val res = Iterator((y.toArray zip py.toArray).map {
        case (y1, py1) => y1 + "," + py1 }.mkString("\n"))

      py.dispose()
      prob.get.dispose()
      res
    }
    res.saveAsTextFile(s"/home/leihui/IdeaProjects/spark-mxnet/mllib/predict")
    spark.stop()
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        sys.exit(-1)
    }
  }

  private def parseRawData(sc: SparkContext, path: String): RDD[LabeledPoint] = {
    val raw = sc.textFile(path)
    raw.map { s =>
      val parts = s.split(' ')
      val label = java.lang.Double.parseDouble(parts(0))
      val features = Vectors.dense(parts(1).trim().split(',').map(_.toDouble))
      LabeledPoint(label, features)
    }
  }

  private class CommandLine {
    @Option(name = "--input", usage = "Input training file.")
    val input: String = null
    @Option(name = "--input-val", usage = "Input validation file.")
    val inputVal: String = null
    @Option(name = "--output", usage = "Output inferred result.")
    val output: String = null
    @Option(name = "--jars", usage = "Jars for running MXNet on other nodes.")
    val jars: String = null
    @Option(name = "--num-server", usage = "PS server number")
    val numServer: Int = 1
    @Option(name = "--num-worker", usage = "PS worker number")
    val numWorker: Int = 1
    @Option(name = "--num-epoch", usage = "Number of epochs")
    val numEpoch: Int = 10
    @Option(name = "--java", usage = "Java bin")
    val java: String = "java"
    @Option(name = "--model", usage = "Model definition")
    val model: String = "mlp"
    @Option(name = "--gpus", usage = "the gpus will be used, e.g. '0,1,2,3'")
    val gpus: String = null
    @Option(name = "--cpus", usage = "the cpus will be used, e.g. '0,1,2,3'")
    val cpus: String = null

    def checkArguments(): Unit = {
      require(input != null, "Undefined input path")
      require(numServer > 0, s"Invalid number of servers: $numServer")
      require(numWorker > 0, s"Invalid number of workers: $numWorker")
    }
  }

  def getMlp: Symbol = {
    val data = Symbol.Variable("data")
    val fc1 = Symbol.FullyConnected(name = "fc1")()(Map("data" -> data, "num_hidden" -> 128))
    val act1 = Symbol.Activation(name = "relu1")()(Map("data" -> fc1, "act_type" -> "relu"))
    val fc2 = Symbol.FullyConnected(name = "fc2")()(Map("data" -> act1, "num_hidden" -> 64))
    val act2 = Symbol.Activation(name = "relu2")()(Map("data" -> fc2, "act_type" -> "relu"))
    val fc3 = Symbol.FullyConnected(name = "fc3")()(Map("data" -> act2, "num_hidden" -> 10))
    val mlp = Symbol.SoftmaxOutput(name = "softmax")()(Map("data" -> fc3))
    mlp
  }

  // LeCun, Yann, Leon Bottou, Yoshua Bengio, and Patrick
  // Haffner. "Gradient-based learning applied to document recognition."
  // Proceedings of the IEEE (1998)
  def getLenet: Symbol = {
    val data = Symbol.Variable("data")
    // first conv
    val conv1 = Symbol.Convolution()()(
      Map("data" -> data, "kernel" -> "(5, 5)", "num_filter" -> 20))
    val tanh1 = Symbol.Activation()()(Map("data" -> conv1, "act_type" -> "tanh"))
    val pool1 = Symbol.Pooling()()(Map("data" -> tanh1, "pool_type" -> "max",
      "kernel" -> "(2, 2)", "stride" -> "(2, 2)"))
    // second conv
    val conv2 = Symbol.Convolution()()(
      Map("data" -> pool1, "kernel" -> "(5, 5)", "num_filter" -> 50))
    val tanh2 = Symbol.Activation()()(Map("data" -> conv2, "act_type" -> "tanh"))
    val pool2 = Symbol.Pooling()()(Map("data" -> tanh2, "pool_type" -> "max",
      "kernel" -> "(2, 2)", "stride" -> "(2, 2)"))
    // first fullc
    val flatten = Symbol.Flatten()()(Map("data" -> pool2))
    val fc1 = Symbol.FullyConnected()()(Map("data" -> flatten, "num_hidden" -> 500))
    val tanh3 = Symbol.Activation()()(Map("data" -> fc1, "act_type" -> "tanh"))
    // second fullc
    val fc2 = Symbol.FullyConnected()()(Map("data" -> tanh3, "num_hidden" -> 10))
    // loss
    val lenet = Symbol.SoftmaxOutput(name = "softmax")()(Map("data" -> fc2))
    lenet
  }
}
