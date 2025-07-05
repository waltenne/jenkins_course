/**
 * Métodos utilitários para pipelines Jenkins
 */

def libCheckout(Map config) {
    def defaults = [
        repoUrl: null,
        branchName: 'main',
        relativeDir: '.'
    ]
    config = defaults + config
    
    if (!config.repoUrl?.trim()) {
        error "O parâmetro 'repoUrl' é obrigatório e não pode ser vazio!"
    }
    
    checkout([
        $class: 'GitSCM',
        branches: [[name: '*/your-branch']],
        extensions: [
            [$class: 'CloneOption', depth: 1, shallow: true, noTags: true],
            [$class: 'CleanBeforeCheckout'],
            [$class: 'LocalBranch']
        ],
        userRemoteConfigs: [[url: 'your-repo-url']],
        doGenerateSubmoduleConfigurations: false,
        submoduleCfg: []
    ])
}

def validateCommit(String commitPattern = '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-zA-Z0-9_-]+\\))?(!)?: .+') {
    def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
    
    def matcher = (commitMsg =~ /${commitPattern}/)
    
    if (!matcher.matches()) {
        error """
        [ERRO] Mensagem de commit inválida!
        Padrão esperado: tipo(escopo opcional)!: descrição
        Exemplo válido: 'feat!: mudança que quebra compatibilidade'
        Mensagem recebida: '${commitMsg}'
        """
    }
    
    env.COMMIT_TYPE = matcher[0][1] ?: 'fix'
    env.IS_BREAKING_CHANGE = (matcher[0][3] == '!') ? 'true' : 'false'
    env.VERSION_TYPE = determineVersionType()
    
    echo "Tipo de commit: ${env.COMMIT_TYPE}"
    echo "Breaking change: ${env.IS_BREAKING_CHANGE}"
    echo "Tipo de versão (semver): ${env.VERSION_TYPE}"
}

private String determineVersionType() {
    if (env.IS_BREAKING_CHANGE == 'true') {
        return 'MAJOR'
    } else if (env.COMMIT_TYPE == 'feat') {
        return 'MINOR'
    }
    return 'PATCH'
}

def incrementVersion(String projectDir = 'files/projects/java-17-example') {
    def newVersion = ''
    
    dir(projectDir) {
        def pom = readMavenPom file: 'pom.xml'
        def currentVersion = pom.version
        echo "Versão atual: ${currentVersion}"
        
        def (major, minor, patch) = parseVersion(currentVersion)
        
        try {
            switch(env.VERSION_TYPE) {
                case 'MAJOR':
                    major++
                    minor = 0
                    patch = 0
                    echo "Incrementando MAJOR version: ${major}.0.0"
                    break
                case 'MINOR':
                    minor++
                    patch = 0
                    echo "Incrementando MINOR version: ${major}.${minor}.0"
                    break
                default:
                    patch++
                    echo "Incrementando PATCH version: ${major}.${minor}.${patch}"
            }
        } catch(Exception e) {
            patch++
            echo "Erro ao calcular versão - fallback PATCH: ${e.message}"
        }
        
        newVersion = "${major}.${minor}.${patch}"
        echo "Nova versão calculada: ${newVersion}"
        
        sh "mvn versions:set -q -DnewVersion=${newVersion} -DgenerateBackupPoms=false"
        env.RELEASE_VERSION = newVersion
    }
    
    return newVersion
}

private List<Integer> parseVersion(String version) {
    def baseVersion = version.replaceAll(/-.*$/, '')
    def parts = baseVersion.tokenize('.').collect { it.toInteger() }
    
    while (parts.size() < 3) {
        parts << 0
    }
    
    return parts
}

def build(Map config) {
    def defaults = [
        projectDir: 'files/projects/java-17-example',
        goals: ['clean', 'package'],
        mavenProfiles: [],
        debug: false,
        quiet: true,
        skipTests: true
    ]
    
    config = defaults + config
    
    dir(config.projectDir) {
        def args = config.goals.collect()
        if (config.quiet) args << '-q'
        if (config.debug) args << '-X'
        if (config.mavenProfiles) args << "-P${config.mavenProfiles.join(',')}"
        if (config.skipTests) args << '-DskipTests'
        
        echo "Executando Maven build: mvn ${args.join(' ')}"
        sh "mvn ${args.join(' ')}"
    }
}

