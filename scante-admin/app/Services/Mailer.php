<?php
namespace App\Services;

/**
 * Envio de e-mail simples via mail() nativo do PHP (sem dependências —
 * projeto não usa Composer). Falhas só vão pro error_log.
 */
class Mailer {

    /** Avisa a equipe ScanTE que uma empresa abriu um chamado de suporte. */
    public static function notificarNovoChamado(string $paraEmail, array $chamado): void {
        if (!$paraEmail) return;

        $assunto = 'Novo chamado de suporte — ' . $chamado['empresaNome'];

        $corpo = "Uma empresa abriu um chamado no portal:\n\n"
            . "  Empresa: {$chamado['empresaNome']}\n"
            . "  Licença: {$chamado['licenca']}\n"
            . "  Assunto: {$chamado['assunto']}\n\n"
            . "Mensagem:\n{$chamado['mensagem']}\n\n"
            . "Responda em:\n{$chamado['linkAdmin']}\n\n"
            . "— ScanTE Admin";

        $headers = "From: ScanTE Admin <" . MAIL_FROM . ">\r\n"
            . "Content-Type: text/plain; charset=UTF-8\r\n";

        $ok = @mail($paraEmail, $assunto, $corpo, $headers);
        if (!$ok) {
            error_log("[Mailer] Falha ao notificar equipe sobre novo chamado (#{$chamado['chamadoId']})");
        }
    }
}
