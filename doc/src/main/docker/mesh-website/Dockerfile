FROM nginx:1.9.5
MAINTAINER Johannes Schüth <j.schueth@gentics.com>

RUN echo 'Acquire::HTTP::Proxy "http://10.43.84.216:3142";' >> /etc/apt/apt.conf.d/01proxy \
 && echo 'Acquire::HTTPS::Proxy "false";' >> /etc/apt/apt.conf.d/01proxy

 # Make sure the package repository is up to date.
RUN echo 'deb http://ftp.at.debian.org/debian jessie main contrib non-free' > /etc/apt/sources.list
RUN echo 'deb http://security.debian.org/ jessie/updates main contrib' >> /etc/apt/sources.list

RUN apt-get update ; apt-get -y upgrade ; apt-get install -y locales
RUN rm /etc/nginx/conf.d/*
RUN mkdir -p /opt/mesh/htdocs
ADD website.conf /etc/nginx/conf.d/website.conf

 # Setup locale/timezone
RUN echo "LC_ALL=en_US.UTF-8" > /etc/default/locale
RUN echo "LANG=en_US.UTF-8" >> /etc/default/locale
RUN echo "LANGUAGE=en_US.UTF-8" > /etc/default/locale

RUN sed -i  's/.*\(en_US.UTF-8.*\)/\1/' /etc/locale.gen 
RUN locale-gen
RUN cp /usr/share/zoneinfo/Europe/Vienna /etc/localtime 
RUN echo "Europe/Vienna" > /etc/timezone 

EXPOSE 80 443

# forward request and error logs to docker log collector
RUN mkdir -p /opt/mesh/log
RUN ln -sf /dev/stdout /opt/mesh/log/access.log
RUN ln -sf /dev/stderr /opt/mesh/log/error.log

ADD ./maven/website /opt/mesh/htdocs/website
