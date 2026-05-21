@echo off
REM Script para configurar ambiente Android e compilar o projeto
REM Executar como Admin

echo ====================================
echo Configurando Ambiente Android
echo ====================================

REM Definir JAVA_HOME
setx JAVA_HOME "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
setx ANDROID_HOME "%USERPROFILE%\AppData\Local\Android\Sdk"

REM Adicionar ao PATH
setx Path "%Path%;C:\Program Files\Amazon Corretto\jdk11.0.31_11\bin;%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools;%USERPROFILE%\AppData\Local\Android\Sdk\tools"

echo.
echo Ambiente configurado! Variaveis de ambiente:
echo JAVA_HOME = C:\Program Files\Amazon Corretto\jdk11.0.31_11
echo ANDROID_HOME = %USERPROFILE%\AppData\Local\Android\Sdk

echo.
echo ====================================
echo Compilando Projeto
echo ====================================

cd /d "C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet"

echo.
echo Executando: gradlew.bat assembleDebug
echo.

call gradlew.bat assembleDebug

echo.
echo ====================================
echo Build Completo!
echo ====================================

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo APK gerado em: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo Proximo passo: Instalar no emulador Android ou dispositivo fisico
) else (
    echo Erro na compilacao. Verifique logs acima.
)

pause
