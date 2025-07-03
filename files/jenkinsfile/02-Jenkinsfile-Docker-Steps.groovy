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
