# This Dockerfile is used to create a Jenkins agent that connects to the controller and is used to as a worker
FROM jenkins/inbound-agent:latest-jdk17

ARG DEBIAN_RELEASE="bullseye"
ARG USER_NAME="jenkins"
ARG GIT_USER_NAME="Maven Release Manager"
ARG GIT_USER_EMAIL="entwicklung@gentics.com"
ARG NODE_VERSION=20.12.1

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
                      build-essential gnupg2 \ 
  && curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - \
  && add-apt-repository "deb https://download.docker.com/linux/debian ${DEBIAN_RELEASE} stable" \
  && apt-get update \
  && apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin \
  && apt-get clean \
  && rm -Rf /var/lib/apt/lists/*

# Maven
RUN cd /opt && wget -nv -O maven.tar.gz "https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz" && \
    tar -xzf maven.tar.gz && mv apache-maven* maven && rm maven.tar.gz && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

# kubectl
RUN curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.28.4/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/kubectl

# Docker compose v1
RUN curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose \
  && chmod +x /usr/local/bin/docker-compose

# Update public keys for node installs.
RUN set -ex \
	&& for key in \
		4ED778F539E3634C779C87C6D7062848A1AB005C \
		141F07595B7B3FFE74309A937405533BE57C7D57 \
		74F12602B6F1C4E913FAA37AD3A89613643B6201 \
		DD792F5973C6DE52C432CBDAC77ABFA00DDBF2B7 \
		CC68F5A3106FF448322E48ED27F5E38D5B0A215F \
		8FCCA13FEF1D0C2E91008E09770F7A9A5AE15600 \
		C4F0DFFF4E8C1A8236409D08E73BC641CC11F4C8 \
		890C08DB8579162FEE0DF9DB8BEAB4DFCF555EF4 \
		C82FA3AE1CBEDC6BE46B9360C43CEC45C17AB93C \
		108F52B48DB57BB0CC439B2997B01419BD92F80A \
		A363A499291CBBC940DD62E41F10027AF002F8B0 \
		; do \
			gpg2 --batch --keyserver keys.openpgp.org --recv-keys "$key"; \
		done

# Download NodeJS
RUN mkdir /opt/node \
  && curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.tar.xz" \
  && curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt" \
  && curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt.sig"
# Verify downloaded NodeJS files
RUN gpg --verify SHASUMS256.txt.sig SHASUMS256.txt \
  && grep " node-v$NODE_VERSION-linux-x64.tar.xz\$" SHASUMS256.txt | sha256sum -c -
# Install NodeJS
RUN tar -xJf "node-v$NODE_VERSION-linux-x64.tar.xz" -C /opt/node --strip-components=1 \
  && rm "node-v$NODE_VERSION-linux-x64.tar.xz" SHASUMS256.txt.sig SHASUMS256.txt \
  && ln -s /opt/node/bin/node /usr/local/bin/node \
  && ln -s /opt/node/bin/npm /usr/local/bin/npm \
  && ln -s /opt/node/bin/npx /usr/local/bin/npx \
  && echo "export PATH=\$PATH:/opt/node/bin" > /etc/profile.d/nodejs.sh \
  && chmod +x /etc/profile.d/* \
  && chown -R 10000:10000 /opt/node

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