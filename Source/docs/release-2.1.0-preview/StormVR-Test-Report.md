# Testes — StormVR Motion Preview 2.1.0

## Android
Compilação final: `assembleDebug testDebugUnitTest --offline`, Gradle 8.9 / JDK 17 / Android SDK 34, BUILD SUCCESSFUL em 8 s; 82 tarefas (12 executadas, 70 atualizadas). APK arm64-v8a, Android mínimo 26, versão 2.1.0 código 7, pacote com.stormvr.app.motion.preview.

Sete testes passaram, zero falhas: HandGripClassifierTest (3), ObjectAppearanceTest (3), HandPriorTest (1). A primeira tentativa com a alteração anatômica em andamento teve uma falha; a compilação final reexecutou os testes com a correção e passou.

`apksigner verify --print-certs` passou. Certificado de desenvolvimento SHA-256 c49d459f22097935bcd67d5c912defd769fce788bc0c455efa2a3b74a730d583. Assinatura diferente da 2.0.2 fornecida: instalação paralela, sem atualização dos dados anteriores.

## Jogos e áudio
Sintaxe JavaScript validada. Cinco jogos passaram em teste Node com DOM simulado: início, interação que pontua, fim após 45 s e saída. Isso não é teste de renderização Android/WebView nem validação VR.

Áudio original Pulso: arquivo Vorbis com duração de 34 segundos, mapa incluído. Reprodução e sincronização no alto-falante do celular não testadas.

## Windows
Driver, configurador e gamepad compilados com MSVC x64; painel Python empacotado. Driver exporta HmdDriverFactory, verificado com dumpbin. Instalador C# compilado incluindo o ZIP como recurso.

Instalação e reinstalação executadas por chamada do método Install em uma biblioteca fictícia isolada no workspace: DLL presente e backup anterior criado. Isso valida extração e backup, não carregamento no SteamVR, prompt de administrador ou interface gráfica do instalador. A instalação real do usuário foi preservada.

## Não executado
ADB não listou celular. Sem teste físico no Realme RMX3562, pareamento Apollo/Moonlight, reprodução do HTTP 403, streaming ao vivo, poses e bindings no SteamVR, FPS/latência/temperatura/bateria, precisão do objeto, orientações da câmera em ambas as posições e regressão integral da 2.0.2.

Os relatórios anteriores sobre ausência de Java/SDK/MSVC foram substituídos: as ferramentas foram localizadas e usadas nesta entrega. Todos os requisitos não estão concluídos; veja StormVR-Pending-Improvements.json.
