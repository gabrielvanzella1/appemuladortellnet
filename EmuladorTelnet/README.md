# Emulador Telnet - Projeto Android

Um emulador Telnet especializado para coletores de dados em empresas de logística, com sistema de licença integrado.

## 📋 Estrutura do Projeto

```
EmuladorTelnet/
├── app/                          # Módulo principal da aplicação
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/            # Código Kotlin/Java
│   │   │   ├── res/             # Recursos (layouts, strings, etc)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                # Testes unitários
│   │   └── androidTest/         # Testes de instrumentação
│   └── build.gradle.kts
├── gradle/                       # Configurações do Gradle
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 🚀 Começando

### Pré-requisitos

- Android Studio 2023.1+
- JDK 11+
- Android SDK 34
- Gradle 8.2+

### Instalação

1. Clone o repositório:
```bash
git clone <seu-repositorio>
```

2. Abra no Android Studio ou VS Code:
```bash
cd EmuladorTelnet
```

3. Sincronize o Gradle:
```bash
./gradlew sync  # ou gradlew.bat sync no Windows
```

## 📱 Recursos Principais (Planejados)

- [x] Estrutura base do projeto
- [x] Layout da interface principal
- [ ] Conexão Telnet
- [ ] Emulador de terminal
- [ ] Sistema de licença
- [ ] Armazenamento de configurações
- [ ] Suporte a múltiplas conexões

## 🏗️ Arquitetura

### Camadas

- **Presentation**: Atividades, Fragments e ViewModels
- **Domain**: Casos de uso e entidades
- **Data**: Repositórios e fontes de dados

### Dependências Principais

- AndroidX
- Kotlin Coroutines
- LiveData/StateFlow

## 📝 Referências

- [ConnectBot](https://connectbot.org/) - Cliente SSH/Telnet de referência
- [GoldenLink](https://goldenlink.com.br/) - Especificações do projeto

## 📄 Licença

Proprietário - Empresa de Logística

---

**Desenvolvido com ❤️ para logística**
