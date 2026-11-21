# Conectar o StormVR ao PC

O vídeo do Moonlight/Apollo e o rastreamento do SteamVR são duas conexões separadas. O vídeo pode funcionar enquanto o headset não aparece no SteamVR, e um erro HTTP do Apollo não prova uma falha no driver.

| Caminho | O que transporta | Padrão usado pelo StormVR |
| --- | --- | --- |
| Apollo + Moonlight | Lista de apps, pareamento e vídeo do PC | API Moonlight no host; painel web Apollo em `https://IP-DO-PC:47990/` |
| StormVR + SteamVR | Pose da cabeça e mãos do celular ao driver | UDP 5556 (cabeça) e UDP 5557 (mãos) |

## 1. Parear Apollo e abrir o vídeo

1. Conecte o Realme e o PC à mesma rede local. No StormVR, use o IPv4 atual do PC, não o endereço do celular.
2. Deixe o Apollo iniciado no Windows. No app, abra **Moonlight**, adicione o IPv4 e toque em **Parear (PIN)**.
3. Quando o StormVR mostrar o PIN, abra `https://IP-DO-PC:47990/` no navegador do PC, entre no painel Apollo, escolha **PIN** no menu, digite o código e confirme. O painel pode avisar que o certificado é autoassinado.
4. Aguarde a confirmação de pareamento no StormVR, escolha **Desktop** no Moonlight e confira se a imagem do PC chega ao celular.

O caminho `/pin` não é necessário. Use a página inicial do painel, faça login e escolha a aba PIN. O guia do Apollo/Moonlight descreve esse fluxo de login, aba PIN, confirmação e seleção do app.

Se o StormVR mostrar **HTTP 403**, o PC respondeu à conexão, mas recusou aquela solicitação. Confirme o IPv4, que o serviço ativo é o Apollo e que o painel abre na página inicial. Se o erro ocorrer ao buscar os apps depois de parear, remova esse PC da lista do StormVR e pareie novamente pelo painel. O 403 do Moonlight/Apollo é independente das portas UDP do driver SteamVR.

## 2. Carregar o headset no SteamVR

1. Feche o SteamVR antes de atualizar o driver. Instale a pasta `stormvr` em `SteamVR/drivers/stormvr` usando `install.bat`; se a Steam Library não for detectada, passe o caminho da pasta SteamVR como argumento.
2. Confirme estes arquivos no destino:
   - `drivers/stormvr/driver.vrdrivermanifest`
   - `drivers/stormvr/bin/win64/driver_stormvr.dll`
   - `drivers/stormvr/bin/win64/openvr_api.dll`
3. Abra o SteamVR e procure **stormvr** em `SteamVR/logs/vrserver.txt`. Se o driver não aparecer, verifique se há uma instalação duplicada do driver antigo `keyboardtouch` e se o SteamVR apontou para a mesma biblioteca onde os arquivos foram copiados.
4. No StormVR, abra a Sala VR, informe o mesmo IPv4 do PC e ative **Modo PC (SteamVR)**. Autorize UDP 5556 e 5557 no Firewall do Windows quando solicitado.
5. No painel do driver, veja o diagnóstico da cabeça e das mãos. Se o rastreamento não chegar, confira o IP, as portas e o log antes de alterar o painel de vídeo.

O Moonlight usa as portas do Apollo para API e mídia; o driver escuta nas portas UDP do quadro acima. Não encaminhe portas no roteador para um teste na mesma rede local.

## 3. Usar a pegada da mão como gatilho do controle

Na Sala VR, abra a aba **Caneta / Objeto**, ative **Usar pegada de objeto como controle (experimental)** e mantenha a mão que segura o item visível para a câmera. O StormVR estima a curvatura dos dedos a partir dos 21 landmarks; após confirmação por mais de um quadro, usa a pegada como gatilho do controlador virtual da mão. A posição e a orientação continuam vindo do rastreamento da mão e seguem pelo protocolo existente ao SteamVR.

Este modo não classifica o objeto por nome/aparência e não mede a pose do objeto separado da mão. Oclusões fortes podem interromper os landmarks. Desative a opção para voltar ao comportamento anterior de pinça. A validação precisa ser feita com o item e a câmera do RMX3562.

## Limites desta correção

O texto fornecido registra um erro 403, mas não inclui a captura nem identifica se ele apareceu no navegador, durante o pareamento ou ao listar os apps. A atualização do app melhora a instrução e a mensagem para cada tentativa, mas a origem exata precisa ser confirmada pelo ponto em que o 403 aparece. Não houve acesso físico ao RMX3562 nem ao SteamVR instalado para confirmar o pareamento ou o rastreamento.

## Referências

- [Guia de configuração do Apollo](https://github.com/ClassicOldSong/Apollo/blob/master/docs/getting_started.md)
- [Documentação de drivers OpenVR da Valve](https://github.com/ValveSoftware/openvr/wiki/Driver-Documentation)
