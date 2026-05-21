#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from reportlab.lib.pagesizes import letter, A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch, cm
from reportlab.lib.colors import HexColor, black, white
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, Image
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY
from datetime import datetime

# Criar PDF
pdf_path = "c:\\Users\\7700924385\\web\\trabalho\\appEmuladorTellnet\\Documentacao_App_Telnet.pdf"
doc = SimpleDocTemplate(pdf_path, pagesize=A4, topMargin=1*cm, bottomMargin=1*cm)

# Estilos customizados
styles = getSampleStyleSheet()
title_style = ParagraphStyle(
    'CustomTitle',
    parent=styles['Heading1'],
    fontSize=28,
    textColor=HexColor('#1f4788'),
    spaceAfter=12,
    alignment=TA_CENTER,
    fontName='Helvetica-Bold'
)

heading1_style = ParagraphStyle(
    'CustomHeading1',
    parent=styles['Heading1'],
    fontSize=16,
    textColor=HexColor('#2e5090'),
    spaceAfter=8,
    spaceBefore=10,
    fontName='Helvetica-Bold'
)

heading2_style = ParagraphStyle(
    'CustomHeading2',
    parent=styles['Heading2'],
    fontSize=13,
    textColor=HexColor('#3d6ba8'),
    spaceAfter=6,
    spaceBefore=8,
    fontName='Helvetica-Bold'
)

normal_style = ParagraphStyle(
    'CustomNormal',
    parent=styles['Normal'],
    fontSize=11,
    alignment=TA_JUSTIFY,
    spaceAfter=6,
    leading=14
)

# Criar conteúdo
story = []

# Capa
story.append(Spacer(1, 2*cm))
story.append(Paragraph("EMULADOR TELNET", title_style))
story.append(Paragraph("Aplicativo Android Enterprise", ParagraphStyle(
    'Subtitle',
    parent=styles['Normal'],
    fontSize=16,
    textColor=HexColor('#666666'),
    alignment=TA_CENTER,
    spaceAfter=1*cm
)))

story.append(Spacer(1, 1*cm))

# Data
data_atual = datetime.now().strftime("%d de %B de %Y").replace("January", "Janeiro").replace("February", "Fevereiro").replace("March", "Março").replace("April", "Abril").replace("May", "Maio").replace("June", "Junho").replace("July", "Julho").replace("August", "Agosto").replace("September", "Setembro").replace("October", "Outubro").replace("November", "Novembro").replace("December", "Dezembro")
story.append(Paragraph(f"<b>Data:</b> {data_atual}", ParagraphStyle(
    'Date',
    parent=styles['Normal'],
    fontSize=11,
    alignment=TA_CENTER,
    spaceAfter=2*cm
)))

story.append(Paragraph("<b>Versão:</b> 1.0.0", ParagraphStyle(
    'Version',
    parent=styles['Normal'],
    fontSize=11,
    alignment=TA_CENTER,
    spaceAfter=3*cm
)))

story.append(PageBreak())

# ÍNDICE
story.append(Paragraph("ÍNDICE", heading1_style))
story.append(Spacer(1, 0.3*cm))

indice = [
    "1. Visão Geral do Projeto",
    "2. Funcionalidades Implementadas",
    "3. Funcionalidades em Desenvolvimento",
    "4. Especificações Técnicas",
    "5. Segurança e Licenciamento",
    "6. Autenticação (Login/Cadastro)",
    "7. Cadastro de Empresa",
    "8. Roadmap Futuro",
]

for item in indice:
    story.append(Paragraph(f"• {item}", normal_style))

story.append(PageBreak())

# SEÇÃO 1: VISÃO GERAL
story.append(Paragraph("1. VISÃO GERAL DO PROJETO", heading1_style))
story.append(Spacer(1, 0.2*cm))

story.append(Paragraph(
    "<b>Objetivo:</b> Emulador Telnet avançado para Android, permitindo conexões a servidores Telnet remotos com suporte a cores ANSI, histórico de comandos, persistência de dados e licenciamento enterprise.",
    normal_style
))