def unitTests(Map config) {
    def defaults = [
        projectDir: 'files/projects/java-17-example',
        mavenProfiles: [],
        debug: false,
        quiet: true
    ]
    
    config = defaults + config
    
    dir(config.projectDir) {
        def args = ['test']
        if (config.quiet) args << '-q'
        if (config.debug) args << '-X'
        if (config.mavenProfiles) args << "-P${config.mavenProfiles.join(',')}"
        
        echo "Executando Maven unit tests: mvn ${args.join(' ')}"
        sh "mvn ${args.join(' ')}"
    }
}

def integrationTests(Map config) {
    def defaults = [
        projectDir: 'files/projects/java-17-example',
        mavenProfiles: [],
        debug: false,
        quiet: true
    ]
    
    config = defaults + config
    
    dir(config.projectDir) {
        def args = ['verify', '-DskipUnitTests']
        if (config.quiet) args << '-q'
        if (config.debug) args << '-X'
        if (config.mavenProfiles) args << "-P${config.mavenProfiles.join(',')}"
        
        echo "Executando Maven integration tests: mvn ${args.join(' ')}"
        sh "mvn ${args.join(' ')}"
    }
}

def generateReleaseNotes() {
    withEnv(['LANG=en_US.UTF-8', 'LC_ALL=en_US.UTF-8']) {
        def changelog = sh(
            script: 'git log --pretty=format:"- %s (%h)" HEAD',
            returnStdout: true
        ).trim()

        def plainTextDesc = """Release ${env.RELEASE_VERSION}
============================
CHANGES:
${changelog}
"""

        def htmlDesc = """
<div style="font-family: Arial, sans-serif; line-height: 1.5;">
<h3 style="margin-bottom: 5px;">Release ${env.RELEASE_VERSION}</h3>
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

def deployFakeToTomcat(Map config) {
    def defaults = [
        projectDir: 'files/projects/java-17-example',
        releaseVersion: null,
        dryRun: false,
        quiet: true
    ]
    
    config = defaults + config
    
    dir(config.projectDir) {
        def version = config.releaseVersion ?: env.RELEASE_VERSION
        def artifactName = "jenkins-demo-${version}.war"
        def warFile = "target/${artifactName}"
        
        if (!fileExists(warFile)) {
            error "Arquivo WAR não encontrado: ${warFile}"
        }
        
        if (config.dryRun) {
            echo "[DRY RUN] Deploy para Tomcat simulando cópia de ${warFile} para /opt/tomcat/webapps/"
        } else {
            sh "mkdir -p /opt/tomcat/webapps/"
            def cpCmd = "cp ${warFile} /opt/tomcat/webapps/"
            if (config.quiet) cpCmd += " > /dev/null 2>&1"
            sh cpCmd
            echo "Aplicação ${artifactName} implantada no Tomcat"
        }
    }
}

/**
 * Realiza deploy de um artefato WAR em um Tomcat remoto usando o Manager API
 * 
 * @param config Mapa de configuração com os seguintes parâmetros:
 *   - warFile: Caminho completo para o arquivo WAR (obrigatório)
 *   - artifactName: Nome do artefato (opcional, padrão: nome do arquivo WAR)
 *   - version: Versão do artefato (opcional)
 *   - tomcatUrl: URL do Tomcat Manager (ex: http://localhost:8081)
 *   - tomcatUser: Usuário com permissão manager-script
 *   - tomcatPass: Senha do usuário
 *   - contextPath: Caminho da aplicação (ex: 'myapp' para http://tomcat:8080/myapp)
 *   - dryRun: Simula o deploy sem executar (default: false)
 *   - quiet: Suprime output detalhado (default: true)
 *   - forceDeploy: Força redeploy mesmo sem alterações (default: true)
 */
def deployToTomcat(Map config) {
    // Validação dos parâmetros obrigatórios
    if (!config.warFile) {
        error "Parâmetro 'warFile' é obrigatório"
    }
    if (!config.tomcatUrl) {
        error "Parâmetro 'tomcatUrl' é obrigatório"
    }
    
    // Configurações padrão
    def defaults = [
        artifactName: config.warFile.split('/').last().replace('.war', ''),
        version: null,
        dryRun: false,
        quiet: true,
        forceDeploy: true,
        contextPath: config.warFile.split('/').last().replace('.war', '')
    ]
    
    config = defaults + config
    
    // Verifica se o arquivo WAR existe
    if (!fileExists(config.warFile)) {
        error "Arquivo WAR não encontrado: ${config.warFile}"
    }
    
    // Monta o comando de deploy
    def deployCmd = """
        curl -v -u ${config.tomcatUser}:${config.tomcatPass} \
        -T "${config.warFile}" \
        "${config.tomcatUrl}/manager/text/deploy?path=/${config.contextPath}&update=true"
    """
    
    if (config.quiet) {
        deployCmd += " > /dev/null 2>&1"
    }
    
    // Execução (simulada ou real)
    if (config.dryRun) {
        echo """
        [DRY RUN] Simulando deploy para Tomcat:
        - Arquivo: ${config.warFile}
        - Artifact: ${config.artifactName}
        - Versão: ${config.version ?: 'não especificada'}
        - Tomcat: ${config.tomcatUrl}
        - Contexto: /${config.contextPath}
        - Comando: ${deployCmd.split('\n').collect { it.trim() }.join(' ')}
        """
    } else {
        try {
            echo "Iniciando deploy de ${config.artifactName} v${config.version ?: '?'} para ${config.tomcatUrl}/${config.contextPath}"
            sh deployCmd
            echo "Deploy concluído com sucesso!"
        } catch (Exception e) {
            error "Falha no deploy para Tomcat: ${e.message}"
        }
    }
}

def archiveArtifact(Map config) {
    def defaults = [
        projectDir: 'files/projects/java-17-example',
        pattern: 'target/*.war',
        onlyIfSuccessful: true
    ]
    
    config = defaults + config
    
    dir(config.projectDir) {
        archiveArtifacts artifacts: config.pattern, onlyIfSuccessful: config.onlyIfSuccessful
    }
}

def processTestReports(Map config) {
    def defaults = [
        projectDir: 'files/projects/java-17-example',
        cleanAfter: true,
        quiet: true
    ]
    
    config = defaults + config
    
    dir(config.projectDir) {
        def surefireReports = findFiles(glob: 'target/surefire-reports/**/*.xml')
        def failsafeReports = findFiles(glob: 'target/failsafe-reports/**/*.xml')

        if (surefireReports) {
            junit 'target/surefire-reports/**/*.xml'
        } else if (!config.quiet) {
            echo 'Nenhum relatório de testes unitários encontrado'
        }

        if (failsafeReports) {
            junit 'target/failsafe-reports/**/*.xml'
        } else if (!config.quiet) {
            echo 'Nenhum relatório de testes de integração encontrado'
        }

        if (config.cleanAfter) {
            sh 'mvn clean' + (config.quiet ? " -q" : "")
        }
    }
}

def logSummary() {
    def duration = currentBuild.durationString.replace(' and counting', '')
    def msg = "${currentBuild.currentResult}: Job ${env.JOB_NAME} #${env.BUILD_NUMBER}\n" +
            "Duração: ${duration}\n" +
            "URL: ${env.BUILD_URL}"
    echo msg
}

def call(Map config) {
    // Implementação padrão quando chamado como pipelineUtils(config)
    return build(config)
}


// Métodos específicos do Jenkinsfile (não vão para a shared lib)
def generateReleaseDashboard() {
    def changelog = sh(
        script: 'git log -1 --pretty=format:"<li>%s (%h) - %an</li>"',
        returnStdout: true
    ).trim()
    
    return """
    <div style='font-family: Arial; padding: 10px; border: 1px solid #ddd;'>
        <h3>Release ${env.RELEASE_VERSION}</h3>
        <h4>Changes:</h4>
        <ul>${changelog}</ul>
        <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
    </div>
    """
}

def archiveReleaseInfo() {
    writeFile file: 'release_info.html', text: currentBuild.description
    archiveArtifacts artifacts: 'release_info.html'
}