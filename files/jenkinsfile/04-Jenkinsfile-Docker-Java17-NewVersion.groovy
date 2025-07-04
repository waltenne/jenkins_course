pipeline {
    agent {
        label 'Docker'
    }
    
    environment {
        PROJECT_DIR = 'files/projects/java-17-example'
        ARTIFACT_NAME = 'java17-example.war'
        COMMIT_PATTERN = '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-z]+\\))?(!)?: .+'
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
            }
        }

        stage('Validate Commit') {
            steps {
                script {
                    def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    
                    // Verificação mais robusta do padrão de commit
                    def matcher = (commitMsg =~ /^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\([a-z]+\))?(!)?: .+/)
                    
                    if (!matcher.matches()) {
                        error """
                        [ERRO] Mensagem de commit inválida!
                        Padrão esperado: tipo(escopo opcional): descrição
                        Exemplo válido: 'fix: corrige problema na autenticação'
                        Mensagem recebida: '${commitMsg}'
                        """
                    }
                    
                    // Extração segura dos componentes do commit
                    env.COMMIT_TYPE = matcher[0][1] ?: 'fix' // Default para 'fix' se não conseguir extrair
                    env.IS_BREAKING_CHANGE = (matcher[0][3] == '!') ? 'true' : 'false'
                    
                    echo "Tipo de commit: ${env.COMMIT_TYPE}"
                    echo "Breaking change: ${env.IS_BREAKING_CHANGE}"
                }
            }
        }

        stage('Increment Version') {
            steps {
                dir(env.PROJECT_DIR) {
                    script {
                        def pom = readMavenPom file: 'pom.xml'
                        def currentVersion = pom.version.replace('-SNAPSHOT', '')
                        def versionParts = currentVersion.tokenize('.').collect { it.toInteger() }
                        echo "Versão atual: ${currentVersion}"
                        // Lógica de incremento com fallback seguro
                        try {
                            if (env.IS_BREAKING_CHANGE == 'true') {
                                versionParts[0]++
                                versionParts[1] = 0
                                versionParts[2] = 0
                                echo "Breaking change - MAJOR version increment"
                            } else if (env.COMMIT_TYPE == 'feat') {
                                versionParts[1]++
                                versionParts[2] = 0
                                echo "Feature - MINOR version increment"
                            } else {
                                versionParts[2]++
                                echo "Fix/other - PATCH version increment"
                            }
                        } catch (Exception e) {
                            versionParts[2]++ // Fallback para PATCH se houver erro
                            echo "Erro no versionamento - usando fallback PATCH: ${e.message}"
                        }
                        
                        def newVersion = versionParts.join('.') + '-SNAPSHOT'
                        echo "Nova versão: ${newVersion}"
                        sh "mvn versions:set -DnewVersion=${newVersion} -DgenerateBackupPoms=false"
                        echo "Versão atualizada de ${pom.version} para ${newVersion}"
                        env.RELEASE_VERSION = newVersion.replace('-SNAPSHOT', '')
                    }
                }
            }
        }
        
        stage('Build') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn --version'
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn test'
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn verify -DskipUnitTests'
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
        
        stage('Generate Release Notes') {
            steps {
                script {
                    // Gera changelog baseado nos commits desde a última tag
                    def changelog = sh(script: """
                        git log --pretty=format:"- %s (%h)" HEAD
                    """, returnStdout: true).trim()
                    
                    writeFile file: "${env.PROJECT_DIR}/RELEASE_NOTES.md", text: """
                    # Release ${env.RELEASE_VERSION}
                    
                    ## Mudanças
                    ${changelog}
                    
                    ## Informações da Build
                    - Job: ${env.JOB_NAME}
                    - Build: ${env.BUILD_NUMBER}
                    """
                    
                    archiveArtifacts artifacts: "${env.PROJECT_DIR}/RELEASE_NOTES.md"
                }
            }
        }
        
        stage('Deploy to Tomcat (Optional)') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                script {
                    def warFile = "${env.PROJECT_DIR}/target/jenkins-demo.war"
                    
                    if (fileExists(warFile)) {
                        sh "mkdir -p /opt/tomcat/webapps/"
                        sh "cp ${warFile} /opt/tomcat/webapps/"
                        echo "Aplicação implantada no Tomcat: v${env.RELEASE_VERSION}"
                    } else {
                        error "Arquivo WAR não encontrado: ${warFile}"
                    }
                }
            }
        }
    }
    
    post {
        success {
            dir(env.PROJECT_DIR) {
                junit 'target/surefire-reports/**/*.xml'
                junit 'target/failsafe-reports/**/*.xml'
                sh 'mvn clean'
            }
            
            script {
                def duration = currentBuild.durationString.replace(' and counting', '')
                def releaseInfo = env.RELEASE_VERSION ? "Versão: ${env.RELEASE_VERSION}\n" : ""
                def msg = """${currentBuild.currentResult}: Job ${env.JOB_NAME} #${env.BUILD_NUMBER}
${releaseInfo}Duração: ${duration}
URL: ${env.BUILD_URL}"""
                
                echo msg
                // slackSend color: currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger', message: msg
            }
            echo "Build ${env.BUILD_NUMBER} (v${env.RELEASE_VERSION}) concluída com sucesso!"
        }
        
        failure {
            echo "Build ${env.BUILD_NUMBER} falhou!"
        }
        
        unstable {
            echo "Build ${env.BUILD_NUMBER} está instável!"
        }
    }
}