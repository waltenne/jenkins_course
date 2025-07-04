/**
 * Classe utilitária para operações de pipeline Jenkins
 */
class PipelineUtils implements Serializable {
    def steps
    
    PipelineUtils(steps) {
        this.steps = steps
    }
    
    /**
     * Realiza checkout de repositório Git
     * @param repoUrl URL do repositório (obrigatório)
     * @param branchName Nome do branch (default: 'main')
     * @param relativeDir Diretório relativo para checkout (default: '.')
     */
    def libCheckout(String repoUrl, String branchName = 'main', String relativeDir = '.') {
        if (!repoUrl?.trim()) {
            steps.error "O parâmetro 'repoUrl' é obrigatório e não pode ser vazio!"
        }
        
        steps.checkout([
            $class: 'GitSCM',
            branches: [[name: branchName ?: 'main']],
            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: relativeDir]],
            userRemoteConfigs: [[url: repoUrl]]
        ])
    }
    
    /**
     * Valida mensagem de commit conforme Conventional Commits
     * @param commitPattern Padrão regex para validação
     */
    def validateCommit(String commitPattern = '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-zA-Z0-9_-]+\\))?(!)?: .+') {
        def commitMsg = steps.sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
        
        def matcher = (commitMsg =~ /${commitPattern}/)
        
        if (!matcher.matches()) {
            steps.error """
            [ERRO] Mensagem de commit inválida!
            Padrão esperado: tipo(escopo opcional)!: descrição
            Exemplo válido: 'feat!: mudança que quebra compatibilidade'
            Mensagem recebida: '${commitMsg}'
            """
        }
        
        steps.env.COMMIT_TYPE = matcher[0][1] ?: 'fix'
        steps.env.IS_BREAKING_CHANGE = (matcher[0][3] == '!') ? 'true' : 'false'
        steps.env.VERSION_TYPE = determineVersionType()
        
        steps.echo "Tipo de commit: ${steps.env.COMMIT_TYPE}"
        steps.echo "Breaking change: ${steps.env.IS_BREAKING_CHANGE}"
        steps.echo "Tipo de versão (semver): ${steps.env.VERSION_TYPE}"
    }
    
    private String determineVersionType() {
        if (steps.env.IS_BREAKING_CHANGE == 'true') {
            return 'MAJOR'
        } else if (steps.env.COMMIT_TYPE == 'feat') {
            return 'MINOR'
        }
        return 'PATCH'
    }
    
    /**
     * Incrementa versão no pom.xml conforme semver
     * @param projectDir Diretório do projeto (default: 'files/projects/java-17-example')
     * @return Nova versão
     */
    def incrementVersion(String projectDir = 'files/projects/java-17-example') {
        def newVersion = ''
        
        steps.dir(projectDir) {
            def pom = steps.readMavenPom(file: 'pom.xml')
            def currentVersion = pom.version
            steps.echo "Versão atual: ${currentVersion}"
            
            def (major, minor, patch) = parseVersion(currentVersion)
            
            try {
                switch(steps.env.VERSION_TYPE) {
                    case 'MAJOR':
                        major++
                        minor = 0
                        patch = 0
                        steps.echo "Incrementando MAJOR version: ${major}.0.0"
                        break
                    case 'MINOR':
                        minor++
                        patch = 0
                        steps.echo "Incrementando MINOR version: ${major}.${minor}.0"
                        break
                    default:
                        patch++
                        steps.echo "Incrementando PATCH version: ${major}.${minor}.${patch}"
                }
            } catch(Exception e) {
                patch++
                steps.echo "Erro ao calcular versão - fallback PATCH: ${e.message}"
            }
            
            newVersion = "${major}.${minor}.${patch}"
            steps.echo "Nova versão calculada: ${newVersion}"
            
            steps.sh "mvn versions:set -q -DnewVersion=${newVersion} -DgenerateBackupPoms=false"
            steps.env.RELEASE_VERSION = newVersion
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
    
    /**
     * Executa build Maven
     * @param config Mapa de configuração com:
     *   - projectDir: Diretório do projeto
     *   - goals: Lista de goals Maven
     *   - mavenProfiles: Lista de profiles
     *   - debug: Flag para modo debug
     *   - quiet: Flag para modo silencioso
     *   - skipTests: Flag para pular testes
     */
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
        
        steps.dir(config.projectDir) {
            def args = config.goals.collect()
            if (config.quiet) args << '-q'
            if (config.debug) args << '-X'
            if (config.mavenProfiles) args << "-P${config.mavenProfiles.join(',')}"
            if (config.skipTests) args << '-DskipTests'
            
            steps.echo "Executando Maven build: mvn ${args.join(' ')}"
            steps.sh "mvn ${args.join(' ')}"
        }
    }
    
    /**
     * Executa testes unitários Maven
     * @param config Mapa de configuração com:
     *   - projectDir: Diretório do projeto
     *   - mavenProfiles: Lista de profiles
     *   - debug: Flag para modo debug
     *   - quiet: Flag para modo silencioso
     */
    def unitTests(Map config) {
        def defaults = [
            projectDir: 'files/projects/java-17-example',
            mavenProfiles: [],
            debug: false,
            quiet: true
        ]
        
        config = defaults + config
        
        steps.dir(config.projectDir) {
            def args = ['test']
            if (config.quiet) args << '-q'
            if (config.debug) args << '-X'
            if (config.mavenProfiles) args << "-P${config.mavenProfiles.join(',')}"
            
            steps.echo "Executando Maven unit tests: mvn ${args.join(' ')}"
            steps.sh "mvn ${args.join(' ')}"
        }
    }
    
    /**
     * Executa testes de integração Maven
     * @param config Mapa de configuração com:
     *   - projectDir: Diretório do projeto
     *   - mavenProfiles: Lista de profiles
     *   - debug: Flag para modo debug
     *   - quiet: Flag para modo silencioso
     */
    def integrationTests(Map config) {
        def defaults = [
            projectDir: 'files/projects/java-17-example',
            mavenProfiles: [],
            debug: false,
            quiet: true
        ]
        
        config = defaults + config
        
        steps.dir(config.projectDir) {
            def args = ['verify', '-DskipUnitTests']
            if (config.quiet) args << '-q'
            if (config.debug) args << '-X'
            if (config.mavenProfiles) args << "-P${config.mavenProfiles.join(',')}"
            
            steps.echo "Executando Maven integration tests: mvn ${args.join(' ')}"
            steps.sh "mvn ${args.join(' ')}"
        }
    }
    
    /**
     * Gera release notes baseado no histórico do Git
     */
    def generateReleaseNotes() {
        steps.withEnv(['LANG=en_US.UTF-8', 'LC_ALL=en_US.UTF-8']) {
            def changelog = steps.sh(
                script: 'git log --pretty=format:"- %s (%h)" HEAD',
                returnStdout: true
            ).trim()

            def plainTextDesc = """RELEASE ${steps.env.RELEASE_VERSION}
============================
CHANGES:
${changelog}

BUILD INFORMATION:
- Job: ${steps.env.JOB_NAME}
- Build: ${steps.env.BUILD_NUMBER}
- Date: ${new Date().format("yyyy-MM-dd HH:mm:ss z", TimeZone.getTimeZone('America/Sao_Paulo'))}
"""

            def htmlDesc = """
<div style="font-family: Arial, sans-serif; line-height: 1.5;">
<h3 style="margin-bottom: 5px;">RELEASE ${steps.env.RELEASE_VERSION}</h3>
<hr style="margin: 5px 0 10px 0;">
<strong>CHANGES:</strong><br>
<pre style="margin: 5px 0; font-family: monospace;">${changelog}</pre>
<hr style="margin: 5px 0 10px 0;">
</div>
"""

            try {
                steps.currentBuild.description = htmlDesc
            } catch (Exception e) {
                steps.echo "HTML não suportado, usando texto simples"
                steps.currentBuild.description = plainTextDesc
            }
        }
    }
    
    /**
     * Realiza deploy para Tomcat
     * @param config Mapa de configuração com:
     *   - projectDir: Diretório do projeto
     *   - releaseVersion: Versão para deploy
     *   - dryRun: Simular deploy
     *   - quiet: Modo silencioso
     */
    def deployToTomcat(Map config) {
        def defaults = [
            projectDir: 'files/projects/java-17-example',
            releaseVersion: null,
            dryRun: false,
            quiet: true
        ]
        
        config = defaults + config
        
        steps.dir(config.projectDir) {
            def version = config.releaseVersion ?: steps.env.RELEASE_VERSION
            def artifactName = "jenkins-demo-${version}.war"
            def warFile = "target/${artifactName}"
            
            if (!steps.fileExists(warFile)) {
                steps.error "Arquivo WAR não encontrado: ${warFile}"
            }
            
            if (config.dryRun) {
                steps.echo "[DRY RUN] Deploy para Tomcat simulando cópia de ${warFile} para /opt/tomcat/webapps/"
            } else {
                steps.sh "mkdir -p /opt/tomcat/webapps/"
                def cpCmd = "cp ${warFile} /opt/tomcat/webapps/"
                if (config.quiet) cpCmd += " > /dev/null 2>&1"
                steps.sh cpCmd
                steps.echo "Aplicação ${artifactName} implantada no Tomcat"
            }
        }
    }
    
    /**
     * Arquiva artefatos
     * @param config Mapa de configuração com:
     *   - projectDir: Diretório do projeto
     *   - pattern: Padrão de arquivos
     *   - onlyIfSuccessful: Arquivar apenas se sucesso
     */
    def archiveArtifact(Map config) {
        def defaults = [
            projectDir: 'files/projects/java-17-example',
            pattern: 'target/*.war',
            onlyIfSuccessful: true
        ]
        
        config = defaults + config
        
        steps.dir(config.projectDir) {
            steps.archiveArtifacts artifacts: config.pattern, onlyIfSuccessful: config.onlyIfSuccessful
        }
    }
    
    /**
     * Processa relatórios de teste
     * @param config Mapa de configuração com:
     *   - projectDir: Diretório do projeto
     *   - cleanAfter: Limpar após processar
     *   - quiet: Modo silencioso
     */
    def processTestReports(Map config) {
        def defaults = [
            projectDir: 'files/projects/java-17-example',
            cleanAfter: true,
            quiet: true
        ]
        
        config = defaults + config
        
        steps.dir(config.projectDir) {
            def surefireReports = steps.findFiles(glob: 'target/surefire-reports/**/*.xml')
            def failsafeReports = steps.findFiles(glob: 'target/failsafe-reports/**/*.xml')

            if (surefireReports) {
                steps.junit 'target/surefire-reports/**/*.xml'
            } else if (!config.quiet) {
                steps.echo 'Nenhum relatório de testes unitários encontrado'
            }

            if (failsafeReports) {
                steps.junit 'target/failsafe-reports/**/*.xml'
            } else if (!config.quiet) {
                steps.echo 'Nenhum relatório de testes de integração encontrado'
            }

            if (config.cleanAfter) {
                steps.sh 'mvn clean' + (config.quiet ? " -q" : "")
            }
        }
    }
    
    /**
     * Gera resumo do build
     */
    def logSummary() {
        def duration = steps.currentBuild.durationString.replace(' and counting', '')
        def msg = "${steps.currentBuild.currentResult}: Job ${steps.env.JOB_NAME} #${steps.env.BUILD_NUMBER}\n" +
                "Duração: ${duration}\n" +
                "URL: ${steps.env.BUILD_URL}"
        steps.echo msg
    }
}

// Instância global para compatibilidade
def pipelineUtils = new PipelineUtils(this)