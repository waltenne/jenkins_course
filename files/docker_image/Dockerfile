# Usa Maven oficial com JDK 17
FROM maven:3.9-eclipse-temurin-17

# Instala Node.js LTS e configura locale UTF-8
RUN curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        nodejs \
        locales && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Configura locale para UTF-8
RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen && \
    update-locale LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8

# Variáveis de ambiente com sintaxe correta
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

# Cria e define diretório de trabalho do Jenkins Agent
RUN mkdir -p /home/jenkins
WORKDIR /home/jenkins

# Verifica instalações
RUN java -version && \
    mvn -version && \
    node --version && \
    npm --version