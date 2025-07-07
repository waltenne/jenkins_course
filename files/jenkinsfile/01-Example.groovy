pipeline {
    agent any
    stages {
        stage('Verify') {
            steps {
                sh 'java -version'
                sh 'mvn -version'
                sh 'node --version'
                sh 'npm --version'
                echo 'Tudo instalado e funcionando!'
            }
        }
    }
}
