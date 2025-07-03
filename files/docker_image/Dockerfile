# Usa Maven oficial com JDK 17
FROM maven:3.9-eclipse-temurin-17

# Instala Node.js LTS
RUN curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt-get update && \
    apt-get install -y nodejs && \
    apt-get clean

# Cria diretório de trabalho do Jenkins Agent
RUN mkdir -p /home/jenkins

# Define diretório de trabalho
WORKDIR /home/jenkins

# Verifica instalações
RUN java -version && \
    mvn -version && \
    node --version && \
    npm --version
