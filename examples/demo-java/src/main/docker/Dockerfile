
FROM     debian:wheezy
MAINTAINER Johannes Schüth "j.schueth@gentics.com"

RUN echo 'deb http://ppa.launchpad.net/webupd8team/java/ubuntu precise main' > /etc/apt/sources.list.d/java.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886

ENV DEBIAN_FRONTEND noninteractive

RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections

# Install required apt packages
RUN apt-get update && apt-get install -y \
        oracle-java8-installer \
        oracle-java8-set-default \
        locales \
        php5 apache2 libapache2-mod-php5 \
        openssh-server


#APACHE2 / PHP
RUN a2enmod rewrite
RUN a2enmod headers
RUN rm /etc/apache2/sites-enabled/*
RUN rm /var/www/index.html
EXPOSE 80

#Mesh
EXPOSE 8080
EXPOSE 7474

#SSHD
RUN mkdir /var/run/sshd
RUN echo 'root:finger' | chpasswd
RUN sed -i 's/PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
EXPOSE 22

RUN mkdir -p /opt/mesh
ADD ./mesh-starter.jar /opt/mesh/
ADD ./demo/ /var/www/
ADD ./mesh-demo.conf /etc/apache2/sites-enabled/

# Add init script
ADD ./startup.sh /opt/startup.sh
CMD ["/bin/bash", "/opt/startup.sh"]
