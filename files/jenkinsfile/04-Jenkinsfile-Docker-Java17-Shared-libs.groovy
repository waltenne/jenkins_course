@Library('my-shared-library') _  // substitua pelo nome real da sua shared library

pipeline {
    agent { label 'Docker' }

    environment {
        PROJECT_DIR = 'files/projects/java-17-example'
        COMMIT_PATTERN = '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-z]+\\))?(!)?: .+'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    pipelineUtils.checkoutGit(
                        branch: 'doc/jenkins',
                        repoUrl: 'https://github.com/waltenne/jenkins_course.git',
                        targetDir: '.'
                    )
                }
            }
        }

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
                    env.RELEASE_VERSION = pipelineUtils.incrementVersion(
                        projectDir: env.PROJECT_DIR,
                        commitType: env.COMMIT_TYPE,
                        isBreakingChange: env.IS_BREAKING_CHANGE
                    )
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
                    pipelineUtils.build(
                        projectDir: env.PROJECT_DIR,
                        goals: ['test'],
                        quiet: true,
                        skipTests: false
                    )
                }
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    pipelineUtils.build(
                        projectDir: env.PROJECT_DIR,
                        goals: ['verify'],
                        quiet: true,
                        skipTests: false,
                        mavenProfiles: ['integration']
                    )
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

        stage('Generate Release Notes') {
            steps {
                script {
                    pipelineUtils.generateReleaseNotes(
                        releaseVersion: env.RELEASE_VERSION
                    )
                }
            }
        }

        stage('Deploy to Tomcat (Optional)') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                script {
                    pipelineUtils.deployToTomcat(
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
                    pipelineUtils.processTestReports(
                        projectDir: env.PROJECT_DIR,
                        cleanAfter: true,
                        quiet: true
                    )
                    pipelineUtils.logSummary()
                }
            }
            echo "Build ${env.BUILD_NUMBER} concluída com sucesso!"

        }
        failure {
            echo "Build ${env.BUILD_NUMBER} falhou!"
        }
        unstable {
            echo "Build ${env.BUILD_NUMBER} está instável!"
        }
    }
}
