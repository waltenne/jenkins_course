# 🚀 Jenkins Pipeline Avançado - Módulo 5

<div align="center">
  <img src="https://images.icon-icons.com/2699/PNG/512/kubernetes_logo_icon_168359.png" width="80" alt="Kubernetes Logo">
  <img src="https://images.icon-icons.com/2389/PNG/512/amazon_aws_logo_icon_145507.png" width="80" alt="AWS Logo">
  <img src="https://cdn.icon-icons.com/icons2/2699/PNG/512/microsoft_azure_logo_icon_170956.png" width="80" alt="Azure Logo">
  <img src="https://images.icon-icons.com/2699/PNG/512/google_cloud_logo_icon_171058.png" width="80" alt="GCP Logo">
</div>

## 📘 Módulo 5 – Pipelines Avançados

### 1. � Padrões Avançados

**Pipeline como Código (Jenkinsfile em SCM)**  
```
// Jenkinsfile no repositório do projeto
// Jenkins automaticamente detecta e executa
pipeline {
    agent any
    triggers {
        pollSCM('H/5 * * * *') // Verifica mudanças a cada 5 minutos
    }
    stages {
        stage('Build') {
            steps {
                checkout scm // Obtém código do mesmo repositório
                sh 'make build'
            }
        }
    }
}
```

**Multibranch Pipelines**  
```
// Jenkinsfile configurado para múltiplos branches
def BRANCH_NAME = env.BRANCH_NAME

pipeline {
    agent none
    stages {
        stage('Build') {
            when {
                expression { BRANCH_NAME ==~ /(feature|bugfix|hotfix)\/.*/ }
            }
            agent { label 'builder' }
            steps {
                echo "Building branch: ${BRANCH_NAME}"
                sh 'mvn clean package'
            }
        }
    }
}
```

### 2. ☁️ Integração com Cloud

**Deploy em Kubernetes**  
```
stage('Deploy K8s') {
    environment {
        KUBECONFIG = credentials('kubeconfig')
    }
    steps {
        sh '''
        kubectl apply -f k8s/deployment.yaml
        kubectl rollout status deployment/my-app
        '''
    }
}
```

**Integração AWS (ECR + ECS)**  
```
stage('Build and Push to AWS') {
    steps {
        script {
            docker.build("${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/my-app:${BUILD_NUMBER}")
            docker.withRegistry('https://${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com', 'ecr:us-east-1:aws-credentials') {
                docker.image("${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/my-app:${BUILD_NUMBER}").push()
            }
        }
    }
}
```

**Terraform + Jenkins**  
```
stage('Provision Infrastructure') {
    environment {
        TF_VAR_access_key = credentials('aws-access-key')
        TF_VAR_secret_key = credentials('aws-secret-key')
    }
    steps {
        dir('terraform') {
            sh 'terraform init'
            sh 'terraform plan -out=tfplan'
            sh 'terraform apply -auto-approve tfplan'
        }
    }
}
```

### 3. ⚡ Otimização

**Caching de Dependências**  
```
stage('Build with Cache') {
    steps {
        script {
            // Cache para Maven
            withMaven(
                maven: 'maven-3.8.6',
                mavenLocalRepo: '.repository',
                options: [
                    artifactsPublisher(disabled: true),
                    junitPublisher(disabled: true)
                ]) {
                sh 'mvn clean package'
            }
        }
    }
}
```

**Execução Distribuída**  
```
pipeline {
    agent none
    stages {
        stage('Build') {
            agent { label 'linux && docker' }
            steps {
                sh 'make build'
            }
        }
        stage('Test') {
            agent { label 'windows && jdk11' }
            steps {
                bat 'mvn test'
            }
        }
    }
}
```

**Execução com o Label do Doker**

Definindo o agent como `label` dessa forma  ```agent { label 'Docker' }``` o Jenkins irá escalar a execução para qualquer Slave que tenha o label `Docker`, seguindo a configuração feita será escalado um container para cada execução, mas por exemplo caso tenha slaves fixos como por exemplo ec2, pode ser utilizado a mesma estratégia que o Jenkins irá escalar a execução para qualquer Slave que tenha a mesma label.

