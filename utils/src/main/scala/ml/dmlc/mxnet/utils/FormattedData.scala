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

package ml.dmlc.mxnet.utils

import java.io.{File, PrintWriter}

import org.apache.mxnet.Shape

/**
 * MXNet on Spark training arguments
 * @author Yizhi Liu
 */
object FormattedData {
  def main(args: Array[String]): Unit = {

    val dataPath = s"/home/leihui/IdeaProjects/spark-mxnet/utils/datasets/mnist"
    val batchSize = 10

    val (trainIter, testIter) = Data.mnistIterator(dataPath = dataPath, batchSize = batchSize, inputShape = Shape(28,28))

    val datas = trainIter.getData().flatMap(f => f.toArray).sliding(784, 784).map(f => f.seq.mkString(",")).zipWithIndex.map(f => (f._2, f._1)).toSeq

    val label = trainIter.getLabel().flatMap(f => f.toArray).map(f => f.toString).zipWithIndex.map(f => (f._2, f._1))

    val datasets = label.zip(datas).map(f => s"${f._1._2} ${f._2._2}")

    //文件写入
    val writer = new PrintWriter(new File("/home/leihui/IdeaProjects/spark-mxnet/utils/datasets/train.txt"))
    for(d <- datasets)
      writer.println(d)
    writer.close()
  }
}
