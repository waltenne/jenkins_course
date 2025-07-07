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
        
        stage('Deploy to Tomcat (Optional)') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                script {
                    // Corrija o nome do arquivo WAR e o caminho
                    def warFile = "${env.PROJECT_DIR}/target/jenkins-demo.war"
                    
                    // Verifique se o arquivo existe antes de copiar
                    if (fileExists(warFile)) {
                        sh "mkdir -p /opt/tomcat/webapps/"
                        sh "cp ${warFile} /opt/tomcat/webapps/"
                        echo "Deployed jenkins-demo.war to Tomcat"
                    } else {
                        error "Arquivo WAR não encontrado: ${warFile}"
                    }
                }
            }
        }
    }
    
    post {
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
        
        success {
            echo "Build ${env.BUILD_NUMBER} completed successfully!"
        }
        
        failure {
            echo "Build ${env.BUILD_NUMBER} failed!"
        }
        
        unstable {
            echo "Build ${env.BUILD_NUMBER} is unstable!"
        }
    }
}