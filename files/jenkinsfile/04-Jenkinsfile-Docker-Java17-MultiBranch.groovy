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
                    pipelineUtils.generateReleaseNotes()
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
    dashboardView(
        columns: [
            { it -> hudson.views.StatusColumn() },
            { it -> hudson.views.WeatherColumn() },
            { it -> hudson.views.JobColumn() },
            { it -> hudson.views.LastSuccessColumn() },
            { it -> hudson.views.LastFailureColumn() },
            { it -> hudson.views.BuildButtonColumn() }
        ],
        portlets: [
            { it -> hudson.plugins.view.dashboard.test.TestStatisticsPortlet() },
            { it -> new hudson.plugins.view.dashboard.core.IframePortlet(
                url: "${env.JENKINS_URL}job/${env.JOB_NAME}/lastSuccessfulBuild/artifact/release_info.html",
                height: "400px"
            )}
        ],
        includeRegex: '.*',
        recurse: false
    )
])