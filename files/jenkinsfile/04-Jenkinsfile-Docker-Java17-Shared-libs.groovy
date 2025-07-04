@Library('jenkins-shared-libs') _

// Inicializa a shared library
def utils = new PipelineUtils(this)

pipeline {
    agent { label 'Docker' }

    environment {
        PROJECT_DIR = 'files/projects/java-17-example'
        COMMIT_PATTERN = '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-zA-Z0-9_-]+\\))?(!)?: .+'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    utils.libCheckout(
                        repoUrl: 'https://github.com/waltenne/jenkins_course.git',
                        branchName: 'doc/jenkins',
                        relativeDir: '.'
                    )
                }
            }
        }

        stage('Validate Commit') {
            steps {
                script {
                    utils.validateCommit(env.COMMIT_PATTERN)
                }
            }
        }

        stage('Increment Version') {
            steps {
                script {
                    env.RELEASE_VERSION = utils.incrementVersion(env.PROJECT_DIR)
                    echo "Versão liberada: ${env.RELEASE_VERSION}"
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    utils.build(
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
                    utils.unitTests(
                        projectDir: env.PROJECT_DIR,
                        quiet: true
                    )
                }
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    utils.integrationTests(
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
                    utils.archiveArtifact(
                        projectDir: env.PROJECT_DIR,
                        pattern: 'target/*.war'
                    )
                }
            }
        }

        stage('Generate Release Notes') {
            steps {
                script {
                    utils.generateReleaseNotes()
                }
            }
        }

        stage('Deploy to Tomcat (Optional)') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                script {
                    utils.deployToTomcat(
                        projectDir: env.PROJECT_DIR,
                        releaseVersion: env.RELEASE_VERSION
                    )
                }
            }
        }
    }

    post {
        success {
            script {
                utils.processTestReports(
                    projectDir: env.PROJECT_DIR,
                    cleanAfter: true,
                    quiet: true
                )
                utils.logSummary()
                echo "Build ${env.BUILD_NUMBER} concluída com sucesso!"
            }
        }
        failure {
            script {
                utils.logSummary()
                echo "Build ${env.BUILD_NUMBER} falhou!"
            }
        }
        unstable {
            script {
                utils.logSummary()
                echo "Build ${env.BUILD_NUMBER} está instável!"
            }
        }
    }
}