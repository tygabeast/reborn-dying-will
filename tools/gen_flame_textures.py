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

def img(): return Image.new('RGBA', (16, 16), (0, 0, 0, 0))

def soft(rx, ry, power=1.6, peak=255):
    im = img(); px = im.load()
    for y in range(16):
        for x in range(16):
            d = (((x + .5 - 8) / rx) ** 2 + ((y + .5 - 8) / ry) ** 2) ** .5
            if d < 1: px[x, y] = (255, 255, 255, int(peak * (1 - d) ** power))
    return im

def make(kind):
    im = img(); d = ImageDraw.Draw(im)
    if kind == 'px':
        d.rectangle([4, 4, 11, 11], fill=W); d.rectangle([5, 5, 10, 10], fill=(255, 255, 255, 255))
    elif kind == 'drop':
        d.polygon([(8, 1), (12, 8), (12, 11), (10, 14), (6, 14), (4, 11), (4, 8)], fill=(235, 235, 235, 255))
        d.rectangle([6, 8, 7, 10], fill=W)
    elif kind == 'bubble':
        d.ellipse([2, 2, 13, 13], outline=(255, 255, 255, 230)); d.rectangle([5, 5, 6, 6], fill=W)
        d.ellipse([3, 3, 12, 12], fill=(255, 255, 255, 40))
    elif kind == 'spark':
        d.line([(8, 1), (8, 14)], fill=(255, 255, 255, 220)); d.line([(1, 8), (14, 8)], fill=(255, 255, 255, 220))
        d.rectangle([7, 7, 8, 8], fill=W)
    elif kind == 'puff': im = soft(7.5, 7.5, 1.2, 230)
    elif kind == 'wisp': im = soft(7.5, 4.5, 1.1, 200)
    elif kind == 'shard':
        d.polygon([(8, 0), (12, 8), (8, 15), (4, 8)], fill=(210, 210, 210, 255))
        d.polygon([(8, 0), (12, 8), (8, 8)], fill=W); d.line([(8, 1), (8, 14)], fill=(255, 255, 255, 255))
    elif kind == 'chip':
        d.polygon([(2, 5), (9, 2), (14, 7), (12, 13), (5, 14), (3, 10)], fill=(220, 220, 220, 255))
        d.polygon([(2, 5), (9, 2), (8, 8)], fill=W)
    elif kind == 'leaf':
        d.polygon([(1, 8), (6, 3), (12, 4), (15, 8), (12, 12), (6, 13)], fill=(225, 225, 225, 255))
        d.line([(2, 8), (14, 8)], fill=W)
    elif kind == 'mini':
        d.polygon([(8, 1), (12, 7), (12, 11), (10, 14), (6, 14), (4, 11), (4, 7)], fill=(235, 235, 235, 255))
        d.polygon([(8, 6), (10, 10), (9, 13), (7, 13), (6, 10)], fill=W)
    elif kind == 'slash':
        d.line([(2, 13), (13, 2)], fill=W, width=2); d.line([(3, 13), (13, 3)], fill=(255, 255, 255, 120))
    elif kind == 'ring':
        d.ellipse([1, 1, 14, 14], outline=W); d.ellipse([2, 2, 13, 13], outline=(255, 255, 255, 110))
    elif kind == 'glow': im = soft(7.9, 7.9, 1.0, 255)
    return im

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
