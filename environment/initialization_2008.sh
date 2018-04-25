#!/usr/bin/env bash
# dmtest z008.kmtongji.com environment
# Here's the full working install script if someone else needs to install on Amazon Linux (should maybe also work on RHEL and CentOS)
sudo su

yum update -y

# install gcc
#gcc version 4.8.5 20150623 (Red Hat 4.8.5-16) (GCC)

#install OpenBLAS
yum groupinstall -y 'Development Tools'
yum install -y openblas-devel.x86_64
yum install atlas-devel

#install jdk8   #系统一般都有安装，不用安装
#yum install -y java-1.8.0-openjdk-devel.x86_64
#unlink /etc/alternatives/java
#ln -s /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java /etc/alternatives/java
#unlink /etc/alternatives/jre_openjdk
#ln -s /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/ /etc/alternatives/jre_openjdk
#unlink /etc/alternatives/jre
#ln -s /usr/lib/jvm/jre-1.8.0-openjdk.x86_64 /etc/alternatives/jre

#install scala 2.11.8

#install OpenCV 2.4.x   z008上的版本为 2.4.13.6
# 镜像安装
yum install opencv
yum install opencv-devel
# github 编译安装
#https://blog.csdn.net/u012675539/article/details/43490895
#https://blog.csdn.net/cleverysm/article/details/1925549
yum install -y cmake git gtk2-devel pkgconfig numpy ffmpeg
git clone https://github.com/opencv/opencv.git
cd opencv && git checkout 2.4
mkdir build && cd build
cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/usr -DLIB_SUFFIX=64 -DBUILD_SHARED_LIBS=ON ..
make -j $(nproc)
make install

#install mxnet change mxnet version from 1.1.0-SNAPSHOT to 1.1.0
cd ~
git clone --recursive https://github.com/apache/incubator-mxnet.git mxnet --branch 1.1.0
#change mxnet version from 1.1.0-SNAPSHOT to 1.1.0
#change make/config.mk   USE_DIST_KVSTORE = 1
yum install -y libcurl-devel.x86_64 openssl-devel.x86_64 lapack-devel.x86_64 lapack64-static.x86_64
ln -s /usr/lib64/liblapack64.a /usr/lib64/liblapack.a
cd mxnet
make -j $(nproc) USE_OPENCV=1 USE_BLAS=openblas

#install maven
cd ~
wget http://mirrors.koehn.com/apache/maven/maven-3/3.5.5/binaries/apache-maven-3.5.5-bin.tar.gz
tar xzvf apache-maven-3.5.0-bin.tar.gz
mv apache-maven-3.5.0 /opt/
echo "export PATH=/opt/apache-maven-3.5.5/bin:$PATH" >>/etc/profile.d/mvn.sh
source /etc/profile.d/mvn.sh
mvn -v

#install mxnet scala library and run unit-tests
cd ~/mxnet/
make -j $(nproc) scalapkg
make scalatest

#install library in local maven repo
make scalainstall
#replace placeholders in the pom files (seems to be a bug that keeps ${project.version} in the pom files)
find /home/leihui/.m2/repository/ml/dmlc/mxnet/ -type f -exec sed -i 's/${project.version}/1.1..0/g' {} +

#install mxnet libraries on the java lib path
# doc: mxnet on jobServer dependency    ***important
# /home/kmde/mxnet/libmxnet-init-scala-linux-x86_64-1.1.0.so
# /home/kmde/mxnet/libmxnet-scala-linux-x86_64-cpu-1.1.0.so
# /home/kmde/mxnet/mxnet-core_2.11-1.1.0.jar
# /home/kmde/mxnet/mxnet-full_2.11-linux-x86_64-cpu-1.1.0.jar

# mxnet run dependency  ***important
# /home/kmde/mxnet/jars/elemental-spark-jobs.jar
# /home/kmde/mxnet/mxnet-full_2.11-linux-x86_64-cpu-1.1.0.jar

