# 🏗️ Fundamentos do Jenkins - Módulo 1

<div align="center">
  <img src="https://cdn.icon-icons.com/icons2/2107/PNG/512/file_type_jenkins_icon_130515.png" width="120" alt="Logo Jenkins">
  <h3>A Principal Ferramenta de Automação CI/CD</h3>
</div>

## 1. 🔄 Conceitos Básicos de CI/CD

### 🧩 Integração Contínua (CI)

```
CI = Prática de desenvolvimento onde:
✓ Alterações são integradas diariamente
✓ Builds automatizados são disparados
✓ Testes são executados automaticamente
✓ Feedback imediato para a equipe
✓ Benefícios principais:
  - 80% menos bugs em produção
  - Evita "inferno de merge"
  - Código sempre pronto para deploy
```

**Fluxo Padrão**:
```
✓ Desenvolvedor: git commit → git push
✓ Servidor CI: Detecta mudanças
✓ Executa: Build + Testes
✓ Produz: Artefatos + Relatórios
✓ Notifica: Equipe de desenvolvimento
```

### 🚀 Entrega vs Implantação Contínua

```
ENTREGA CONTÍNUA:
• Sempre pronto para produção
• Liberação manual para produção
• Portões de aprovação

IMPLANTAÇÃO CONTÍNUA:
• Liberação automática
• Sem intervenção humana
• Requer cobertura total de testes
```

**Estágios do Pipeline**:
```
+---------------+    +----------------+    +---------------+
|   Build &     | →  |  Testes &      | →  |  Deploy em    |
| Empacotamento |    |  Validação     |    |  Produção     |
+---------------+    +----------------+    +---------------+
```

## 2. 🏭 Arquitetura do Jenkins

### 🧠 Componentes Principais

```
NÓ MASTER:
• Interface web
• Agendador de jobs
• Fila de builds
• Gerenciador de plugins
• Armazenamento de artefatos

NÓS AGENTES:
• Executores (builds paralelos)
• Workspaces isolados
• Ambientes customizados
• Recursos escaláveis
```

**Fluxo de Comunicação**:
```
+---------------+     +-----------------+
| Desenvolvedor | →   | Repositório Git |
+---------------+     +-----------------+
                        ↓
                +-----------------+
                | Jenkins Master  |
                +-----------------+
                  ↓           ↓
          +--------------+  +----------------+
          | Agente Linux |  | Agente Windows |
          +--------------+  +----------------+
```

### 🔌 Ecossistema de Plugins

**Plugins**:
```
1. Pipeline: Definição de workflows modernos
2. Blue Ocean: Interface visual aprimorada  
3. Git: Integração com repositórios
4. Docker Pipeline: Suporte a containers
5. Credentials: Gerenciamento seguro de senhas
6. JUnit: Relatórios de testes
7. Mailer: Sistema de notificações
```

## 3. 🛠️ Primeiros Passos

### 🖥️ Primeiro Job no Jenkins

```
pipeline {
    agent any
    stages {
        stage('Olá Mundo') {
            steps {
                echo 'Bem-vindo ao Jenkins!'
                sh 'echo "Informações do sistema:" && uname -a'
            }
        }
    }
}
```

**Funcionalidades-Chave**:
```
✓ Alocação de agentes
✓ Definição de estágios  
✓ Execução de passos
✓ Integração com shell
✓ Feedback imediato
```

### ⚙️ Configuração do Sistema

**Configurações Recomendadas**:
```
# Segurança:
- Ativar segurança baseada em matriz
- Configurar permissões de usuário
- Habilitar HTTPS

# Performance:
- Opções JVM: -Xmx4g -XX:MaxRAMPercentage=70.0
- Executores: 2x núcleos de CPU
- Período de quiet: 5s

# Backup:
- Plugin ThinBackup
- Backup regular no SCM
- Plano de recuperação de desastres
```

## 4. 📊 Jenkins vs Alternativas

**Comparação Técnica**:
```
| Critério             | Jenkins     | GitLab CI    | GitHub Actions    |
|----------------------|-------------|--------------|-------------------|
| Instalação           | Self-hosted | SaaS/On-prem | Somente cloud     |
| Custo                | Gratuito    | Freemium     | Minutos gratuitos |
| Plugins              | 1800+       | Limitados    | Em crescimento    |
| Escalabilidade       | Excelente   | Bom          | Excelente         |
| Curva de Aprendizado | Moderada    | Fácil        | Fácil             |
```