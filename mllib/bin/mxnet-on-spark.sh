#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

CURR_DIR=$(cd `dirname $0`; pwd)
SPARK_MODULE_DIR=$(cd $CURR_DIR/../; pwd)
SCALA_PKG_DIR=$(cd $CURR_DIR/../../; pwd)
OS=""

if [ "$(uname)" == "Darwin" ]; then
	# Do something under Mac OS X platform
  OS='osx-x86_64-cpu'
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
  OS='linux-x86_64-cpu'
fi

SPARK_JAR=`find ${SPARK_MODULE_DIR}/target/scala-2.11/mllib.jar -type f -exec ls "{}" + | grep -v -E '(javadoc|sources)'`
SCALA_JAR=`find ${HOME}/.m2/repository/ml/dmlc/mxnet/mxnet-full_2.11-${OS}/1.1.0-SNAPSHOT/ -maxdepth 1 -name "*.jar" -type f -exec ls "{}" + | grep -v -E '(javadoc|sources)'`

SPARK_OPTS+=" --name mxnet-spark-mnist"
SPARK_OPTS+=" --driver-memory 1g"
SPARK_OPTS+=" --executor-memory 1g"
SPARK_OPTS+=" --num-executors 2"
SPARK_OPTS+=" --executor-cores 1"
SPARK_OPTS+=" --jars ${SCALA_JAR}"

# Download training and test set
#if [ ! -f ./train.txt ]; then
#  wget https://s3-us-west-2.amazonaws.com/mxnet.liuyz/data/mnist/train.txt
#fi

#if [ ! -f ./val.txt ]; then
#  wget https://s3-us-west-2.amazonaws.com/mxnet.liuyz/data/mnist/val.txt
#fi

# running opts
#RUN_OPTS+=" --input train.txt"
#RUN_OPTS+=" --input-val val.txt"
RUN_OPTS+=" --input train-images-idx3-ubyte"
RUN_OPTS+=" --input-val train-labels-idx1-ubyte"
RUN_OPTS+=" --output ${SPARK_MODULE_DIR}/"
# These jars are required by the KVStores at runtime.
# They will be uploaded and distributed to each node automatically.
RUN_OPTS+=" --jars $SCALA_JAR,$SPARK_JAR"
#RUN_OPTS+=" --num-server 1"
#RUN_OPTS+=" --num-worker 2"
RUN_OPTS+=" --java $JAVA_HOME/bin/java"
RUN_OPTS+=" --model mlp"
RUN_OPTS+=" --cpus 0,1"
RUN_OPTS+=" --num-epoch 5"

# check if SPARK_HOME is set
if [ -z "$SPARK_HOME" ]; then
  echo "SPARK_HOME is unset";
  exit 1
fi

HOST=`hostname`

#$SPARK_HOME/bin/spark-submit --master local \
#  --class ml.dmlc.mxnet.spark.example.ClassificationExample \
#  ${SPARK_OPTS} \
#  ${SPARK_JAR} \
#  ${RUN_OPTS}

CLASS_PATH=${SPARK_MODULE_DIR}/target/*:${SPARK_MODULE_DIR}/target/scala-2.11/*:${HOME}/.m2/repository/org/apache/commons/commons-lang3/3.5/*

echo ${CLASS_PATH}

java -Xmx4G -cp ${CLASS_PATH} \
	ml.dmlc.mxnet.spark.example.ClassificationExample \
    ${RUN_OPTS}