# This Dockerfile is used to create a Jenkins agent that connects to the controller and is used to as a worker
FROM jenkins/inbound-agent:latest-jdk17

ARG DEBIAN_RELEASE="bullseye"
ARG USER_NAME="jenkins"
ARG GIT_USER_NAME="Maven Release Manager"
ARG GIT_USER_EMAIL="entwicklung@gentics.com"

USER root

RUN echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE} main contrib non-free" > /etc/apt/sources.list \
  && echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE}-backports main" >> /etc/apt/sources.list \
  && echo "deb http://security.debian.org/debian-security ${DEBIAN_RELEASE}-security main" >> /etc/apt/sources.list \
  && echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE}-updates main" >> /etc/apt/sources.list \
  && apt-get update \
  && apt-get -y upgrade \
  && apt-get install -y git-core gawk sed wget curl sudo \
                      less vim curl tar wget sed psmisc \
                      file pwgen locales zip unzip \
                      software-properties-common devscripts \
                      bzip2 rsync gpg moreutils \
  && curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - \
  && add-apt-repository "deb https://download.docker.com/linux/debian ${DEBIAN_RELEASE} stable" \
  && apt-get update \
  && apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin \
  && apt-get clean \
  && rm -Rf /var/lib/apt/lists/*

# Maven
RUN cd /opt && wget -nv -O maven.tar.gz "https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz" && \
    tar -xzf maven.tar.gz && mv apache-maven* maven && rm maven.tar.gz && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

# kubectl
RUN curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.28.4/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/kubectl

# Docker compose v1
RUN curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose \
  && chmod +x /usr/local/bin/docker-compose

ADD build/settings.xml /opt/maven/conf/settings.xml
ADD build/config /home/${USER_NAME}/.ssh/config
RUN ssh-keyscan github.com >> /home/${USER_NAME}/.ssh/known_hosts
RUN ssh-keyscan git.gentics.com >> /home/${USER_NAME}/.ssh/known_hosts
RUN mkdir -p /home/${USER_NAME}/.m2/repository

# Update Bash RC
RUN rm /home/${USER_NAME}/.bashrc
RUN echo '. /etc/profile.d/*.sh' > /home/${USER_NAME}/.bashrc

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
RUN git config --system user.name "${GIT_USER_NAME}" \
 && git config --system user.email "${GIT_USER_EMAIL}"

RUN chown ${USER_NAME}: /opt -R
RUN usermod -aG docker ${USER_NAME}
USER ${USER_NAME}

ENV LANG en_US.UTF-8
ENV TZ=Europe/Vienna