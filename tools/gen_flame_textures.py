#!/usr/bin/env python3
"""Generates the flame particle sprites, the white mesh texture and the script-ring band.
All sprites are white/grey on transparent so the game can tint them with the flame's colours."""
import os, random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'reborn_dying_will')
PART = os.path.join(ROOT, 'textures', 'particle')
ENT = os.path.join(ROOT, 'textures', 'entity')
os.makedirs(PART, exist_ok=True); os.makedirs(ENT, exist_ok=True)
KINDS = ['px', 'drop', 'bubble', 'spark', 'puff', 'wisp', 'shard', 'chip', 'leaf', 'mini', 'slash', 'ring', 'glow']
W = (255, 255, 255, 255)

R = 32          # sprite size in the atlas
SS = 8          # supersampling: shapes are drawn on a 16-unit grid at 16*SS pixels, then reduced

def img(n=16 * SS): return Image.new('RGBA', (n, n), (0, 0, 0, 0))

def P(*pts): return [(x * SS, y * SS) for x, y in pts]

def soft(rx, ry, power=1.6, peak=255):
    im = Image.new('RGBA', (R, R), (0, 0, 0, 0)); px = im.load(); c = R / 2; k = R / 16
    for y in range(R):
        for x in range(R):
            d = (((x + .5 - c) / (rx * k)) ** 2 + ((y + .5 - c) / (ry * k)) ** 2) ** .5
            if d < 1: px[x, y] = (255, 255, 255, int(peak * (1 - d) ** power))
    return im

def finish(im):
    im = im.resize((R, R), Image.LANCZOS)
    px = im.load()
    for y in range(R):
        for x in range(R):
            r, g, b, a = px[x, y]
            px[x, y] = (255, 255, 255, 0) if a < 8 else (min(255, r), min(255, g), min(255, b), a)
    return im

def make(kind):
    im = img(); d = ImageDraw.Draw(im)
    w = lambda n: max(1, int(n * SS))
    if kind == 'px':
        d.rounded_rectangle(P((4.5, 4.5), (11.5, 11.5)), radius=w(1.2), fill=W)
    elif kind == 'drop':
        d.polygon(P((8, 1.5), (11.5, 8), (11.8, 10.5), (10.2, 13.6), (8, 14.4), (5.8, 13.6), (4.2, 10.5), (4.5, 8)), fill=(235, 235, 235, 255))
        d.ellipse(P((6, 8.5), (7.6, 11)), fill=W)
    elif kind == 'bubble':
        d.ellipse(P((2, 2), (14, 14)), fill=(255, 255, 255, 45), outline=(255, 255, 255, 235), width=w(1.1))
        d.ellipse(P((4.5, 4.5), (6.8, 6.8)), fill=W)
    elif kind == 'spark':
        d.line(P((8, 1), (8, 15)), fill=(255, 255, 255, 230), width=w(1.2)); d.line(P((1, 8), (15, 8)), fill=(255, 255, 255, 230), width=w(1.2))
        d.ellipse(P((6.5, 6.5), (9.5, 9.5)), fill=W)
    elif kind == 'puff': return soft(7.5, 7.5, 1.4, 220)
    elif kind == 'wisp': return soft(7.5, 4.5, 1.4, 190)
    elif kind == 'shard':
        d.polygon(P((8, 0.5), (12, 8), (8, 15.5), (4, 8)), fill=(205, 205, 205, 255))
        d.polygon(P((8, 0.5), (12, 8), (8, 8)), fill=W); d.line(P((8, 1), (8, 15)), fill=W, width=w(.6))
    elif kind == 'chip':
        d.polygon(P((2, 5), (9, 2), (14, 7), (12, 13), (5, 14), (3, 10)), fill=(215, 215, 215, 255))
        d.polygon(P((2, 5), (9, 2), (8, 8)), fill=W)
    elif kind == 'leaf':
        d.polygon(P((1, 8), (4, 4.5), (8, 3.5), (12, 4.5), (15, 8), (12, 11.5), (8, 12.5), (4, 11.5)), fill=(225, 225, 225, 255))
        d.line(P((2, 8), (14, 8)), fill=W, width=w(.7))
    elif kind == 'mini':
        d.polygon(P((8, 1), (11.5, 7), (12, 10.5), (10, 14), (6, 14), (4, 10.5), (4.5, 7)), fill=(235, 235, 235, 255))
        d.polygon(P((8, 6), (10, 10), (9, 13), (7, 13), (6, 10)), fill=W)
    elif kind == 'slash':
        d.line(P((3, 13), (13, 3)), fill=W, width=w(1.4)); d.line(P((4, 13.5), (13.5, 4)), fill=(255, 255, 255, 110), width=w(.8))
    elif kind == 'ring':
        d.ellipse(P((1.5, 1.5), (14.5, 14.5)), outline=W, width=w(1.1)); d.ellipse(P((3, 3), (13, 13)), outline=(255, 255, 255, 90), width=w(.7))
    elif kind == 'glow': return soft(7.9, 7.9, 1.2, 255)
    return finish(im)

