# Relógio Fire TV

Aplicativo Android experimental para mostrar a hora (24 horas) em um canto da tela, sobre outros aplicativos. Não modifica o firmware e não requer root.

## Download

[Baixar APK de teste 0.1.0](https://github.com/tolosaandre-source/firetv/raw/refs/heads/main/downloads/firetv-clock-0.1.0-debug.apk)

Compilação e assinatura verificadas. Ainda não testado em um Fire TV físico. O checksum está em [downloads/SHA256SUMS](downloads/SHA256SUMS). Para os comandos de instalação abaixo, renomeie o arquivo baixado para `firetv-clock-debug.apk`.

## Compilar

Requer JDK completo, Android SDK API 35 e build-tools 35.0.0. No ambiente preparado:

```sh
bash scripts/build.sh
```

Em outra máquina, defina JAVA_HOME e ANDROID_HOME. O APK de desenvolvimento fica em `build/firetv-clock-debug.apk`. A chave de desenvolvimento é criada em `.local/` e deve ser preservada para instalar atualizações sem desinstalar o app. Não é uma chave de publicação. O build usa diretamente as ferramentas oficiais do SDK e não precisa de Gradle ou bibliotecas externas.

## Instalar no Fire TV

Faça estes passos em um computador na mesma rede do Fire TV. O ambiente na nuvem não tem acesso automático à sua rede doméstica.

1. No Fire TV, habilite as opções do desenvolvedor (em Configurações → Meu Fire TV → Sobre, pressione o nome do dispositivo sete vezes, se necessário).
2. Habilite a depuração ADB e consulte o IP do aparelho nas configurações de rede.
3. Com Android platform-tools no computador, execute os comandos abaixo, substituindo IP_DO_FIRE_TV pelo endereço real. Aceite a autorização na TV.

```sh
adb connect IP_DO_FIRE_TV:5555
adb install -r firetv-clock-debug.apk
adb shell am start -n br.com.tolosa.fireclock/.MainActivity
```

No aplicativo, selecione “Permitir exibição sobre outros apps”. Se o Fire OS não oferecer essa tela, tente pelo computador autorizado:

```sh
adb shell appops set br.com.tolosa.fireclock SYSTEM_ALERT_WINDOW allow
```

Se o dispositivo rejeitar a autorização, registre a mensagem; não há garantia de suporte nesse firmware. Volte ao app e selecione “Ativar relógio”. Pressione Home e abra um vídeo. Use “Mudar canto da tela”, “Mudar tamanho” e “Desativar relógio” para ajustar.

## Verificação no aparelho

- Confirme a hora na tela inicial e a atualização ao virar o minuto.
- Abra cada serviço de streaming que utiliza e confirme a visibilidade durante reprodução, pausa e menus.
- Verifique se o controle remoto continua funcionando e se o relógio não cobre legendas; ajuste o canto se necessário.
- Confirme os dois tamanhos, os quatro cantos e a remoção ao desativar.
- Reinicie o aparelho, abra o aplicativo e ative novamente.

O horário e o fuso seguem o Fire TV. O app não pede acesso à internet. Não há início automático após reinicialização nesta versão. Apps e telas protegidas podem ocultar sobreposições; o sistema pode encerrar o serviço. O comportamento no Fire OS 8.1.8.5 ainda precisa ser validado fisicamente. Não exponha a porta ADB na internet e desative a depuração quando terminar.

Para remover:

```sh
adb uninstall br.com.tolosa.fireclock
```
