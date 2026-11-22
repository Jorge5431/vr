# StormVR Motion Preview 2.1.0 — 21/11/2026

Esta entrega é uma versão de teste compilada a partir da fonte recuperada do StormVR (base 0.4.0, commit upstream a94f5afea67ac23ed1d9beedd84bbf691056c5ec), com melhorias e reconstrução de recursos identificados no APK 2.0.2 fornecido. Não é a recuperação integral do código-fonte privado da 2.0.2. A equivalência de todas as funções daquela versão ainda não foi demonstrada.

## Implementado
- Câmera traseira Camera2 para mãos e imagem de fundo em aparelhos sem ARCore, modelo de mãos incluído para uso offline.
- Expiração independente por mão, reconstrução anatômica após filtragem, proteção para ossos colapsados e articulações invertidas.
- Perfis Precisão, Equilibrado, Movimento rápido e Econômico: ajustam filtros e limite de inferência (30/30/60/15); Econômico também reduz a imagem de inferência. São limites solicitados, não FPS medidos.
- Visuais mão completa, esqueleto e 21 pontos/conexões. A malha leve existente foi preservada; não é uma nova mão fotorrealista.
- Cadastro visual em Caneta / Objeto: fotografar a mão segurando o objeto, selecionar uma marca e usar essa marca para mirar na sala e enviar a pose estimada ao PC. Guarda 25 amostras de cor, sem guardar a fotografia.
- StormSaber: música original Pulso incluída, importação de mapas em subpasta, mensagens para falha de áudio, orientação horizontal invertida respeitada.
- Biblioteca na tela espacial com Storm Target, Storm Blocks, Storm Boxing, Storm Lab e Storm Rhythm, placar, rodadas e recorde local. São minijogos 2D na Sala VR, não cinco mundos VR completos.
- Driver SteamVR, configurador Windows, gamepad e painel empacotados. Instalador seleciona biblioteca SteamVR e preserva instalação anterior em StormVR-backups.

## Limites
O controle por objeto é experimental: requer mão e marca visíveis, não identifica automaticamente qualquer categoria de objeto e não mede uma pose 6DoF independente. A profundidade vem da mão; a direção é uma mira olho–marca. Perdas e oclusões precisam de teste real.

O erro HTTP 403 do Apollo não foi reproduzido nem declarado resolvido. O acesso ao painel foi corrigido e o fluxo de novo pareamento documentado. O app novo precisa ser pareado novamente.

Não foram realizados benchmarks no RMX3562, validação de precisão, conforto, temperatura, consumo ou teste completo de vídeo e controles no SteamVR. Não há equivalência comprovada com Meta Quest 3. Permanecem pendentes diagnósticos completos bruto/filtrado/reconstruído por ponto, novos gatilhos por indicador, recuperação visual suave após oclusão, Home totalmente redesenhada e evolução dos minijogos para ambientes imersivos.

## Instalação preservada
O APK usa com.stormvr.app.motion.preview, versão 2.1.0, assinatura de desenvolvimento e nome StormVR Motion Preview. Instala ao lado da 2.0.2; não substitui seus dados, configurações e pareamentos. A chave original da 2.0.2 não foi fornecida. O instalador Windows não foi executado contra o SteamVR real: os testes usaram uma pasta isolada.