for k in KINDS: make(k).save(os.path.join(PART, 'mote_%s.png' % k))
Image.new('RGBA', (4, 4), (255, 255, 255, 255)).save(os.path.join(ENT, 'flame_white.png'))

# script ring: a band of glyphs that wraps once around the ring
random.seed(7)
G = [[(0, 0, 1, 0), (.5, 0, .5, 1)], [(0, 0, 0, 1), (0, 1, 1, 1)], [(0, 1, .5, 0), (.5, 0, 1, 1)], [(0, .5, 1, .5), (.5, 0, .5, 1)],
     [(0, 0, 1, 1)], [(0, 0, 1, 0), (0, 1, 1, 1), (.5, 0, .5, 1)], [(0, 0, 0, 1), (1, 0, 1, 1), (0, .5, 1, .5)], [(.2, 0, .8, 0), (.8, 0, .2, 1), (.2, 1, .8, 1)]]
S = 4
band = Image.new('RGBA', (512 * S, 64 * S), (0, 0, 0, 0)); bd = ImageDraw.Draw(band)
for y in (6, 57): bd.line([(0, y * S), (512 * S, y * S)], fill=(255, 255, 255, 120), width=2 * S)
for i in range(32):
    x0 = i * 16 + 3; y0 = 16
    for g in random.choice(G):
        bd.line([((x0 + g[0] * 10) * S, (y0 + g[1] * 32) * S), ((x0 + g[2] * 10) * S, (y0 + g[3] * 32) * S)], fill=W, width=3 * S)
band.resize((512, 64), Image.LANCZOS).save(os.path.join(ENT, 'script_ring.png'))
print('ok')

# ---- flame-coloured glints for infused items (64x64, tileable). Tinted by the flame's more colourful shade.
import math
LOOKS = {  # flame: (core, edge) as in FlameLooks.java
    'sky': (0xFF8A1E, 0xFFD98A), 'storm': (0xFF4D6D, 0x7D0C20), 'rain': (0x2F80ED, 0x8FE3FF), 'sun': (0xFFFBE0, 0xE3E83A),
    'lightning': (0xD8FFE0, 0x3DDC64), 'cloud': (0xC026D3, 0xE0C6FF), 'mist': (0x6C3FA8, 0xC4B5FF), 'earth': (0xF04A1D, 0xFFA23A),
    'glacier': (0x9A1EA9, 0xAF6BFF), 'forest': (0xFFEF80, 0xCAD018), 'mountain': (0x7AFF95, 0x22BE49), 'swamp': (0xFF0B37, 0x640A1A),
    'desert': (0x563286, 0x7E5EFF), 'river': (0x1264D1, 0x3FCFFF), 'night': (0x0B0618, 0x3B1D7A)}
def rgb(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
def sat(c):
    r, g, b = rgb(c); mx, mn = max(r, g, b), min(r, g, b)
    return 0 if mx == 0 else (mx - mn) / mx
MISC = os.path.join(ROOT, 'textures', 'misc'); os.makedirs(MISC, exist_ok=True)
N = 64
def h2(x, y): return ((x * 374761393 + y * 668265263) ^ ((x * 374761393 + y * 668265263) >> 13)) * 1274126177 & 0xFFFF
for name, (core, edge) in LOOKS.items():
    hue = core if sat(core) >= sat(edge) else edge
    if name == 'night': hue = 0x9B6BFF  # a dark flame glints in its rim colour
    hr, hg, hb = rgb(hue)
    m = max(hr, hg, hb, 1); hr, hg, hb = hr * 255 / m, hg * 255 / m, hb * 255 / m  # full brightness, same hue
    im = Image.new('RGB', (N, N)); px = im.load()
    for y in range(N):
        for x in range(N):
            v = 0.0
            for k, (fx, fy, ph, w) in enumerate([(1, 1, 0.0, .55), (2, 1, 1.7, .30), (1, 3, 3.1, .25), (3, 2, 4.4, .15)]):
                v += w * (0.5 + 0.5 * math.sin(2 * math.pi * (fx * x + fy * y) / N + ph))
            v = (v / 1.25) ** .8 + (h2(x // 2, y // 2) / 65535 - .5) * .08
            v = max(0.0, min(1.0, v))
            v *= .82
            w = max(0.0, v - .65) * 1.6 * 255  # the glint blend squares colour, so it needs bright texels; peaks go white
            px[x, y] = (min(255, int(hr * v + w)), min(255, int(hg * v + w)), min(255, int(hb * v + w)))
    im.save(os.path.join(MISC, 'glint_%s.png' % name))
print('glints ok')
