package main

import (
	"fmt"
	"sync/atomic"
	"time"

	"github.com/lxn/walk"
	. "github.com/lxn/walk/declarative"
)

var (
	brandGreen     = walk.RGB(0, 184, 110)
	brandGreenDark = walk.RGB(0, 140, 84)
	bgLight        = walk.RGB(247, 248, 250)
	textMuted      = walk.RGB(110, 118, 129)
	textDark       = walk.RGB(30, 35, 41)
	textOnBrand    = walk.RGB(255, 255, 255)
	textOnBrandDim = walk.RGB(214, 247, 233)
)

// runStatusWindow mostra a janela gráfica com o status do relay (em vez do
// console) e bloqueia até ela ser fechada. Fechar a janela encerra o relay.
func runStatusWindow(lic *License, cfg Config) int {
	var (
		mw           *walk.MainWindow
		sessoesLabel *walk.Label
	)

	maxSessoesTxt := "Ilimitado"
	if lic.MaxSessions > 0 {
		maxSessoesTxt = fmt.Sprintf("%d", lic.MaxSessions)
	}
	maxDispositivosTxt := "Ilimitado"
	if lic.MaxDevices > 0 {
		maxDispositivosTxt = fmt.Sprintf("%d", lic.MaxDevices)
	}

	baseFont := Font{Family: "Segoe UI", PointSize: 9}

	win := MainWindow{
		AssignTo:   &mw,
		Title:      "ScanTE Relay",
		MinSize:    Size{Width: 340, Height: 440},
		Background: SolidColorBrush{Color: bgLight},
		Font:       baseFont,
		Layout:     VBox{MarginsZero: true, SpacingZero: true},
		Children: []Widget{
			// Cabeçalho
			Composite{
				Background: SolidColorBrush{Color: brandGreen},
				Layout:     VBox{Margins: Margins{Left: 20, Top: 16, Right: 20, Bottom: 16}, SpacingZero: true},
				Children: []Widget{
					Label{
						Text:      "ScanTE Relay",
						TextColor: textOnBrand,
						Font:      Font{Family: "Segoe UI", PointSize: 16, Bold: true},
					},
					Label{
						Text:      "Persistência de sessão para coletores",
						TextColor: textOnBrandDim,
						Font:      Font{Family: "Segoe UI", PointSize: 9},
					},
				},
			},

			// Corpo
			Composite{
				Layout: VBox{Margins: Margins{Left: 20, Top: 18, Right: 20, Bottom: 16}, Spacing: 14},
				Children: []Widget{
					Composite{
						Layout: HBox{MarginsZero: true, SpacingZero: true},
						Children: []Widget{
							Label{Text: "●", TextColor: brandGreen, Font: Font{Family: "Segoe UI", PointSize: 13, Bold: true}},
							Label{Text: "  Relay em execução", TextColor: textDark, Font: Font{Family: "Segoe UI", PointSize: 11, Bold: true}},
							HSpacer{},
						},
					},

					GroupBox{
						Title:  "Licença",
						Font:   baseFont,
						Layout: Grid{Columns: 2, Spacing: 8, Margins: Margins{Left: 14, Top: 16, Right: 14, Bottom: 12}},
						Children: []Widget{
							Label{Text: "Cliente:", TextColor: textMuted}, Label{Text: lic.Customer, TextColor: textDark},
							Label{Text: "Número de série:", TextColor: textMuted}, Label{Text: lic.Serial, TextColor: textDark},
							Label{Text: "Validade:", TextColor: textMuted}, Label{Text: lic.Expiry, TextColor: textDark},
							Label{Text: "Sessões máximas:", TextColor: textMuted}, Label{Text: maxSessoesTxt, TextColor: textDark},
							Label{Text: "Dispositivos máximos:", TextColor: textMuted}, Label{Text: maxDispositivosTxt, TextColor: textDark},
						},
					},

					GroupBox{
						Title:  "Conexão",
						Font:   baseFont,
						Layout: Grid{Columns: 2, Spacing: 8, Margins: Margins{Left: 14, Top: 16, Right: 14, Bottom: 12}},
						Children: []Widget{
							Label{Text: "Endereço de escuta:", TextColor: textMuted}, Label{Text: cfg.ListenAddr, TextColor: textDark},
							Label{Text: "Sessões ativas agora:", TextColor: textMuted},
							Label{AssignTo: &sessoesLabel, Text: "0", TextColor: brandGreenDark, Font: Font{Family: "Segoe UI", PointSize: 12, Bold: true}},
						},
					},

					VSpacer{},

					Label{
						Text:      "Fechar esta janela encerra o relay.",
						TextColor: textMuted,
						Font:      Font{Family: "Segoe UI", PointSize: 8},
					},
				},
			},
		},
	}

	if err := win.Create(); err != nil {
		crashPause(fmt.Sprintf("Não foi possível abrir a janela de status: %v", err))
		return 1
	}

	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()
	stop := make(chan struct{})
	go func() {
		for {
			select {
			case <-ticker.C:
				n := atomic.LoadInt64(&activeSessions)
				mw.Synchronize(func() {
					sessoesLabel.SetText(fmt.Sprintf("%d", n))
				})
			case <-stop:
				return
			}
		}
	}()

	code := mw.Run()
	close(stop)
	return code
}
