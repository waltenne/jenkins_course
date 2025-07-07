# 🛠️ Jenkins Pipeline Fundamentals - Módulo 3

<div align="center">
  <img src="https://jenkins.io/images/logo-title-opengraph.png" width="300" alt="Jenkins Logo">
</div>

## 📘 Módulo 3 – Construção de Pipelines Básicos

---

### 1. 🏁 Introdução a Pipelines

Os pipelines são a forma moderna de automatizar processos no Jenkins. Eles permitem criar **fluxos de trabalho definidos como código**, controlando todas as etapas de build, teste, deploy e notificações.

Existem dois estilos principais:

✔️ **Declarativo**  
- Estruturado e mais legível  
- Ideal para a maioria dos casos  
- Com sintaxe mais restrita e padronizada  

✔️ **Scripted**  
- Baseado em Groovy puro  
- Flexível e poderoso  
- Útil para cenários complexos que exigem lógica avançada

---

### ✨ Componentes Essenciais

Um pipeline declarativo contém 4 componentes principais:

- `agent`: Onde o pipeline executa
- `stages`: Fases do processo
- `steps`: Ações dentro de cada fase
- `post`: Ações pós-execução, como notificações e limpeza

**Exemplo mínimo de pipeline:**

<pre>
pipeline {
    agent any
    stages {
        stage('Exemplo') {
            steps {
                echo 'Passo básico'
            }
        }
    }
}
</pre>

---

# ✨ Componentes Detalhados de uma Pipeline

---

## 🖥️ `agent`

O bloco `agent` define **onde o pipeline (ou cada estágio) será executado**.  
Ele determina o ambiente em que os comandos vão rodar.

Você pode escolher:

- **Qualquer agente disponível** (`any`)
- **Um nó específico** (ex.: máquina Linux com label)
- **Um container Docker** (para builds isolados)
- **Nenhum agente global**, obrigando cada `stage` a definir o seu próprio agente

**Exemplos:**

<pre>
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Executando em qualquer agente disponível.'
            }
        }
    }
}
</pre>

<pre>
pipeline {
    agent {
        label 'linux'
    }
    stages {
        stage('Test') {
            steps {
                echo 'Executando no nó Linux.'
            }
        }
    }
}
</pre>

<pre>
pipeline {
    agent {
        docker {
            image 'maven:3.8.1-jdk-11'
            args '-v /root/.m2:/root/.m2'
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
    }
}
</pre>

<pre>
pipeline {
    agent none
    stages {
        stage('Build') {
            agent {
                label 'linux'
            }
            steps {
                echo 'Executando build no nó Linux.'
            }
        }
        stage('Test') {
            agent {
                docker {
                    image 'openjdk:11'
                }
            }
            steps {
                sh 'java -version'
            }
        }
    }
}
</pre>

✅ **Dica:** Use `agent none` se quiser controle total sobre cada estágio.

---

## 🎯 `stages`

O bloco `stages` agrupa todas as **fases principais do pipeline**, que organizam o fluxo de trabalho.  
Cada `stage` representa uma etapa lógica, como:

- Build
- Test
- Deploy

Dentro de cada `stage`, você define exatamente o que vai acontecer.

**Exemplo:**

<pre>
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Compilando aplicação...'
            }
        }
        stage('Test') {
            steps {
                echo 'Executando testes...'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Fazendo deploy...'
            }
        }
    }
}
</pre>

✅ **Dica:** Use o bloco `when` dentro de um `stage` para definir condições de execução.

---

## 🛠️ `steps`

O bloco `steps` contém as **ações executadas dentro de cada estágio**.  
São os comandos que realizam as tarefas práticas, como:

- Executar scripts
- Compilar o código
- Fazer uploads
- Enviar notificações

**Exemplos:**

<pre>
stage('Build') {
    steps {
        sh 'mvn clean package'
    }
}
</pre>

<pre>
stage('Deploy') {
    steps {
        script {
            def version = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
            echo "Versão do deploy: ${version}"
        }
    }
}
</pre>

