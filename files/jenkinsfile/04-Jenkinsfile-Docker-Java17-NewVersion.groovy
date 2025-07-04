pipeline {
    agent {
        label 'Docker'
    }
    
    environment {
        PROJECT_DIR = 'files/projects/java-17-example'
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
                        def currentVersion = pom.version
                        echo "Versão atual: ${currentVersion}"
                        
                        // Remove qualquer sufixo existente (como -SNAPSHOT)
                        def baseVersion = currentVersion.replaceAll(/-.*$/, '')
                        def versionParts = baseVersion.tokenize('.').collect { it.toInteger() }
                        
                        // Garante que temos pelo menos 3 partes (major.minor.patch)
                        while (versionParts.size() < 3) {
                            versionParts << 0
                        }
                        
                        // Lógica de incremento semântico
                        try {
                            if (env.IS_BREAKING_CHANGE == 'true') {
                                versionParts[0]++
                                versionParts[1] = 0
                                versionParts[2] = 0
                                echo "Breaking change - MAJOR version increment (${versionParts[0]}.0.0)"
                            } else if (env.COMMIT_TYPE == 'feat') {
                                versionParts[1]++
                                versionParts[2] = 0
                                echo "Feature - MINOR version increment (${versionParts[0]}.${versionParts[1]}.0)"
                            } else {
                                versionParts[2]++
                                echo "Fix/other - PATCH version increment (${versionParts[0]}.${versionParts[1]}.${versionParts[2]})"
                            }
                        } catch (Exception e) {
                            versionParts[2]++
                            echo "Erro no versionamento - usando fallback PATCH: ${e.message}"
                        }
                        
                        // Constrói a nova versão sem SNAPSHOT
                        def newVersion = versionParts.join('.')
                        echo "Nova versão: ${newVersion}"
                        
                        // Atualiza o pom.xml
                        sh "mvn versions:set -q -DnewVersion=${newVersion} -DgenerateBackupPoms=false"
                        echo "Versão atualizada de ${currentVersion} para ${newVersion}"
                        
                        // Armazena a versão para uso posterior
                        env.RELEASE_VERSION = newVersion
                    }
                }
            }
        }
        
        stage('Build') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn clean package -q -DskipTests'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn test -q '
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn verify -q -DskipUnitTests'
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
                    withEnv(['LANG=en_US.UTF-8', 'LC_ALL=en_US.UTF-8']) {
                        // Obtém o changelog
                        def changelog = sh(
                            script: 'git log --pretty=format:"- %s (%h)" HEAD',
                            returnStdout: true
                        ).trim()
                        
                        // Cria conteúdo em texto puro (alternativa segura)
                        def plainTextDesc = """RELEASE ${env.RELEASE_VERSION}
============================
CHANGES:
${changelog}

BUILD INFORMATION:
- Job: ${env.JOB_NAME}
- Build: ${env.BUILD_NUMBER}
- Date: ${new Date().format("yyyy-MM-dd HH:mm:ss z", TimeZone.getTimeZone('America/Sao_Paulo'))}
"""

                        def htmlDesc = """
<div style="font-family: Arial, sans-serif; line-height: 1.5;">
<h3 style="margin-bottom: 5px;">RELEASE ${env.RELEASE_VERSION}</h3>
<hr style="margin: 5px 0 10px 0;">
<strong>CHANGES:</strong><br>
<pre style="margin: 5px 0; font-family: monospace;">${changelog}</pre>
<hr style="margin: 5px 0 10px 0;">
</div>
"""

                        try {
                            currentBuild.description = htmlDesc
                        } catch (Exception e) {
                            echo "HTML não suportado, usando texto simples"
                            currentBuild.description = plainTextDesc
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Tomcat (Optional)') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                script {
                    dir(env.PROJECT_DIR) {
                        // Usa o ARTIFACT_NAME definido no environment
                        def artifact_name = "jenkins-demo-${env.RELEASE_VERSION}.war" 
                        def warFile = "target/${artifact_name}"
                        if (fileExists(warFile)) {
                            sh "mkdir -p /opt/tomcat/webapps/"
                            sh "cp ${warFile} /opt/tomcat/webapps/"
                            echo "Aplicação ${artifact_name} implantada no Tomcat"
                        } else {
                            error "Arquivo WAR não encontrado: ${warFile}"
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            dir(env.PROJECT_DIR) {
                // Verifica se existem relatórios de teste antes de processar
                script {
                    def surefireReports = findFiles(glob: 'target/surefire-reports/**/*.xml')
                    def failsafeReports = findFiles(glob: 'target/failsafe-reports/**/*.xml')
                    
                    if (surefireReports) {
                        junit 'target/surefire-reports/**/*.xml'
                    } else {
                        echo 'Nenhum relatório de testes unitários encontrado'
                    }
                    
                    if (failsafeReports) {
                        junit 'target/failsafe-reports/**/*.xml'
                    } else {
                        echo 'Nenhum relatório de testes de integração encontrado'
                    }
                }
                sh 'mvn clean'
            }
            
            script {
                def duration = currentBuild.durationString.replace(' and counting', '')
                def msg = "${currentBuild.currentResult}: Job ${env.JOB_NAME} #${env.BUILD_NUMBER}\n" +
                        "Duração: ${duration}\n" +
                        "URL: ${env.BUILD_URL}"
                echo msg
            }
        }
        
        success {
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