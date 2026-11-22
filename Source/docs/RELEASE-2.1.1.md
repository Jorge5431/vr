# StormVR Motion Preview 2.1.1

O Apollo deste PC tinha dois cadastros RMX3562 com permissão para listar e assistir, sem permissão para iniciar aplicativos. Corrigimos ambos com cópia de segurança da configuração e reinício do serviço. Isso explica a recusa ao iniciar Desktop, mas a transmissão precisa ser confirmada no celular.

O app agora inicia em H.264, 720p, 30 fps e 8 Mbps para diagnóstico, e permite desativar o modo compatível na tela Moonlight. A sessão aguarda quadros realmente desenhados antes de ocultar a mensagem de conexão; pede um novo quadro se o vídeo parar e mostra aviso após oito segundos sem imagem. Erros ao iniciar o app apontam a permissão Apollo. O botão de refazer pareamento conserva o PC cadastrado.

As mãos passaram a ter filtragem do punho separada da articulação, calibração de comprimento limitada e ação de recalibrar. A mão oculta deixa de manter uma pinça por quase meio segundo. A opção de mirar com a cabeça e selecionar com pinça segue o princípio de interação indireta das diretrizes públicas da Meta.

O cadastro de objetos procura primeiro perto da marca anterior, acompanha a mão, admite mudança moderada de tamanho e iluminação e mostra estado/tempo da leitura na tela. Continua dependente de uma marca visível e da estimativa de profundidade da mão; não identifica automaticamente qualquer objeto.

Pesquisa: [Moonlight Android](https://github.com/moonlight-stream/moonlight-android), [Apollo](https://github.com/ClassicOldSong/Apollo), [MediaPipe](https://github.com/google-ai-edge/mediapipe), [OpenCV](https://github.com/opencv/opencv), [ALVR](https://github.com/alvr-org/ALVR), [A-Frame](https://github.com/aframevr/aframe) e [OpenVR](https://github.com/ValveSoftware/openvr). Foram adaptados conceitos de tolerância de interação, recuperação de vídeo e comparação de padrões. Nenhum código desses projetos foi copiado nesta atualização. ALVR exigiria substituir o protocolo de vídeo e o cliente inteiro; A-Frame é para cenas WebVR; não resolveriam diretamente o defeito observado.

Limite de teste: o celular não apareceu no ADB. Não há medição de FPS, latência, conforto ou teste de conexão ponta a ponta no RMX3562. A versão Preview instala ao lado da 2.0.2, com dados separados.