story.append(Paragraph(
    "<b>Público-alvo:</b> Profissionais de TI, administradores de sistemas, desenvolvedores e entusiastas de tecnologia retrô.",
    normal_style
))

story.append(Paragraph(
    "<b>Plataforma:</b> Android 7.0+ (API 24), com interface nativa Kotlin + Android Jetpack.",
    normal_style
))

story.append(Spacer(1, 0.3*cm))

# SEÇÃO 2: FUNCIONALIDADES IMPLEMENTADAS
story.append(Paragraph("2. FUNCIONALIDADES IMPLEMENTADAS ✅", heading1_style))
story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("2.1 - Cliente Telnet (RFC 854)", heading2_style))
story.append(Paragraph(
    "• Socket TCP real com suporte completo ao protocolo Telnet RFC 854<br/>" +
    "• Negociação de opções (WILL, WONT, DO, DON'T)<br/>" +
    "• Leitura não-bloqueante com BufferedReader.ready()<br/>" +
    "• Tratamento automático de conexões e desconexões",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("2.2 - Terminal com Cores ANSI/VT100", heading2_style))
story.append(Paragraph(
    "• Parser completo de sequências ANSI (\\x1b[...m)<br/>" +
    "• Suporte a 16 cores + cores de background<br/>" +
    "• Estilos: bold, italic, underline, reverse<br/>" +
    "• Renderização em HTML com WebView",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("2.3 - Histórico de Comandos", heading2_style))
story.append(Paragraph(
    "• Navegação com setas ↑ e ↓<br/>" +
    "• Armazenamento em memória (máx 100 comandos)<br/>" +
    "• InputHistoryManager para controle<br/>" +
    "• Retenção durante sessão ativa",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("2.4 - Banco de Dados (Room v2)", heading2_style))
story.append(Paragraph(
    "• Persistência local com SQLite<br/>" +
    "• Entidades: SavedConnection, CommandHistory, SessionLog, AppPreference, LicenseInfo<br/>" +
    "• Migrações automáticas com fallbackToDestructiveMigration<br/>" +
    "• Operações assíncronas com Coroutines (Dispatcher.IO)",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("2.5 - Conexões Salvas", heading2_style))
story.append(Paragraph(
    "• Spinner dropdown com lista de hosts salvos<br/>" +
    "• Auto-preenchimento de host/porta ao selecionar<br/>" +
    "• Salvar novas conexões com nome customizável<br/>" +
    "• Favoritos e histórico de último acesso",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("2.6 - Licenciamento Enterprise", heading2_style))
story.append(Paragraph(
    "• Device Fingerprinting com SHA-256<br/>" +
    "• Licenças TRIAL (30 dias) e PREMIUM (1 ano)<br/>" +
    "• Validação por device impossível de burlar<br/>" +
    "• Menu 📜 Licença com dialog customizado<br/>" +
    "• Armazenamento seguro em banco de dados",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("2.7 - Controle de Teclado Virtual", heading2_style))
story.append(Paragraph(
    "• Auto-ocultar teclado ao conectar<br/>" +
    "• Auto-mostrar teclado ao desconectar<br/>" +
    "• Ocultar após envio de comando<br/>" +
    "• Melhor visualização do terminal",
    normal_style
))

story.append(PageBreak())

# SEÇÃO 3: FUNCIONALIDADES EM DESENVOLVIMENTO
story.append(Paragraph("3. FUNCIONALIDADES EM DESENVOLVIMENTO 🚀", heading1_style))
story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("3.1 - Autenticação (LOGIN)", heading2_style))
story.append(Paragraph(
    "<b>Status:</b> Planejado para Phase 7<br/>" +
    "<b>Descrição:</b> Sistema de login com autenticação por usuário e senha.<br/>" +
    "<b>Features:</b><br/>" +
    "• Tela de login com email/senha<br/>" +
    "• Backend Firebase Authentication (ou servidor custom)<br/>" +
    "• Recuperação de senha por email<br/>" +
    "• Armazenamento seguro de tokens<br/>" +
    "• Logout com limpeza de sessão",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("3.2 - Cadastro de Usuário", heading2_style))
story.append(Paragraph(
    "<b>Status:</b> Planejado para Phase 7<br/>" +
    "<b>Descrição:</b> Registro de novos usuários na plataforma.<br/>" +
    "<b>Fields:</b><br/>" +
    "• Nome completo<br/>" +
    "• Email (único, validado)<br/>" +
    "• Senha (mínimo 8 caracteres, com regras de força)<br/>" +
    "• Telefone (opcional)<br/>" +
    "• CPF/CNPJ<br/>" +
    "• Aceitar termos de serviço",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("3.3 - Cadastro de Empresa", heading2_style))
story.append(Paragraph(
    "<b>Status:</b> Planejado para Phase 8<br/>" +
    "<b>Descrição:</b> Registro de empresas para planos Premium.<br/>" +
    "<b>Fields:</b><br/>" +
    "• Razão social<br/>" +
    "• CNPJ (validado, único)<br/>" +
    "• Inscrição estadual<br/>" +
    "• Endereço completo (rua, número, cidade, CEP)<br/>" +
    "• Telefone comercial<br/>" +
    "• Email corporativo<br/>" +
    "• Website (opcional)<br/>" +
    "• Logo (upload de imagem)<br/>" +
    "• Plano (TRIAL, PREMIUM, ENTERPRISE)",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("3.4 - Copy/Paste no Terminal", heading2_style))
story.append(Paragraph(
    "<b>Status:</b> Planejado para Phase 5 (continuação)<br/>" +
    "• Selecionar texto da tela<br/>" +
    "• Copiar para clipboard<br/>" +
    "• Colar comandos do clipboard",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("3.5 - Suporte SSH", heading2_style))
story.append(Paragraph(
    "<b>Status:</b> Planejado para Phase 6<br/>" +
    "• Cliente SSH integrado<br/>" +
    "• Autenticação por chave privada (RSA/ED25519)<br/>" +
    "• Senha SSH<br/>" +
    "• Gerenciador de chaves locais",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("3.6 - Temas UI", heading2_style))
story.append(Paragraph(
    "<b>Status:</b> Planejado para Phase 9<br/>" +
    "• Dark Mode / Light Mode<br/>" +
    "• Customização de cores do terminal<br/>" +
    "• Seleção de fonte e tamanho",
    normal_style
))

story.append(PageBreak())

# SEÇÃO 4: ESPECIFICAÇÕES TÉCNICAS
story.append(Paragraph("4. ESPECIFICAÇÕES TÉCNICAS", heading1_style))
story.append(Spacer(1, 0.2*cm))

# Tabela de Stack Técnico
tech_data = [
    ['Componente', 'Tecnologia', 'Versão'],
    ['Linguagem', 'Kotlin', '1.8.10'],
    ['Java Runtime', 'OpenJDK (Corretto)', '11'],
    ['Android SDK', 'Android 14 (API 34)', '-'],
    ['Min SDK', 'Android 7.0 (API 24)', '-'],
    ['Gradle', 'Gradle Build System', '8.6'],
    ['AGP', 'Android Gradle Plugin', '7.4.2'],
    ['Architecture', 'MVVM + Repository Pattern', '-'],
    ['UI Framework', 'Android Jetpack', '-'],
    ['Database', 'Room ORM', '2.5.2'],
    ['Async', 'Kotlin Coroutines', '1.7.1'],
    ['Networking', 'Java Socket (RFC 854)', '-'],
]

tech_table = Table(tech_data, colWidths=[2.5*cm, 3.5*cm, 2*cm])
tech_table.setStyle(TableStyle([
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#2e5090')),
    ('TEXTCOLOR', (0, 0), (-1, 0), white),
    ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
    ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
    ('FONTSIZE', (0, 0), (-1, 0), 11),
    ('BOTTOMPADDING', (0, 0), (-1, 0), 8),
    ('BACKGROUND', (0, 1), (-1, -1), HexColor('#f0f0f0')),
    ('GRID', (0, 0), (-1, -1), 1, black),
    ('FONTSIZE', (0, 1), (-1, -1), 10),
]))

story.append(tech_table)
story.append(Spacer(1, 0.3*cm))

story.append(Paragraph(
    "<b>Arquitetura de Camadas:</b><br/>" +
    "• <b>UI Layer:</b> MainActivity, LicenseDialog, WebView para terminal<br/>" +
    "• <b>ViewModel Layer:</b> TelnetViewModel com LiveData/Flow<br/>" +
    "• <b>Repository Layer:</b> TelnetRepository abstrai operações BD<br/>" +
    "• <b>Database Layer:</b> Room com entities e DAOs<br/>" +
    "• <b>Network Layer:</b> TelnetClient (TCP Socket)",
    normal_style
))

story.append(Spacer(1, 0.3*cm))

story.append(Paragraph(
    "<b>Permissões Android:</b><br/>" +
    "• android.permission.INTERNET (conexões Telnet/SSH)<br/>" +
    "• android.permission.WRITE_EXTERNAL_STORAGE (salvar logs)",
    normal_style
))

story.append(PageBreak())

# SEÇÃO 5: SEGURANÇA
story.append(Paragraph("5. SEGURANÇA E LICENCIAMENTO", heading1_style))
story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("5.1 - Device Fingerprinting", heading2_style))
story.append(Paragraph(
    "• <b>Método:</b> SHA-256 hash de hardware único<br/>" +
    "• <b>Componentes:</b> ANDROID_ID + Model + Manufacturer + Serial + Fingerprint<br/>" +
    "• <b>Impossível burlar:</b> Requer mudança física do hardware<br/>" +
    "• <b>Formato:</b> 32 caracteres hexadecimais",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("5.2 - Modelos de Licença", heading2_style))
story.append(Paragraph(
    "• <b>TRIAL:</b> 30 dias de teste, recursos completos<br/>" +
    "• <b>PREMIUM:</b> 1 ano de acesso, suporte prioritário<br/>" +
    "• <b>ENTERPRISE:</b> Ilimitado, múltiplos devices, customização",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("5.3 - Validação de Licença", heading2_style))
story.append(Paragraph(
    "• Verifica device fingerprint na ativação<br/>" +
    "• Valida data de expiração<br/>" +
    "• Armazenamento seguro em banco local<br/>" +
    "• Sync com servidor (futuro)",
    normal_style
))

story.append(Spacer(1, 0.15*cm))
story.append(Paragraph("5.4 - Proteção de Dados", heading2_style))
story.append(Paragraph(
    "• Senhas não armazenadas (futuros logins via servidor)<br/>" +
    "• Histórico de comandos criptografado (fase 10)<br/>" +
    "• Conexões via HTTPS para APIs (futuro)<br/>" +
    "• Certificação SSL/TLS obrigatória em SSH",
    normal_style
))

story.append(PageBreak())

# SEÇÃO 6: AUTENTICAÇÃO
story.append(Paragraph("6. AUTENTICAÇÃO (LOGIN/CADASTRO)", heading1_style))
story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("6.1 - Fluxo de Login", heading2_style))
story.append(Paragraph(
    "<b>Tela de Login:</b><br/>" +
    "1. Usuário insere email e senha<br/>" +
    "2. Validação local (email válido, senha não vazia)<br/>" +
    "3. Envio para servidor (HTTPS POST)<br/>" +
    "4. Servidor retorna token JWT ou erro<br/>" +
    "5. Token armazenado localmente (EncryptedSharedPreferences)<br/>" +
    "6. Redirecionamento para MainActivity",
    normal_style
))

story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("6.2 - Fluxo de Cadastro de Usuário", heading2_style))
story.append(Paragraph(
    "<b>Tela de Registro:</b><br/>" +
    "1. Usuário preenche formulário (nome, email, senha)<br/>" +
    "2. Validação de força de senha (8+ caracteres, uppercase, números)<br/>" +
    "3. Verificação de email único (HTTPS POST)<br/>" +
    "4. Envio de confirmação por email (link com token)<br/>" +
    "5. Usuário clica link e confirma email<br/>" +
    "6. Redirecionamento para login<br/>" +
    "7. Login + redirect para app",
    normal_style
))

story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("6.3 - Recuperação de Senha", heading2_style))
story.append(Paragraph(
    "• Tela \"Esqueci a senha\"<br/>" +
    "• Inserir email cadastrado<br/>" +
    "• Email com link de reset<br/>" +
    "• Token com expiração (1 hora)<br/>" +
    "• Tela para nova senha<br/>" +
    "• Confirmação via email",
    normal_style
))

story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("6.4 - Segurança de Senhas", heading2_style))
story.append(Paragraph(
    "• Mínimo 8 caracteres<br/>" +
    "• Obrigatório: 1 maiúscula, 1 minúscula, 1 número, 1 símbolo<br/>" +
    "• Hashing: bcrypt no servidor (nunca transmitir plaintext)<br/>" +
    "• HTTPS TLS 1.2+ em todas as requisições<br/>" +
    "• Rate limiting: máx 5 tentativas falhadas/5 minutos",
    normal_style
))

story.append(PageBreak())

# SEÇÃO 7: CADASTRO DE EMPRESA
story.append(Paragraph("7. CADASTRO DE EMPRESA", heading1_style))
story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("7.1 - Requisitos de Cadastro", heading2_style))
story.append(Paragraph(
    "• <b>Usuário:</b> Deve estar logado<br/>" +
    "• <b>Permissão:</b> Apenas usuário com role 'ADMIN' ou 'OWNER'<br/>" +
    "• <b>CNPJ:</b> Validado contra banco de dados (único)<br/>" +
    "• <b>Confirmação:</b> Email de verificação da empresa",
    normal_style
))

story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("7.2 - Fluxo de Cadastro Empresarial", heading2_style))
story.append(Paragraph(
    "<b>Passo 1 - Dados Básicos:</b><br/>" +
    "• Razão social (obrigatório)<br/>" +
    "• CNPJ (validação automática)<br/>" +
    "• Inscrição estadual (opcional)<br/>" +
    "• Classificação (Indústria, Comércio, Serviços, etc)<br/>" +
    "<br/>" +
    "<b>Passo 2 - Contato:</b><br/>" +
    "• Email corporativo<br/>" +
    "• Telefone<br/>" +
    "• Website<br/>" +
    "<br/>" +
    "<b>Passo 3 - Endereço:</b><br/>" +
    "• Rua, número, complemento<br/>" +
    "• CEP (auto-preenche cidade/estado)<br/>" +
    "• Cidade, estado<br/>" +
    "• País (padrão Brasil)<br/>" +
    "<br/>" +
    "<b>Passo 4 - Documentos:</b><br/>" +
    "• Upload de logo (PNG/JPG)<br/>" +
    "• Descrição (até 500 caracteres)<br/>" +
    "<br/>" +
    "<b>Passo 5 - Plano:</b><br/>" +
    "• Seleção de plano (TRIAL, PREMIUM)<br/>" +
    "• Método de pagamento (Cartão, PIX, Boleto)<br/>" +
    "• Resumo com valores<br/>" +
    "• Aceitar termos",
    normal_style
))

story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("7.3 - Validações", heading2_style))
story.append(Paragraph(
    "• CNPJ: Validação via algoritmo (DV check)<br/>" +
    "• CEP: Consulta API de endereços (ViaCEP/Postmon)<br/>" +
    "• Email: Validação de domínio corporativo<br/>" +
    "• Telefone: Formato internacional (+55 11 XXXXX-XXXX)<br/>" +
    "• Tamanho de arquivo de logo: máx 5MB, min 200x200px",
    normal_style
))

story.append(Spacer(1, 0.2*cm))

story.append(Paragraph("7.4 - Após Cadastro", heading2_style))
story.append(Paragraph(
    "• Email de confirmação com código de verificação<br/>" +
    "• Usuário criador vira OWNER da empresa<br/>" +
    "• Pode adicionar outros usuários (ADMIN, USER)<br/>" +
    "• Ativa licença TRIAL (30 dias) ou PREMIUM<br/>" +
    "• Acesso ao painel administrativo",
    normal_style
))

story.append(PageBreak())

# SEÇÃO 8: ROADMAP
story.append(Paragraph("8. ROADMAP FUTURO", heading1_style))
story.append(Spacer(1, 0.2*cm))

roadmap_data = [
    ['Phase', 'Funcionalidade', 'Status'],
    ['1-4', 'Client Telnet + Histórico', '✅ Concluído'],
    ['5', 'Database + Conexões Salvas', '✅ Concluído'],
    ['6', 'Licenciamento Enterprise', '✅ Concluído'],
    ['7', 'Autenticação (Login/Cadastro)', '🚀 Em planejamento'],
    ['8', 'Gestão de Empresas', '🚀 Em planejamento'],
    ['9', 'Copy/Paste Terminal', '🚀 Próximo'],
    ['10', 'SSH Support', '📋 Backlog'],
    ['11', 'UI Themes (Dark/Light)', '📋 Backlog'],
    ['12', 'Admin Dashboard', '📋 Backlog'],
    ['13', 'Analytics & Logs', '📋 Backlog'],
    ['14', 'Multi-language (i18n)', '📋 Backlog'],
]

roadmap_table = Table(roadmap_data, colWidths=[1.5*cm, 4.5*cm, 2*cm])
roadmap_table.setStyle(TableStyle([
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#2e5090')),
    ('TEXTCOLOR', (0, 0), (-1, 0), white),
    ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
    ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
    ('FONTSIZE', (0, 0), (-1, 0), 10),
    ('BOTTOMPADDING', (0, 0), (-1, 0), 8),
    ('BACKGROUND', (0, 1), (-1, -1), HexColor('#f0f0f0')),
    ('GRID', (0, 0), (-1, -1), 1, black),
    ('FONTSIZE', (0, 1), (-1, -1), 9),
]))

story.append(roadmap_table)
story.append(Spacer(1, 0.3*cm))

story.append(Paragraph(
    "<b>Próximos 3 meses:</b><br/>" +
    "• Implementação de autenticação com Firebase<br/>" +
    "• Sistema de gestão de usuários e empresas<br/>" +
    "• Backend API (Node.js + Express ou Python/Django)<br/>" +
    "• Dashboard administrativo<br/>" +
    "• Testes de segurança e penetração",
    normal_style
))

story.append(PageBreak())

# SEÇÃO 9: CONCLUSÃO
story.append(Paragraph("CONCLUSÃO", heading1_style))
story.append(Spacer(1, 0.2*cm))

story.append(Paragraph(
    "O Emulador Telnet é um aplicativo Android robusto e profissional que combina funcionalidades clássicas de terminal com segurança moderna e licenciamento enterprise. Com a arquitetura MVVM e uso de Room Database, o app oferece performance e confiabilidade para usuários exigentes.",
    normal_style
))

story.append(Spacer(1, 0.15*cm))

story.append(Paragraph(
    "As próximas fases focam em autenticação, gestão de usuários/empresas e um ecosistema completo de plataforma, mantendo o foco em segurança, escalabilidade e experiência do usuário.",
    normal_style
))

story.append(Spacer(1, 0.3*cm))

story.append(Paragraph(
    "<b>📧 Contato & Suporte:</b><br/>" +
    "GitHub: <u>https://github.com/gabrielvanzella1/appemuladortellnet</u><br/>" +
    "Desenvolvedor: Gabriel Vanzella<br/>" +
    "Email: suporte@appemuladortellnet.com",
    normal_style
))

# Criar PDF
doc.build(story)

print(f"✅ PDF gerado com sucesso: {pdf_path}")
