pipeline {
    agent {
        label 'Docker'
    }
    
    environment {
        PROJECT_DIR = 'files/projects/java-17-example'
        ARTIFACT_NAME = 'java17-example.war'
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: 'doc/jenkins']],
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '.']],
                    userRemoteConfigs: [[url: 'https://github.com/waltenne/jenkins_course.git']]
                ])
                sh 'git --no-pager log -1 --oneline' // Mostra o último commit
            }
        }
        
        stage('Build') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn --version' // Verifica a versão do Maven
                    sh 'mvn clean package -DskipTests' // Build rápido sem testes
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn test' // Executa apenas testes unitários
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn verify -DskipUnitTests' // Executa apenas testes de integração
                }
            }
        }
        
        stage('Archive Artifact') {
            steps {
                dir(env.PROJECT_DIR) {
                    archiveArtifacts artifacts: "target/*.war", onlyIfSuccessful: true
                }
            }
        }
    }
    
    post {
        // O bloco 'always' executa sempre, independentemente do resultado do pipeline
        // Dependendo da situação poderá ter problemas da pipeline quebrar ou erros, pois sempre será executado
        // Nesse exemplo, ele busca os relatórios de testes para serem exibidos na notificação
        // Caso algum step anterior de problema nao seja executado, os relatórios nao serao encontrados
        // E isso pode causar erros
        always {
            dir(env.PROJECT_DIR) {
                junit 'target/surefire-reports/**/*.xml'
                junit 'target/failsafe-reports/**/*.xml'
                
                // Limpeza opcional
                sh 'mvn clean'
            }
            
            // Notificação de status
            script {
                def duration = currentBuild.durationString.replace(' and counting', '')
                def msg = "${currentBuild.currentResult}: Job ${env.JOB_NAME} #${env.BUILD_NUMBER}\n" +
                         "Duration: ${duration}\n" +
                         "URL: ${env.BUILD_URL}"
                
                // Adapte para seu sistema de notificação (Slack, Email, etc.)
                echo msg
                // slackSend color: currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger', message: msg
            }
        }
        // O step 'sucess', será executado apenas se todos os steps contidos na pipeline foram executados com sem erro
        success {
            echo "Build ${env.BUILD_NUMBER} completed successfully!"
        }
        // O step 'failure', será executado apenas se tiver algum step com erro
        failure {
            echo "Build ${env.BUILD_NUMBER} failed!"
        }
        // O step 'unstable', será executado se o status do build mudar para instável
        unstable {
            echo "Build ${env.BUILD_NUMBER} is unstable!"
        }
        // O step 'changed', é executado se o status mudou em relação ao build anterior
        changed {
            echo "Build ${env.BUILD_NUMBER} status has changed!"
        }
    }
}