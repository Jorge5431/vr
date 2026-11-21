# Pacote de código StormVR atualizado

Esta cópia parte do commit a94f5afea67ac23ed1d9beedd84bbf691056c5ec do repositório original StormVR. O app Android recuperado era 0.4.0. A versão 0.4.2 / versionCode 3 é apenas uma candidata de código: inclui o modo experimental de pegada como controle, mas não foi compilada nem testada nesta etapa.

As credenciais Wi-Fi do firmware ESP32 foram removidas do template e deixadas em branco. Configure `WIFI_SSID` e `WIFI_PASS` localmente antes de gravar o firmware.

O ZIP contém código e recursos, sem DLLs, EXEs, APKs ou bibliotecas nativas pré-compiladas. O APK 0.4.1 criado anteriormente não contém o modo novo e não deve ser tratado como atualização completa. O driver Windows precisa ser compilado com Visual Studio/CMake antes de instalar; o binário existente na referência foi excluído do pacote.

Abra Source/android no Android Studio para compilar o app e executar os testes unitários adicionados. As instruções do driver estão em Source/pc_driver/LEIA-ME.txt; o guia de conexão e do modo de pegada está em Source/docs/CONEXAO_STEAMVR_APOLLO.md. O modo de pegada detecta a curvatura dos dedos; não reconhece a classe do objeto nem a pose do objeto separado da mão.

Esta referência não contém o código que identificaria a instalação Android como 2.0.2. O repositório https://github.com/Jorge5431/vr.git agora tem a branch candidata https://github.com/Jorge5431/vr/tree/feature/held-object-grip-controller; ela não contém o código 2.0.2. Confirme versão e assinatura antes de instalar qualquer APK sobre dados existentes.
