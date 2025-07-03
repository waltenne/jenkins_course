# 🐳 Módulo 6 - Gerenciamento de Agents Docker no Jenkins

<div align="center">
  <img src="https://images.icon-icons.com/2415/PNG/512/docker_original_wordmark_logo_icon_146557.png" width="100" alt="Docker Logo">
  <img src="https://cdn.icon-icons.com/icons2/2107/PNG/512/file_type_jenkins_icon_130515.png" width="100" alt="Jenkins Logo">
  <h3>Escalabilidade Flexível com Containers</h3>
</div>

## 1. 🏗️ Arquitetura Distribuída

### 🔄 Quando Usar Agents Docker

```
• Projetos com dependências conflitantes
• Ambientes de build isolados
• Necessidade de escalar rapidamente
• Execução de múltiplos builds paralelos
• Redução de overhead em agents físicos
```

### 📌 Tipos de Conexão

```
SSH:
✓ Comunicação criptografada
✓ Requer chaves SSH configuradas
✓ Baixo overhead

JNLP (Java Web Start):
✓ Conexão iniciada pelo agent
✓ Ideal para ambientes restritos
✓ Configuração mais complexa

Docker:
✓ Isolamento completo
✓ Provisionamento automático
✓ Recursos escaláveis
```

## 2. ⚙️ Configuração de Agents Docker

### 📦 Plugin Docker

```
1. Instale os plugins:
   - Docker
   - Docker Pipeline
   - Docker API

2. Configure no Jenkins:
   Manage Jenkins → Nodes → Configure Clouds → Add Docker Cloud

3. Conecte ao Docker Host:
   - URI: tcp://docker-host:2375
   - Credenciais (se necessário)
```

### 🎭 Templates Dinâmicos

```
pipeline {
    agent {
        docker {
            image 'maven:3.8.6-jdk-11'
            args '-v $HOME/.m2:/root/.m2'
            label 'docker-agent'
            registryUrl 'https://registry.example.com'
            registryCredentialsId 'docker-creds'
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
    }
}
```

### ⚖️ Controle de Recursos

```
dockerTemplate(
    image: 'python:3.9',
    label: 'python-agent',
    dockerCommand: '',
    volumes: ['/var/run/docker.sock:/var/run/docker.sock'],
    memoryLimit: '512m',
    cpuShares: 512,
    instanceCap: 5,
    idleTimeout: 10
)
```

## 3. 🚀 Casos de Uso Avançados

### 🏝️ Ambientes Isolados

```
// Jenkinsfile
stage('Teste NodeJS') {
    agent {
        docker {
            image 'node:16'
            args '--network=isolated'
        }
    }
    steps {
        sh 'npm install && npm test'
    }
}
```

### 📈 Escalabilidade Horizontal

```
// Configuração no Jenkins
dockerCloud {
    containerCap = 10
    connectTimeout = 60
    readTimeout = 60
    templates {
        dockerTemplate {
            label = 'dynamic-agent'
            image = 'jenkins/agent:latest'
            pullTimeout = 300
            pullStrategy = PULL_ALWAYS
        }
    }
}
```

### 🧹 Limpeza Automática

```
post {
    always {
        script {
            docker.image('maven:3.8.6').stop()
            docker.image('node:16').stop()
        }
        cleanWs()
    }
}
```

## 4. 🛠️ Configuração Completa

### 🐙 Docker Compose para Jenkins + Agents

```
version: '3.8'

services:
  jenkins:
    image: jenkins/jenkins:lts
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_data:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - DOCKER_HOST=tcp://docker:2375

  docker:
    image: docker:dind
    privileged: true
    volumes:
      - docker_data:/var/lib/docker

volumes:
  jenkins_data:
  docker_data:
```

## 5. 🔍 Troubleshooting

```
# Problemas comuns:
• Erro: "Cannot connect to the Docker daemon"
  Solução: Verificar permissões do socket Docker

• Erro: "Out of memory"
  Solução: Ajustar memoryLimit no template

• Erro: "Image pull failed"
  Solução: Verificar registry credentials

# Comandos úteis:
docker ps -a # Listar containers
docker logs <container> # Ver logs
docker system prune # Limpar recursos não utilizados
```

<div align="center">
  <sub>📌 Dica: Para ambientes críticos, considere Kubernetes para orquestração</sub>
</div>