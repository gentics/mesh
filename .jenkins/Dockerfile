# This Dockerfile is used to build an image containing basic stuff to be used as a Jenkins slave build node.
FROM docker.apa-it.at/maven:3.9.2-amazoncorretto-17-debian-bullseye
USER root

ARG DEBIAN_RELEASE="bullseye"

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
                      bzip2 rsync gpg  \
  && curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - \
  && add-apt-repository "deb https://download.docker.com/linux/debian ${DEBIAN_RELEASE} stable" \
  && apt-get update \
  && apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin \
  && apt-get clean \
  && rm -Rf /var/lib/apt/lists/*


# kubectl
RUN curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.19.0/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/kubectl


# Add Jenkins user
RUN echo "jenkins:jenkins" | chpasswd
RUN mkdir -p /home/jenkins/workspace && mkdir -p /home/jenkins/.ssh

ADD ./files/settings.xml /opt/maven/conf/settings.xml
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

USER jenkins

ENV JAVA_HOME "/usr/lib/jvm/java-17-amazon-corretto/"
