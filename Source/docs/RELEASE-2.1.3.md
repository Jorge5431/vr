# StormVR Motion Preview 2.1.3

Entrega incremental das ações das mãos: o APK envia pinça como gatilho e pegada como grip, separadas por mão, ao driver 2.1.2. Na Sala VR, o diagnóstico mostra “PINÇA” e “PEGADA” para esquerda e direita. Uma pinça sem interseção válida deixa de manter um botão pressionado. O protocolo UDP mantém 92 bytes, com novos bits opcionais; driver e APK antigos ignoram a pegada.

Validação feita: APK Android compilado, 13 testes unitários passaram; driver compilado com MSVC, exportação HmdDriverFactory verificada; teste nativo dos bits por mão, tamanho do pacote e compatibilidade com pacotes antigos passou. O instalador Windows contém o driver atualizado e mantém backup na instalação.

Validação física pendente: o ADB não encontrou o RMX3562 nesta sessão. Portanto, não há prova de pinça/grip no SteamVR Home, Dashboard ou jogo, nem medições de latência e precisão. O código atual não envia as 21 articulações reais ao SteamVR; o esqueleto do perfil Index ainda usa aproximações de curvatura. Não há base para declarar todas as fases do pedido concluídas. Para o próximo teste, conectar o celular por USB com depuração autorizada, instalar o APK Preview, atualizar o driver com SteamVR fechado, abrir a Sala VR com Modo PC, fazer pinça e pegada diante da câmera e observar as ações no Dashboard ou em um aplicativo compatível.