<pre>
stage('Notify') {
    steps {
        mail to: 'dev-team@empresa.com',
             subject: "Pipeline finalizado",
             body: "O processo foi concluído com sucesso."
    }
}
</pre>

✅ **Dica:** Para lógica condicional e variáveis, utilize `script`.

---

## 🔄 `post`

O bloco `post` define **ações que acontecem após a execução do pipeline** ou de um estágio.  
Ele garante que atividades importantes sejam realizadas, independentemente do resultado.

Você pode usar os seguintes blocos:

- `always`: Executa sempre
- `success`: Executa se tudo terminou bem
- `failure`: Executa se algo falhou
- `unstable`: Executa se o build ficou instável
- `changed`: Executa se o status mudou em relação ao build anterior

**Exemplo:**

<pre>
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh 'exit 1' // Simula falha
            }
        }
    }
    post {
        always {
            echo 'Sempre executado.'
        }
        success {
            echo 'Executado apenas em sucesso.'
        }
        failure {
            echo 'Executado em falha.'
        }
        unstable {
            echo 'Executado se o build ficou instável.'
        }
        changed {
            echo 'Executado se o resultado mudou.'
        }
    }
}
</pre>

✅ **Dica:** O `post` é essencial para fazer tratativas após execução do pipeline como por exemplo

1) Limpar recursos
   1) Limpar a workspace com o `cleanWS()`
2) Parar recursos
   1) Por exemplo um build que sobe um compose no docker
3) Enviar notificações
   1) Por exemplo um email com o resultado do pipeline
   2) Alertas para Discord/Slack


---

# ✅ Resumo Rápido

- **agent**: Define o ambiente (máquina/container) de execução.
- **stages**: Agrupa as fases principais do pipeline.
- **steps**: Contém os comandos que fazem o trabalho.
- **post**: Define ações pós-execução.

---

## 📚 Mais Informações

- [Documentação Oficial do Jenkins](https://www.jenkins.io/doc/)
- [Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)

---

### 2. 🚦 Primeiros Passos

**Pipeline "Hello World"**

<pre>
pipeline {
    agent any
    stages {
        stage('Saída') {
            steps {
                echo 'Olá Mundo Jenkins!'
            }
        }
    }
}
</pre>

---

**Gatilhos comuns:**
- Polling SCM (verificação periódica de mudanças)
- Acionamento manual
- Webhooks de repositórios (GitHub, GitLab, Bitbucket)

---

**Comandos Shell básicos:**

<pre>
steps {
    sh 'whoami'    // Mostra usuário atual
    sh 'uname -a'  // Informações do sistema
    sh '''
    echo "Comando"
    echo "multilinha"
    '''
}
</pre>

---

### 3. 🔗 Integração com Git

**Clonando repositórios com o `git`:**

<pre>
stage('Checkout') {
    steps {
        git branch: 'main',
            credentialsId: 'git-creds',
            url: 'https://github.com/waltenne/jenkins_course.git'
    }
}
</pre>

<pre>
stage('Checkout') {
    steps {
        sh 'git clone -b main https://github.com/waltenne/jenkins_course.git'
    }
}
</pre>

---

**Build simples (exemplo Java/Maven):**

<pre>
stage('Build') {
    steps {
        sh 'mvn clean install'
        junit 'target/surefire-reports/*.xml' // Relatório de testes
    }
}
</pre>

---

**Notificações por e-mail em caso de falha:**

<pre>
post {
    failure {
        emailext (
            subject: "FALHA no Build ${env.BUILD_NUMBER}",
            body: "Detalhes: ${env.BUILD_URL}",
            to: 'equipe@empresa.com'
        )
    }
}
</pre>

---

## 📚 Recursos Úteis

• [Documentação Oficial Jenkins](https://www.jenkins.io/doc/)  
• [Sintaxe Pipeline Avançada](https://www.jenkins.io/doc/book/pipeline/syntax/)

---
