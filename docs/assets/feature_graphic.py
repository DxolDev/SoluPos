"""Genera el gráfico de funciones (feature graphic) 1024x500 para Google Play."""
from PIL import Image, ImageDraw, ImageFont
import os

LOGO = r"C:\Users\DaveX\AndroidStudioProjects\SoluPos\app\src\main\res\drawable\logo_solupos.png"
OUT_DIR = r"C:\Users\DaveX\AndroidStudioProjects\SoluPos\docs\assets"
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 1024, 500
BRAND = (32, 32, 255)          # #2020FF

# ---- Fondo: gradiente diagonal índigo ----
top = (24, 20, 150)            # índigo profundo
bot = (60, 54, 255)            # azul de marca vivo
bg = Image.new("RGB", (W, H))
px = bg.load()
for y in range(H):
    for x in range(W):
        t = (x / W * 0.45) + (y / H * 0.55)
        r = int(top[0] + (bot[0] - top[0]) * t)
        g = int(top[1] + (bot[1] - top[1]) * t)
        b = int(top[2] + (bot[2] - top[2]) * t)
        px[x, y] = (r, g, b)
bg = bg.convert("RGBA")

draw = ImageDraw.Draw(bg)

# ---- Círculos decorativos sutiles ----
def soft_circle(cx, cy, rad, color, alpha):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=color + (alpha,))
    return Image.alpha_composite(bg, layer)

bg = soft_circle(880, 90, 220, (255, 255, 255), 12)
bg = soft_circle(950, 430, 160, (255, 255, 255), 10)
draw = ImageDraw.Draw(bg)

# ---- Logo SOLU recoloreado a blanco (usa su alfa como máscara) ----
logo = Image.open(LOGO).convert("RGBA")
bbox = logo.split()[3].getbbox()          # bbox del canal alfa (recorta el vacío)
logo = logo.crop(bbox)
white = Image.new("RGBA", logo.size, (255, 255, 255, 0))
white.putalpha(logo.split()[3])           # blanco con la silueta del logo
logo = white

# El logo YA es el wordmark completo "SOLUPOS"
logo_h = 104
logo_w = int(logo.width * logo_h / logo.height)
logo = logo.resize((logo_w, logo_h), Image.LANCZOS)

# Fuentes (Windows)
def font(path, size):
    return ImageFont.truetype(path, size)

seg_semi = r"C:\Windows\Fonts\seguisb.ttf"
seg_reg = r"C:\Windows\Fonts\segoeui.ttf"
f_tag = font(seg_semi, 44)
f_sub = font(seg_reg, 31)

mark_x = 80
logo_top = 138

bg.alpha_composite(logo, (mark_x, logo_top))

# ---- Tagline ----
draw.text((mark_x + 4, logo_top + logo_h + 26), "Punto de venta móvil",
          font=f_tag, fill=(255, 255, 255, 255))
draw.text((mark_x + 6, logo_top + logo_h + 88), "Escanea códigos  ·  Imprime recibos",
          font=f_sub, fill=(223, 221, 255, 255))

# ---- Iconos de línea (columna derecha) ----
ic = (255, 255, 255, 235)
# Ticket / recibo
tx, ty = 812, 118
draw.rounded_rectangle([tx, ty, tx + 140, ty + 188], radius=14, outline=ic, width=6)
for i, yy in enumerate(range(ty + 34, ty + 160, 28)):
    ln = 92 if i % 2 == 0 else 64
    draw.line([tx + 24, yy, tx + 24 + ln, yy], fill=ic, width=6)

# Código de barras
bx, by = 812, 336
widths = [6, 3, 8, 3, 6, 10, 4, 7, 3, 9, 5, 4]
xx = bx
for w in widths:
    draw.rectangle([xx, by, xx + w, by + 66], fill=ic)
    xx += w + 7

out = os.path.join(OUT_DIR, "feature-graphic.png")
bg.convert("RGB").save(out, "PNG")
print("OK ->", out, bg.size)
