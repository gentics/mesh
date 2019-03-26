# This Dockerfile is used to build an image containing basic stuff to be used as a Jenkins slave build node.
FROM jenkinsci/jnlp-slave
MAINTAINER Johannes Schüth <j.schueth@gentics.com>
USER root

# Key chains
RUN wget -O /etc/apt/trusted.gpg.d/php.gpg https://packages.sury.org/php/apt.gpg
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C2518248EEA14886

# APT
RUN echo 'deb http://debian.apa.at/debian stretch main contrib non-free' > /etc/apt/sources.list
RUN echo 'deb http://debian.apa.at/debian stretch-backports main' >> /etc/apt/sources.list
RUN echo 'deb http://security.debian.org/ stretch/updates main contrib' >> /etc/apt/sources.list

RUN apt-get update && apt-get -y upgrade && apt-get install -y git-core gawk sed wget curl sudo \
  less vim curl tar wget sed psmisc file pwgen locales zip unzip software-properties-common devscripts \
  bzip2 rsync maven

# Docker
RUN curl "https://get.docker.com/builds/`uname -s`/`uname -m`/docker-latest.tgz" > docker.tgz &&\
    tar -xvzf docker.tgz &&\
    mv docker/* /usr/bin &&\
    rm -rf docke*

# Install Java 8
RUN mkdir -p /usr/lib/jvm/ && \
   cd /usr/lib/jvm/ && \
   wget -nv -O jdk-8-x64.tar.gz "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u202-b08/OpenJDK8U-jdk_x64_linux_hotspot_8u202b08.tar.gz" && \
   tar xfz jdk-8-x64.tar.gz && \
   rm *.gz && \
   mv jdk8u202-b08/ java-8
ENV JAVA_1_8_HOME /usr/lib/jvm/java-8

# Install Java 10
#RUN mkdir -p /usr/lib/jvm/ && \
#   cd /usr/lib/jvm/ && \
#   wget -nv -O openjdk-10.0.1_linux-x64_bin.tar.gz "https://download.java.net/java/GA/jdk10/10.0.1/fb4372174a714e6b8c52526dc134031e/10/openjdk-10.0.1_linux-x64_bin.tar.gz" && \
#   tar xfz openjdk-10.0.1_linux-x64_bin.tar.gz && \
#   rm *.gz && \
#   mv jdk-10* java-10
#ENV JAVA_1_10_HOME /usr/lib/jvm/java-10

# Add Jenkins user
RUN echo "jenkins:jenkins" | chpasswd
RUN mkdir -p /home/jenkins/workspace && mkdir -p /home/jenkins/.ssh

ADD ./files/settings.xml /home/jenkins/.m2/settings.xml
#ADD ./files/gentics /home/jenkins/.gnupg/gentics
RUN chown jenkins: /home/jenkins -R

RUN echo "root:root" | chpasswd
ADD ./files/config /home/jenkins/.ssh/config
RUN ssh-keyscan github.com >> /home/jenkins/.ssh/known_hosts
RUN ssh-keyscan git.gentics.com >> /home/jenkins/.ssh/known_hosts

RUN mkdir -p /home/jenkins/.m2/repository
RUN chown jenkins: /home/jenkins -R

# Update Bash RC 
RUN rm /home/jenkins/.bashrc
RUN echo '. /etc/profile.d/*.sh' > /home/jenkins/.bashrc
RUN chown jenkins: /home/jenkins/.bashrc

# Setup locale
RUN sed -i  's/.*\(en_US.UTF-8.*\)/\1/' /etc/locale.gen 
RUN locale-gen
RUN locale-gen en_US.UTF-8
RUN update-locale LANG=en_US.UTF-8

RUN echo "LC_ALL=en_US.UTF-8" > /etc/default/locale
RUN echo "LANG=en_US.UTF-8" > /etc/default/locale
RUN echo "LANGUAGE=en_US:en" >> /etc/default/locale

# Setup timezone
RUN cp /usr/share/zoneinfo/Europe/Vienna /etc/localtime 
RUN echo "Europe/Vienna" > /etc/timezone 

# Setup Git
ADD ./files/.gitconfig /home/jenkins/.gitconfig
RUN chown jenkins: /home/jenkins/.gitconfig

RUN chown jenkins: /opt -R

# Fix docker socket permission issue
RUN groupadd -g 117 docker && usermod -aG docker jenkins

ENV LANG en_US.UTF-8

# Add credentials
#RUN sudo -u jenkins gpg --import /home/jenkins/credentials/gpg/gentics/gpg-key.asc
#RUN sudo -u jenkins gpg --import /home/jenkins/credentials/gpg/gentics/gpg-secret-key.asc

USER jenkins

