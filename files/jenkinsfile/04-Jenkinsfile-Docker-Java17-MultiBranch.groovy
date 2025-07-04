@Library('jenkins-shared-libs') _

properties([
    disableConcurrentBuilds(),
    buildDiscarder(logRotator(numToKeepStr: '5'))
])

ansiColor('xterm') {
    pipeline {
        agent { label 'Docker' }

        environment {
            PROJECT_DIR = 'files/projects/java-17-example'
            COMMIT_PATTERN = '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-zA-Z0-9_-]+\\))?(!)?: .+'
            MAVEN_OPTS = '-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn' // Reduz logs do Maven
        }

        stages {
            stage('Validate Commit') {
                steps {
                    script {
                        echo "\033[36m[INFO]\033[0m Validando mensagem de commit..."
                        pipelineUtils.validateCommit(env.COMMIT_PATTERN)
                    }
                }
            }

            stage('Increment Version') {
                steps {
                    script {
                        echo "\033[36m[INFO]\033[0m Incrementando versão..."
                        env.RELEASE_VERSION = pipelineUtils.incrementVersion(env.PROJECT_DIR)
                        echo "\033[32m[SUCCESS]\033[0m Versão liberada: \033[1m${env.RELEASE_VERSION}\033[0m"
                    }
                }
            }

            stage('Build') {
                steps {
                    script {
                        echo "\033[36m[INFO]\033[0m Iniciando build..."
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
                        echo "\033[36m[INFO]\033[0m Executando testes unitários..."
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
                        echo "\033[36m[INFO]\033[0m Executando testes de integração..."
                        pipelineUtils.integrationTests(
                            projectDir: env.PROJECT_DIR,
                            mavenProfiles: ['integration'],
                            quiet: true
                        )
                    }
                }
            }

            stage('Archive Artifact') {
                steps {
                    script {
                        echo "\033[36m[INFO]\033[0m Arquivos sendo compactados..."
                        pipelineUtils.archiveArtifact(
                            projectDir: env.PROJECT_DIR,
                            pattern: 'target/*.war'
                        )
                    }
                }
            }
        }

        post {
            always {
                script {
                    pipelineUtils.processTestReports(
                        projectDir: env.PROJECT_DIR,
                        cleanAfter: true,
                        quiet: true
                    )
                }
            }
            success {
                script {
                    echo "\033[32m==========================================="
                    echo "         BUILD ${env.BUILD_NUMBER} SUCESSO         "
                    echo "===========================================\033[0m"
                    pipelineUtils.generateReleaseNotes()
                    currentBuild.description = pipelineUtils.generateReleaseDashboard()
                }
            }
            failure {
                script {
                    echo "\033[31m==========================================="
                    echo "         BUILD ${env.BUILD_NUMBER} FALHOU          "
                    echo "===========================================\033[0m"
                }
            }
        }
    }
}