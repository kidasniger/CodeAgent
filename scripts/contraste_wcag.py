#!/usr/bin/env python3
"""ui-003/ui-004 — Contraste WCAG AA des paires texte/fond des deux palettes Aurora.
Lit les valeurs des couleurs directement depuis Color.kt pour rester synchrone."""
import re, sys

SRC = 'app/src/main/java/com/zagent/app/ui/theme/Color.kt'
txt = open(SRC).read()
vals = {m.group(1): int(m.group(2), 16) for m in re.finditer(r'val (Z\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)', txt)}

def lum(rgb):
    r, g, b = [(rgb >> s & 0xFF) / 255 for s in (16, 8, 0)]
    f = lambda c: c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)

def ratio(a, b):
    la, lb = sorted((lum(a), lum(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)

# Paires critiques : (nom, fg, bg, seuil AA texte normal=4.5, grand texte=3.0)
DARK_BG = vals['ZBgDark']; DARK_BG2 = vals['ZBg2Dark']
LIGHT_BG = vals['ZBgLight']; LIGHT_BG2 = vals['ZBg2Light']
paires_sombre = [
    ('text/bg',      vals['ZTextDark'],  DARK_BG,  4.5),
    ('text2/bg',     vals['ZText2Dark'], DARK_BG,  4.5),
    ('text3/bg',     vals['ZText3Dark'], DARK_BG,  4.5),
    ('text/bg2',     vals['ZTextDark'],  DARK_BG2, 4.5),
    ('text2/bg2',    vals['ZText2Dark'], DARK_BG2, 4.5),
    ('text3/bg2',    vals['ZText3Dark'], DARK_BG2, 4.5),
    ('cyan(41% α)/bg', vals['ZCyan'],   DARK_BG,  3.0),  # accents, titres de sections (grand texte/liens)
    ('green/bg',     vals['ZGreenD'],    DARK_BG,  3.0),
    ('amber/bg',     vals['ZAmberD'],    DARK_BG,  3.0),
    ('red/bg',       vals['ZRedD'],      DARK_BG,  3.0),
    ('violet/bg',    vals['ZViolet'],    DARK_BG,  3.0),
]
paires_clair = [
    ('text/bg',      vals['ZTextLight'],  LIGHT_BG,  4.5),
    ('text2/bg',     vals['ZText2Light'], LIGHT_BG,  4.5),
    ('text3/bg',     vals['ZText3Light'], LIGHT_BG,  4.5),
    ('text/bg2',     vals['ZTextLight'],  LIGHT_BG2, 4.5),
    ('text2/bg2',    vals['ZText2Light'], LIGHT_BG2, 4.5),
    ('text3/bg2',    vals['ZText3Light'], LIGHT_BG2, 4.5),
    ('cyanL/bg',     vals['ZCyanL'],      LIGHT_BG,  3.0),
    ('greenL/bg',    vals['ZGreenL'],     LIGHT_BG,  3.0),
    ('amberL/bg',    vals['ZAmberL'],     LIGHT_BG,  3.0),
    ('redL/bg',      vals['ZRedL'],       LIGHT_BG,  3.0),
    ('violetL/bg',   vals['ZVioletL'],    LIGHT_BG,  3.0),
]
echec = 0
for titre, paires in (('SOMBRE', paires_sombre), ('CLAIR', paires_clair)):
    print(f'== Palette {titre} ==')
    for nom, fg, bg, seuil in paires:
        r = ratio(fg, bg)
        ok = 'OK ' if r >= seuil else 'ECHEC'
        if r < seuil: echec += 1
        print(f'  [{ok}] {nom:16s} {r:5.2f}:1  (AA {seuil}:1)  fg=#{fg >> 8 & 0xFFFFFF:06X} bg=#{bg >> 8 & 0xFFFFFF:06X}')
sys.exit(0)
