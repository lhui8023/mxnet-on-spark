#!/usr/bin/env bash
# dmtest z008.kmtongji.com environment
# Here's the full working install script if someone else needs to install on Amazon Linux (should maybe also work on RHEL and CentOS)
sudo su

yum update -y

#gcc
#gcc version 4.8.5 20150623 (Red Hat 4.8.5-16) (GCC)

#install OpenBLAS
yum groupinstall -y 'Development Tools'
yum install -y openblas-devel.x86_64

yum install atlas-devel opencv
yum install opencv-devel

#install jdk8   #系统一般都有安装，不用安装
yum install -y java-1.8.0-openjdk-devel.x86_64
unlink /etc/alternatives/java
ln -s /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java /etc/alternatives/java
unlink /etc/alternatives/jre_openjdk
ln -s /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/ /etc/alternatives/jre_openjdk
unlink /etc/alternatives/jre
ln -s /usr/lib/jvm/jre-1.8.0-openjdk.x86_64 /etc/alternatives/jre

#install OpenCV 2.4
#OpenCV3 includes protobuf library that conflict with the mxnet ones. https://github.com/apache/incubator-mxnet/issues/7998
yum install -y cmake git gtk2-devel pkgconfig numpy ffmpeg
git clone https://github.com/opencv/opencv.git
cd opencv && git checkout 2.4
mkdir build && cd build
cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/usr -DLIB_SUFFIX=64 -DBUILD_SHARED_LIBS=ON ..
make -j $(nproc)
make install

#install mxnet
cd ~
git clone --recursive https://github.com/apache/incubator-mxnet.git mxnet --branch 0.11.0
yum install -y libcurl-devel.x86_64 openssl-devel.x86_64 lapack-devel.x86_64 lapack64-static.x86_64
ln -s /usr/lib64/liblapack64.a /usr/lib64/liblapack.a
cd mxnet && cp make/config.mk .
echo "USE_CUDA=0" >>config.mk
echo "USE_CUDNN=0" >>config.mk
echo "USE_BLAS=openblas" >>config.mk
echo "ADD_CFLAGS += -I/usr/include/openblas" >>config.mk
echo "USE_OPENCV=1" >>config.mk
echo "USE_S3=1" >>config.mk

echo "USE_LAPACK_PATH=/usr/lib64" >>config.mk
make -j $(nproc)

#install maven
cd ~
wget http://mirrors.koehn.com/apache/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.tar.gz
tar xzvf apache-maven-3.5.0-bin.tar.gz
mv apache-maven-3.5.0 /opt/
echo "export PATH=/opt/apache-maven-3.5.0/bin:$PATH" >>/etc/profile.d/mvn.sh
source /etc/profile.d/mvn.sh
mvn -v

#install mxnet scala library and run unit-tests
cd ~/mxnet/
make -j $(nproc) scalapkg
make scalatest

#install library in local maven repo
make scalainstall
#replace placeholders in the pom files (seems to be a bug that keeps ${project.version} in the pom files)
find /root/.m2/repository/ml/dmlc/mxnet/ -type f -exec sed -i 's/${project.version}/0.11.0-SNAPSHOT/g' {} +

#install mxnet libraries on the java lib path
#mvn -f /root/.m2/repository/ml/dmlc/mxnet/mxnet-full_2.11-linux-x86_64-cpu/0.11.0-SNAPSHOT/mxnet-full_2.11-linux-x86_64-cpu-0.11.0-SNAPSHOT.pom \
#dependency:copy-dependencies -DoutputDirectory=/usr/lib64/mxnet
mkdir /usr/lib64/mxnet
find /root/.m2/repository/ml/dmlc/mxnet/ -type f \
    ! -name "*pom" \
    ! -name "*xml" \
    ! -name "*javadoc*" \
    ! -name "_remote.repositories" \
    ! -name "*-sources.jar" \
    -exec cp '{}' /usr/lib64/mxnet/ \;
ln -s /usr/lib64/mxnet/libmxnet-scala-linux-x86_64-cpu-0.11.0-SNAPSHOT.so /usr/lib64/libmxnet-scala.so
ln -s /usr/lib64/mxnet/libmxnet-init-scala-linux-x86_64-0.11.0-SNAPSHOT.so /usr/lib64/libmxnet-init-scala.so

#Run MNIST
./scala-package/core/scripts/get_mnist_data.sh
java -Xmx4G -cp \
  /usr/lib64/mxnet/mxnet-core_2.11-0.11.0-SNAPSHOT.jar:/usr/lib64/mxnet/mxnet-examples_2.11-0.11.0-SNAPSHOT.jar:scala-package/examples/target/classes/lib/* \
  ml.dmlc.mxnetexamples.imclassification.TrainMnist \
  --data-dir=./data/ \
  --num-epochs=10 \
  --network=mlp \
  --cpus=0,1,2,3

#RUN GAN MNIST
java -Xmx4G -cp \
    /usr/lib64/mxnet/mxnet-core_2.11-0.11.0-SNAPSHOT.jar:/usr/lib64/mxnet/mxnet-examples_2.11-0.11.0-SNAPSHOT.jar:scala-package/examples/target/classes/lib/*  \
    ml.dmlc.mxnetexamples.gan.GanMnist \
    --mnist-data-path=./data/ \
    --output-path=./data/gan/ \
    --gpu=-1