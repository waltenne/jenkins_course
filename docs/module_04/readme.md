# 🚀 Jenkins Pipeline Intermediário - Módulo 4

<div align="center">
  <img src="https://images.icon-icons.com/2107/PNG/512/file_type_maven_icon_130397.png" width="100" alt="Maven Logo">
  <img src="https://images.icon-icons.com/2415/PNG/512/java_original_wordmark_logo_icon_146459.png" width="100" alt="Java Logo">
</div>

## 📘 Módulo 4 – Pipelines Intermediários

### 1. 🧩 Estruturas Complexas

**Execução Paralela**  
```groovy
stage('Build e Teste Paralelos') {
    parallel {
        stage('Build') {
            steps {
                sh 'mvn compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
    }
}
```

**Condicionais com `when`**  
```groovy
stage('Deploy Staging') {
    when {
        branch 'develop'
        environment name: 'DEPLOY_ENV', value: 'staging'
    }
    steps {
        sh './deploy-to-staging.sh'
    }
}
```

**Controle de Tempo e Tentativas**  
```groovy
stage('Processo Demorado') {
    options {
        timeout(time: 15, unit: 'MINUTES')
        retry(3)
    }
    steps {
        sh './long-running-process.sh'
    }
}
```

### 2. 🔧 Integração com Ferramentas

**Build Java/Maven Completo**  
```groovy
stage('Build Maven') {
    steps {
        sh 'mvn clean package'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
    }
    post {
        success {
            junit 'target/surefire-reports/**/*.xml'
        }
    }
}
```

**Pipeline com Relatórios**  
```groovy
post {
    always {
        junit '**/target/surefire-reports/**/*.xml'
        archiveArtifacts artifacts: '**/target/*.jar,**/target/*.war'
        publishHTML target: [
            allowMissing: false,
            alwaysLinkToLastBuild: false,
            keepAll: true,
            reportDir: 'target/site',
            reportFiles: 'index.html',
            reportName: 'Relatório de Documentação'
        ]
    }
}
```

### 3. 🛡️ Práticas Intermediárias

**Parâmetros de Pipeline**  
```groovy
pipeline {
    agent any
    parameters {
        string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')

        text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')

        booleanParam(name: 'TOGGLE', defaultValue: true, description: 'Toggle this value')

        choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')

        password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
    }
    stages {
        stage('Example') {
            steps {
                echo "Hello ${params.PERSON}"

                echo "Biography: ${params.BIOGRAPHY}"

                echo "Toggle: ${params.TOGGLE}"

                echo "Choice: ${params.CHOICE}"

                echo "Password: ${params.PASSWORD}"
            }
        }
    }
}
```

**Gerenciamento Seguro de Credenciais**  
```groovy
stage('Acesso Seguro') {
    steps {
        withCredentials([
            usernamePassword(
                // db-creds é o ID da credencial cadastrada no Jenkins
                credentialsId: 'db-creds',
                usernameVariable: 'DB_USER',
                passwordVariable: 'DB_PASS'
            )
        ]) {
            sh '''
                echo "Conectando como ${DB_USER}"
                // comando que usa as credenciais
            '''
        }
    }
}
```

## 🔍 Fluxo Completo Exemplo
```groovy
pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }
    parameters {
        choice(name: 'ENV', choices: ['DEV', 'STAGING', 'PROD'])
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/meu/repo.git'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn -B clean package'
            }
        }
        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh 'mvn test'
                    }
                }
                stage('Integration Tests') {
                    when {
                        expression { params.ENV != 'PROD' }
                    }
                    steps {
                        sh 'mvn verify -Pintegration'
                    }
                }
            }
        }
        stage('Deploy') {
            when {
                expression { params.ENV in ['STAGING', 'PROD'] }
            }
            steps {
                script {
                    echo "Deploying to ${params.ENV}"
                    // Comandos de deploy aqui
                }
            }
        }
    }
    post {
        always {
            junit '**/target/surefire-reports/**/*.xml'
            archiveArtifacts 'target/*.jar'
        }
        success {
            emailext subject: 'SUCESSO: Build ${JOB_NAME}',
                      body: 'O build foi concluído com sucesso!',
                      to: 'team@example.com'
        }
        failure {
            emailext subject: 'FALHA: Build ${JOB_NAME}',
                      body: 'O build falhou. Verifique: ${BUILD_URL}',
                      to: 'team@example.com'
        }
    }
}
```


## 📌 Checklist de Boas Práticas

- [ ] Usar estágios paralelos para otimização
- [ ] Implementar tratamento de erros robusto
- [ ] Versionar todos os Jenkinsfiles
- [ ] Usar credenciais gerenciadas pelo Jenkins
- [ ] Adicionar timeouts para operações críticas
- [ ] Gerar relatórios de qualidade (JUnit, Jacoco)
- [ ] Parametrizar builds quando necessário