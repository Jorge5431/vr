# Instalação da prévia 2.1.0

1. No Android 8 ou superior, arm64, instale StormVR-Android.apk e abra **StormVR Motion Preview**. Mantenha a 2.0.2 instalada. Autorize a câmera quando solicitado. Configurações e pareamentos anteriores não são copiados.
2. Em Sala VR → Caneta / Objeto, toque **Fotografar e cadastrar objeto**. Mantenha mão e objeto visíveis. Na foto, toque numa ponta, borda ou marca com contraste perto dos dedos. Ative **Mirar com o objeto cadastrado**. Use a pinça para selecionar. Troque o cadastro para usar outro objeto. Em pouca luz ou sem marca distinguível, o rastreamento pode falhar.
3. Em Mãos e 6DoF escolha o perfil e visual. Comece em Equilibrado. Em aparelhos sem ARCore a câmera direta permite mãos, mas não substitui posicionamento ambiental 6DoF.
4. Abra StormSaber e selecione **Pulso**, a música incluída. Os outros cinco jogos estão em **Jogos da sala**, na tela da Sala VR.
5. No Windows x64, feche SteamVR e abra StormVR-Windows-Setup.exe. Selecione a pasta **SteamVR**, confirme e aguarde. O pacote inclui driver, configurador, gamepad e painel. A instalação anterior fica em SteamVR/StormVR-backups. O executável não tem assinatura comercial. O driver ZIP contém o mesmo pacote para inspeção/instalação manual.
6. O instalador não instala nem substitui Apollo, SteamVR ou ViGEmBus. O gamepad virtual exige ViGEmBus já instalado. Abra as ferramentas em SteamVR/drivers/stormvr: bin/win64/stormvr_config.exe, Gamepad/stormvr_gamepad.exe e Painel/StormVR-Painel.exe conforme o uso.
7. Conecte PC e celular à mesma rede. Com Apollo iniciado, no Moonlight do app novo adicione o IPv4 do PC e faça **Parear (PIN)**. No navegador do PC, abra https://IP-DO-PC:47990/, entre no painel e use a aba PIN. Confirme a transmissão Desktop antes de testar o SteamVR.
8. Se houver HTTP 403, confira IP e permissões do cliente no Apollo. Remova o PC da lista do app e repita o pareamento se a lista de apps continuar recusada. Esta é uma orientação de diagnóstico; a causa do erro relatado ainda não foi reproduzida.
9. Na Sala VR, informe o mesmo IP e ative Modo PC (SteamVR). O driver usa UDP 5556 para cabeça e 5557 para mãos; a porta do painel de diagnóstico é separada. Se o Windows solicitar acesso à rede, permita na rede privada usada pelos aparelhos.
10. Abra SteamVR. Confira headset e controles, depois se o monitor transmitido por Apollo corresponde à saída de vídeo configurada no driver. Ainda é necessário testar esse caminho completo no aparelho.

Para reverter no Android, continue usando a 2.0.2. Para reverter o driver, feche SteamVR, guarde a pasta stormvr atual e recoloque a pasta de backup em drivers/stormvr. Não execute duas cópias de receptores na mesma porta.
