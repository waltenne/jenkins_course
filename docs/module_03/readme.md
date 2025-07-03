# 🛠️ Jenkins Pipeline Fundamentals - Módulo 3

<div align="center">
  <img src="https://jenkins.io/images/logo-title-opengraph.png" width="300" alt="Jenkins Logo">
</div>

## 📘 Módulo 3 – Construção de Pipelines Básicos

### 1. 🏁 Introdução a Pipelines

**Pipeline Declarativo vs Scripted**  
✔️ **Declarativo**: Estruturado, pré-definido, ideal para maioria dos casos  
✔️ **Scripted**: Flexível, baseado em Groovy, para lógica complexa  

**Componentes essenciais**:
- `agent`: Onde o pipeline executa
- `stages`: Fases do processo
- `steps`: Ações dentro de cada fase
- `post`: Ações pós-execução


```
// Exemplo de estrutura mínima
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
```

### 2. 🚦 Primeiros Passos

**Pipeline "Hello World"**  
```
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
```

**Gatilhos comuns**:
- Polling SCM (verificação periódica)
- Acionamento manual
- Webhooks externos (GitHub/GitLab/Bitbucket)


**Comandos Shell básicos**:
```
steps {
    sh 'whoami'    // Mostra usuário
    sh 'uname -a'  // Info do sistema
    sh '''
    echo "Comando"
    echo "multilinha"
    '''
}
```

### 3. 🔗 Integração com Git

**Clonando repositórios**:
```
stage('Checkout') {
    steps {
        git branch: 'main',
        credentialsId: 'git-creds',
        url: 'https://github.com/seu/repo.git'
    }
}
```

```
stage('Checkout') {
    steps {
        sh ''' git clone -b main https://github.com/seu/repo.git '''
    }
}
```



**Build simples** (exemplo Java/Maven):
```
stage('Build') {
    steps {
        sh 'mvn clean install'
        junit 'target/surefire-reports/*.xml'  // Relatório de testes
    }
}
```

**Notificações por e-mail**:
```
post {
    failure {
        emailext (
            subject: "FALHA no Build ${env.BUILD_NUMBER}",
            body: "Detalhes: ${env.BUILD_URL}",
            to: 'equipe@empresa.com'
        )
    }
}
```

## 📚 Recursos Úteis

• [Documentação Oficial Jenkins](https://www.jenkins.io/doc/)  
• [Sintaxe Pipeline Avançada](https://www.jenkins.io/doc/book/pipeline/syntax/)  
