# StormVR Motion Preview 2.1.2

O relato confirma imagem estereoscópica, movimento da cabeça e dois controles reconhecidos no SteamVR. A falha agora está nas ações das mãos. O driver recebia a pinça para trigger, mas o protocolo não transmitia a pegada para grip. Esta atualização adiciona um bit de pegada por mão ao pacote de 92 bytes e o mapeia à entrada de grip do driver, preservando a interpretação de pacotes antigos. A pinça permanece como trigger.

O driver e o APK devem ser atualizados juntos. O instalador faz cópia de segurança do driver anterior, e o APK Preview permanece separado da instalação 2.0.2.

Teste automatizado: compilar APK, testes unitários do Android, compilar driver com MSVC e verificar exportação HmdDriverFactory. Teste físico pendente: dedos, pinça, grip, seleção no SteamVR Dashboard e um jogo compatível. O rastreamento de dedos no driver usa a curvatura calculada para o perfil Index quando configurado; ainda não recebe 21 articulações reais pelo protocolo e não comprova compatibilidade integral com skeletal input.
