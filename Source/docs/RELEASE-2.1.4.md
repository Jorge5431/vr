# StormVR Motion Preview 2.1.4

O Android agora calcula uma curvatura de 0 a 15 para polegar, indicador, médio, anelar e mindinho, individualmente em cada mão, usando a geometria dos pontos MediaPipe. As cinco medidas ocupam os três bytes reservados de cada mão no pacote UDP existente de 92 bytes. O driver reconhece um marcador e atualiza os valores de dedos e o esqueleto aproximado do perfil Valve Index. Pacotes anteriores continuam válidos; se o marcador não existir, o driver usa as entradas anteriores. Pinça continua sendo trigger e pegada continua sendo grip.

Compilação Android e testes unitários passaram. O teste nativo confirmou tamanho do pacote, marcador, curvaturas independentes e ausência de ativação de grip por pacote antigo. O driver compilou com MSVC. O instalador contém o driver atualizado e preserva um backup da instalação anterior.

Isto melhora a origem do movimento dos dedos no SteamVR, mas não constitui envio de todas as 21 posições articulares nem garante uma mão sem deformação: a câmera do celular, oclusões, iluminação e calibração permanecem limites reais. O celular não ficou disponível via USB nem depuração sem fio nesta sessão. Testes de pinça, grip, Dashboard, jogo, latência, jitter, temperatura e regressão física continuam necessários. Não é possível declarar a atualização integralmente validada sem esses testes.
