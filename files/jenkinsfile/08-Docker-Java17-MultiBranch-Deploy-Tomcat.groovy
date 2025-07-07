@Library('jenkins-shared-libs') _

pipeline {
    agent { label 'Docker' }

    environment {
        PROJECT_DIR = 'files/projects/java-17-example'
        COMMIT_PATTERN = '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-zA-Z0-9_-]+\\))?(!)?: .+'
    }

    stages {
        stage('Validate Commit') {
            steps {
                script {
                    pipelineUtils.validateCommit(env.COMMIT_PATTERN)
                }
            }
        }

        stage('Increment Version') {
            steps {
                script {
                    env.RELEASE_VERSION = pipelineUtils.incrementVersion(env.PROJECT_DIR)
                    echo "Versão liberada: ${env.RELEASE_VERSION}"
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    pipelineUtils.build(
                        projectDir: env.PROJECT_DIR,
                        goals: ['clean', 'package'],
                        quiet: true,
                        skipTests: true
                    )
                }
            }
        }

        stage('Unit Tests') {
            steps {
                script {
                    pipelineUtils.unitTests(
                        projectDir: env.PROJECT_DIR,
                        quiet: true
                    )
                }
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    pipelineUtils.integrationTests(
                        projectDir: env.PROJECT_DIR,
                        mavenProfiles: ['integration'],
                        quiet: true
                    )
                }
            }
        }        

        stage('Generate Release Notes') {
            steps {
                script {
                    pipelineUtils.generateReleaseNotes()
                }
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                script {
                    dir(env.PROJECT_DIR) {
                        // Primeiro verifica se o arquivo existe
                        def warFile = "target/jenkins-demo-${env.RELEASE_VERSION}.war"
                        if (!fileExists(warFile)) {
                            error "Arquivo WAR não encontrado: ${warFile}"
                        }
                        
                        pipelineUtils.deployToTomcat(
                            warFile: warFile,
                            tomcatUrl: 'http://tomcat:8080',
                            contextPath: 'jenkins-demo', // Sem a versão e sem .war
                            quiet: false 
                        )
                    }
                }
            }
        }

        stage('Archive Artifact') {
            steps {
                script {
                    pipelineUtils.archiveArtifact(
                        projectDir: env.PROJECT_DIR,
                        pattern: 'target/*.war'
                    )
                }
            }
        }
    }

    post {
        success {
            script {
                pipelineUtils.processTestReports(
                    projectDir: env.PROJECT_DIR,
                    cleanAfter: true,
                    quiet: true
                )
                pipelineUtils.logSummary()
                echo "Build ${env.BUILD_NUMBER} concluída com sucesso!"
                currentBuild.description = pipelineUtils.generateReleaseDashboard()
                pipelineUtils.archiveReleaseInfo()
            }
        }
        failure {
            script {
                pipelineUtils.logSummary()
                echo "Build ${env.BUILD_NUMBER} falhou!"
            }
        }
        unstable {
            script {
                pipelineUtils.logSummary()
                echo "Build ${env.BUILD_NUMBER} está instável!"
            }
        }
    }
}

properties([
    pipelineTriggers([]),
    buildDiscarder(logRotator(numToKeepStr: '10')),
    [$class: 'jenkins.model.BuildDiscarderProperty', strategy: [$class: 'hudson.tasks.LogRotator', numToKeepStr: '10']]
])