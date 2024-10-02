# VLocator - Aplicativo de Localização em Tempo Real

## Descrição
O **VLocator** é um aplicativo Android que rastreia a localização do dispositivo em tempo real e exibe informações como latitude, longitude, cidade, bairro, e o código postal (CEP) aproximado. O aplicativo utiliza um serviço de localização em primeiro plano (`Foreground Service`) para capturar continuamente a localização do usuário e exibe essas informações na interface.

## Funcionalidades
- Rastreamento em tempo real da localização (latitude e longitude).
- Exibição da cidade e bairro baseado nas coordenadas.
- Integração com a API Nominatim (OpenStreetMap) para obter o CEP aproximado com base nas coordenadas.
- Interface simples com opção para iniciar e parar a captura de localização.
- Suporte a permissões de localização em tempo de execução.
