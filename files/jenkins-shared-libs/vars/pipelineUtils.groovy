def checkout(String repoUrl, String branchName = 'main') {
    if (!repoUrl?.trim()) {
        error "O parâmetro 'repoUrl' é obrigatório e não pode ser vazio!"
    }
    if (!branchName?.trim()) {
        branchName = 'main'
    }

    checkout([
        $class: 'GitSCM',
        branches: [[name: branchName]],
        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '.']],
        userRemoteConfigs: [[url: repoUrl]]
    ])
}

def validateCommit(commitPattern = '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-zA-Z0-9_-]+\\))?(!)?: .+') {
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

    // Definir VERSION_TYPE
    if (env.IS_BREAKING_CHANGE == 'true') {
        env.VERSION_TYPE = 'MAJOR'
    } else if (env.COMMIT_TYPE == 'feat') {
        env.VERSION_TYPE = 'MINOR'
    } else {
        env.VERSION_TYPE = 'PATCH'
    }

    echo "Tipo de commit: ${env.COMMIT_TYPE}"
    echo "Breaking change: ${env.IS_BREAKING_CHANGE}"
    echo "Tipo de versão (semver): ${env.VERSION_TYPE}"
}

def incrementVersion(projectDir = 'files/projects/java-17-example') {
    dir(projectDir) {
        def pom = readMavenPom file: 'pom.xml'
        def currentVersion = pom.version
        echo "Versão atual: ${currentVersion}"

        def baseVersion = currentVersion.replaceAll(/-.*$/, '')
        def versionParts = baseVersion.tokenize('.').collect { it.toInteger() }

        while (versionParts.size() < 3) {
            versionParts << 0
        }

        try {
            switch(env.VERSION_TYPE) {
                case 'MAJOR':
                    versionParts[0]++
                    versionParts[1] = 0
                    versionParts[2] = 0
                    echo "Incrementando MAJOR version: ${versionParts[0]}.0.0"
                    break
                case 'MINOR':
                    versionParts[1]++
                    versionParts[2] = 0
                    echo "Incrementando MINOR version: ${versionParts[0]}.${versionParts[1]}.0"
                    break
                default:
                    versionParts[2]++
                    echo "Incrementando PATCH version: ${versionParts[0]}.${versionParts[1]}.${versionParts[2]}"
                    break
            }
        } catch(Exception e) {
            versionParts[2]++
            echo "Erro ao calcular versão - fallback PATCH: ${e.message}"
        }

        def newVersion = versionParts.join('.')
        echo "Nova versão calculada: ${newVersion}"

        sh "mvn versions:set -q -DnewVersion=${newVersion} -DgenerateBackupPoms=false"

        env.RELEASE_VERSION = newVersion
    }
}

/**
 * Executa o build Maven.
 * @param projectDir - Diretório do projeto
 * @param goals - Lista de goals Maven, ex: ['clean','install'] ou ['clean','deploy']
 * @param mavenProfiles - Lista de profiles Maven
 * @param debug - true para modo debug (-X)
 * @param quiet - true para modo silencioso (-q)
 * @param skipTests - true para pular testes (-DskipTests)
 */
def build(
    String projectDir = 'files/projects/java-17-example',
    List goals = ['clean','package'],
    List mavenProfiles = [],
    boolean debug = false,
    boolean quiet = true,
    boolean skipTests = true
) {
    dir(projectDir) {
        def args = goals.collect()
        if (quiet) args << '-q'
        if (debug) args << '-X'
        if (mavenProfiles) args << "-P${mavenProfiles.join(',')}"
        if (skipTests) args << '-DskipTests'

        echo "Executando Maven build: mvn ${args.join(' ')}"
        sh "mvn ${args.join(' ')}"
    }
}

/**
 * Executa testes unitários Maven.
 * @param projectDir - Diretório do projeto
 * @param mavenProfiles - Lista de profiles Maven
 * @param debug - true para modo debug (-X)
 * @param quiet - true para modo silencioso (-q)
 */
def unitTests(
    String projectDir = 'files/projects/java-17-example',
    List mavenProfiles = [],
    boolean debug = false,
    boolean quiet = true
) {
    dir(projectDir) {
        def args = ['test']
        if (quiet) args << '-q'
        if (debug) args << '-X'
        if (mavenProfiles) args << "-P${mavenProfiles.join(',')}"

        echo "Executando Maven unit tests: mvn ${args.join(' ')}"
        sh "mvn ${args.join(' ')}"
    }
}


/**
 * Executa testes de integração Maven.
 * @param projectDir - Diretório do projeto
 * @param mavenProfiles - Lista de profiles Maven
 * @param debug - true para modo debug (-X)
 * @param quiet - true para modo silencioso (-q)
 */
def integrationTests(
    String projectDir = 'files/projects/java-17-example',
    List mavenProfiles = [],
    boolean debug = false,
    boolean quiet = true
) {
    dir(projectDir) {
        def args = ['verify', '-DskipUnitTests']
        if (quiet) args << '-q'
        if (debug) args << '-X'
        if (mavenProfiles) args << "-P${mavenProfiles.join(',')}"

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

def deployToTomcat(projectDir = 'files/projects/java-17-example', releaseVersion = null, dryRun = false, quiet = true) {
    dir(projectDir) {
        def version = releaseVersion ?: env.RELEASE_VERSION
        def artifactName = "jenkins-demo-${version}.war"
        def warFile = "target/${artifactName}"
        if (!fileExists(warFile)) {
            error "Arquivo WAR não encontrado: ${warFile}"
        }
        if (dryRun) {
            echo "[DRY RUN] Deploy para Tomcat simulando cópia de ${warFile} para /opt/tomcat/webapps/"
        } else {
            sh "mkdir -p /opt/tomcat/webapps/"
            def cpCmd = "cp ${warFile} /opt/tomcat/webapps/"
            if (quiet) cpCmd += " > /dev/null 2>&1"
            sh cpCmd
            echo "Aplicação ${artifactName} implantada no Tomcat"
        }
    }
}

def archiveArtifact(projectDir = 'files/projects/java-17-example', pattern = 'target/*.war', onlyIfSuccessful = true) {
    dir(projectDir) {
        archiveArtifacts artifacts: pattern, onlyIfSuccessful: onlyIfSuccessful
    }
}

def processTestReports(projectDir = 'files/projects/java-17-example', cleanAfter = true, quiet = true) {
    dir(projectDir) {
        def surefireReports = findFiles(glob: 'target/surefire-reports/**/*.xml')
        def failsafeReports = findFiles(glob: 'target/failsafe-reports/**/*.xml')

        if (surefireReports) {
            junit 'target/surefire-reports/**/*.xml'
        } else if (!quiet) {
            echo 'Nenhum relatório de testes unitários encontrado'
        }

        if (failsafeReports) {
            junit 'target/failsafe-reports/**/*.xml'
        } else if (!quiet) {
            echo 'Nenhum relatório de testes de integração encontrado'
        }

        if (cleanAfter) {
            sh 'mvn clean' + (quiet ? " -q" : "")
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
