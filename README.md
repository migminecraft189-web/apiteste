# API Chatbot Tester

Projeto Android nativo com:
- Chat OpenAI usando Chat Completions e modelo `gpt-4o-mini`.
- Testador HTTP genérico com GET, POST, PUT e DELETE.
- Exibição do código HTTP, duração e resposta JSON.
- Chave digitada apenas em memória, sem persistência local.

## Compilar no Bitrise
1. Envie os arquivos deste projeto para o repositório `migminecraft189-web/apiteste`.
2. No Bitrise, adicione o repositório como um projeto Android e escolha o workflow `primary`.
3. Execute a build e baixe `app-debug.apk` na seção de artefatos.

## Segurança
Não coloque chaves no código, commits, logs ou arquivos do APK. Mesmo digitada na tela, uma chave usada diretamente pelo app pode ser exposta por inspeção do aplicativo ou do tráfego. Para uso real, crie um backend que guarde a chave em variável de ambiente e faça a chamada de IA pelo servidor.
