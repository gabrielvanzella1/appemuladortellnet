<?php
namespace App\Models;

use App\Core\Model;

class AppVersao extends Model {
    protected string $table = 'app_versoes';

    public function criar(array $d): int {
        $this->db->execute(
            "INSERT INTO app_versoes
                (version_code, version_name, changelog, empresa_exclusiva_id, arquivo_apk, criado_por, criada_em)
             VALUES (?,?,?,?,?,?,NOW())",
            [
                $d['version_code'], $d['version_name'], $d['changelog'],
                $d['empresa_exclusiva_id'] ?: null, $d['arquivo_apk'], $d['criado_por'],
            ]
        );
        return (int)$this->db->lastInsertId();
    }

    /** Lista com o nome da empresa exclusiva (se houver), mais recente primeiro. */
    public function listarComEmpresa(): array {
        return $this->db->query(
            "SELECT v.*, e.nome AS empresa_nome
             FROM app_versoes v
             LEFT JOIN empresas e ON e.id = v.empresa_exclusiva_id
             ORDER BY v.version_code DESC"
        );
    }

    /** Última versão GERAL (não exclusiva de nenhuma empresa). */
    public function ultimaGeral(): ?array {
        return $this->db->queryOne(
            "SELECT * FROM app_versoes WHERE empresa_exclusiva_id IS NULL ORDER BY version_code DESC LIMIT 1"
        );
    }

    /**
     * Versão-alvo pra uma empresa: a que foi fixada manualmente no painel da
     * empresa, senão a última versão geral disponível.
     */
    public function alvoParaEmpresa(?int $empresaId): ?array {
        if ($empresaId) {
            $emp = $this->db->queryOne("SELECT app_versao_alvo_id FROM empresas WHERE id=?", [$empresaId]);
            $alvoId = $emp['app_versao_alvo_id'] ?? null;
            if ($alvoId) {
                $v = $this->findById((int)$alvoId);
                if ($v) return $v;
            }
        }
        return $this->ultimaGeral();
    }
}