```
pipeline {
    agent { label 'Docker' }
    stages {
        stage('Verify Java') {
            steps {
                sh 'java -version'
                echo 'Java instalado e funcionando!'
            }
        }
        stage('Verify MVN') {
            steps {
                sh 'mvn -version'
                echo 'Maven instalado e funcionando!'
            }
        }
        stage('Verify NODE') {
            steps {
                sh 'node --version'
                echo 'Node instalado e funcionando!'
            }
        }
        stage('Verify NPM') {
            steps {
                sh 'npm --version'
                echo 'NPM instalado e funcionando!'
            }
        }
    }
}
```


**Monitoramento com Prometheus**  
```
post {
    always {
        script {
            prometheusMetric(
                name: 'build_duration_seconds',
                type: 'GAUGE',
                help: 'Build duration in seconds',
                labels: [
                    project: "${JOB_NAME}",
                    branch: "${GIT_BRANCH}"
                ],
                value: currentBuild.duration / 1000
            )
        }
    }
}
```

## 🛠️ Pipeline Avançado Completo
```
pipeline {
    agent none
    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 1, unit: 'HOURS')
        timestamps()
        disableConcurrentBuilds()
    }
    environment {
        DOCKER_REGISTRY = "registry.example.com"
        KUBE_NAMESPACE = "production"
    }
    stages {
        stage('Checkout') {
            agent { label 'git' }
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/main']],
                extensions: [[$class: 'CloneOption', depth: 1]],
                userRemoteConfigs: [[credentialsId: 'git-ssh', url: 'git@github.com:my/repo.git']]])
            }
        }
        stage('Build & Test') {
            agent { label 'docker && maven' }
            steps {
                withMaven(maven: 'maven-3.8.6') {
                    sh 'mvn -B clean package'
                    junit '**/target/surefire-reports/**/*.xml'
                }
            }
        }
        stage('Containerize') {
            agent { label 'docker' }
            steps {
                script {
                    docker.build("${DOCKER_REGISTRY}/my-app:${BUILD_TAG}")
                }
            }
        }
        stage('Deploy to K8s') {
            agent { label 'kubectl' }
            when {
                branch 'main'
            }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh "kubectl -n ${KUBE_NAMESPACE} apply -f k8s/"
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: '**/target/*.jar,**/target/*.war', fingerprint: true
            cleanWs()
        }
        success {
            slackSend(color: 'good', message: "SUCCESS: ${JOB_NAME} #${BUILD_NUMBER}")
        }
        failure {
            slackSend(color: 'danger', message: "FAILED: ${JOB_NAME} #${BUILD_NUMBER}")
        }
    }
}
```

## 4. 🔧 Dicas Avançadas

### 🛡️ Segurança

```
# Sempre use credenciais gerenciadas
withCredentials([
  usernamePassword(
    credentialsId: 'docker-creds',
    usernameVariable: 'DOCKER_USER',
    passwordVariable: 'DOCKER_PASS'
  )
]) {
  sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
}
```

### ⚡ Otimização de Performance

```
# Cache de dependências Maven
stage('Build') {
  steps {
    withMaven(
      maven: 'maven-3.8.6',
      mavenLocalRepo: '.m2/repository',
      options: [artifactsPublisher(disabled: true)]
    ) {
      sh 'mvn clean package'
    }
  }
}
```

### 🔄 Agentes Dinâmicos

```
pipeline {
  agent none
  stages {
    stage('Build') {
      agent {
        docker {
          image 'maven:3.8.6-jdk-11'
          args '-v $HOME/.m2:/root/.m2'
        }
      }
      steps {
        sh 'mvn clean package'
      }
    }
  }
}
```



## 📌 Boas Práticas

✔️ Sempre versionar Jenkinsfiles junto com o código  
✔️ Usar credenciais gerenciadas pelo Jenkins  
✔️ Implementar tratamento de erros básico  

```
// Exemplo de tratamento de erro
stage('Deploy') {
    steps {
        script {
            try {
                sh './deploy.sh'
            } catch (err) {
                echo "Erro no deploy: ${err}"
                currentBuild.result = 'FAILURE'
            }
        }
    }
}
```