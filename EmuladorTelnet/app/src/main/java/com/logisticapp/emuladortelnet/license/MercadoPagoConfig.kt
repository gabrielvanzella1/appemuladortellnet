package com.logisticapp.emuladortelnet.license

object MercadoPagoConfig {
    // Cole aqui seu Access Token do Mercado Pago (Credenciais > Access Token)
    const val ACCESS_TOKEN = "SEU_ACCESS_TOKEN_AQUI"

    // Preço da licença vitalícia em BRL
    const val LICENSE_PRICE = 49.90

    const val CURRENCY_ID = "BRL"
    const val ITEM_TITLE = "Licença Vitalícia - ScanTE"

    // Deep links de retorno ao app após pagamento
    const val SUCCESS_URL = "emuladortelnet://payment/success"
    const val FAILURE_URL = "emuladortelnet://payment/failure"
    const val PENDING_URL = "emuladortelnet://payment/pending"
}
