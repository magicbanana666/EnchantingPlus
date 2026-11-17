"""Generate the arcane table's pixel textures and NeoForge OBJ block model.

Run with Python 3 and Pillow. Enchanting logic and book animation are unchanged.
"""
import json
import math
import random
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/eplus'
OUT = ASSETS / 'textures/block/arcane_table'
OUT.mkdir(parents=True, exist_ok=True)
RNG = random.Random(1741)


def surface(name, base, noise=5, size=32):
    im = Image.new('RGB', (size, size))
    for y in range(size):
        for x in range(size):
            n = RNG.randint(-noise, noise)
            im.putpixel((x, y), tuple(max(0, min(255, c+n)) for c in base))
    return im, ImageDraw.Draw(im)


def save(name, im):
    im.save(OUT / (name + '.png'))


stone, d = surface('stone', (39, 32, 48), 5)
for row in range(0,32,8):
    offset = 0 if row % 16 == 0 else 8
    d.line((0,row,31,row), fill=(24,21,31))
    d.line((0,row+1,31,row+1), fill=(55,44,65))
    for col in range(offset,32,16):
        d.line((col,row,col,row+7), fill=(25,21,32))
        d.line((col+1,row+2,col+1,row+6), fill=(48,39,59))
for x, y in [(2, 7), (18, 3), (11, 22), (25, 17), (3, 29)]:
    d.line([(x,y),(x+4,y),(x+6,y+2),(x+9,y+2)], fill=(49,38,61))
    d.line([(x,y+1),(x+4,y+1)], fill=(24,21,31))
save('stone', stone)

metal, d = surface('brass', (129, 89, 44), 7)
d.line((0,0,31,0), fill=(211,166,88))
d.line((0,1,31,1), fill=(175,128,62))
d.line((0,30,31,30), fill=(92,60,33))
d.line((0,31,31,31), fill=(61,41,29))
for x in (0, 16, 31):
    d.line((x,2,x,29), fill=(97,65,36))
save('brass', metal)

pillar, d = surface('pillar', (37,31,46), 3)
for x, color in [(0,(18,16,24)),(2,(62,49,72)),(5,(27,23,34)),(26,(23,20,30)),(29,(58,46,68)),(31,(17,15,23))]:
    d.line((x,0,x,31), fill=color)
d.line([(12,7),(20,7),(20,16),(13,16),(13,12),(16,12)], fill=(66,49,77), width=2)
d.line([(11,23),(19,23),(19,27),(15,27)], fill=(24,20,31), width=2)
save('pillar', pillar)

panel, d = surface('panel', (34,26,44), 3,64)
for inset, color in [(0,(17,14,23)),(2,(76,56,88)),(4,(46,34,59)),(7,(22,18,30)),(9,(57,40,72))]:
    d.rectangle((inset,inset,63-inset,63-inset), outline=color)
# Purposeful rune, with a dark violet halo and crisp lilac core.
paths = [[(32,21),(40,30),(32,39),(24,30),(32,21)],[(32,14),(32,21)],[(32,39),(32,50)],[(18,22),(18,44),(22,44)],[(46,22),(46,44),(42,44)]]
for pts in paths: d.line(pts, fill=(61,30,82), width=5)
for pts in paths: d.line(pts, fill=(129,58,173), width=3)
for pts in paths[:3]: d.line(pts, fill=(208,127,242), width=1)
save('panel', panel)

# Brick-pattern tabletop, retaining the original purple-and-brass inlay.
top = stone.resize((64,64), Image.Resampling.NEAREST)
d = ImageDraw.Draw(top)
d.rectangle((3,3,60,60), outline=(78,55,80))
d.ellipse((10,10,53,53), outline=(76,35,99), width=3)
d.ellipse((11,11,52,52), outline=(177,86,215), width=1)
d.line([(32,17),(47,32),(32,47),(17,32),(32,17)], fill=(161,116,60), width=1)
for x,y in [(32,17),(47,32),(32,47),(17,32)]:
    d.rectangle((x-2,y-1,x+2,y+1), outline=(205,157,79))
d.rectangle((29,29,34,34), outline=(194,114,232))
for x,y in [(17,13),(44,13),(17,49),(44,49)]:
    d.line([(x,y),(x+3,y),(x+3,y+2)], fill=(184,130,67))
save('top', top)

# Mottled plum obsidian on the continuous bevel, without brick stripes.
edge, d = surface('edge', (34,27,42), 3)
edge_rng=random.Random(9401)
for _ in range(65):
    x,y=edge_rng.randrange(32),edge_rng.randrange(32)
    c=edge_rng.choice([(44,32,53),(52,37,62),(28,23,35),(62,42,73)])
    d.rectangle((x,y,min(31,x+edge_rng.randrange(1,4)),min(31,y+edge_rng.randrange(1,3))),fill=c)
d.line((0,0,31,0),fill=(67,47,76))
d.line((0,31,31,31),fill=(19,16,25))
save('edge',edge)

# A sparse alpha mask replaces the dense borrowed magic-circle texture.
# The renderer supplies a warm brass tint and caps opacity during the surge.
circle=Image.new('RGBA',(128,128),(255,255,255,0))
cd=ImageDraw.Draw(circle)
for angle in range(0,360,45):
    cd.arc((9,9,118,118),angle+6,angle+36,fill=(255,255,255,190),width=1)
for angle in range(0,360,90):
    a=math.radians(angle)
    x,y=63.5+48*math.cos(a),63.5+48*math.sin(a)
    cd.polygon([(round(x),round(y-2)),(round(x+2),round(y)),(round(x),round(y+2)),(round(x-2),round(y))],fill=(255,255,255,160))
circle.save(ASSETS/'textures/fx/arcane_circle.png')

# Additive ribbon profile: black edges, a narrow white core; tint comes from
# the renderer. Continuous along U so segment boundaries stay invisible.
ribbon=Image.new('RGBA',(16,16))
for y in range(16):
    k=max(0,1-abs(y-7.5)/7.5)**2
    v=round(255*k)
    for x in range(16): ribbon.putpixel((x,y),(v,v,v,255))
ribbon.save(ASSETS/'textures/fx/arcane_ribbon.png')
Image.new('RGBA',(4,4),(255,255,255,255)).save(ASSETS/'textures/fx/arcane_jewel.png')


gem, d = surface('crystal', (132,61,177), 3,16)
d.polygon([(0,0),(8,0),(5,8),(0,13)], fill=(198,118,234))
d.polygon([(8,0),(15,0),(15,12),(5,8)], fill=(150,77,202))
d.polygon([(0,13),(5,8),(10,15),(0,15)], fill=(105,42,151))
d.line((0,0,15,0), fill=(237,182,255))
d.line((5,8,8,0), fill=(223,151,252))
save('crystal', gem)

elements = []


def box(name, a, b, tex='stone', top_tex=None, rotation=None, emission=0):
    faces = {}
    for face in ('down','up','north','south','west','east'):
        uv = [0,0,16,16]
        if tex == 'stone':
            if face in ('up','down'): uv = [a[0],a[2],b[0],b[2]]
            elif face in ('north','south'): uv = [a[0],16-b[1],b[0],16-a[1]]
            else: uv = [a[2],16-b[1],b[2],16-a[1]]
        f = {'uv':uv, 'texture':'#'+(top_tex if face=='up' and top_tex else tex)}
        # This is a non-full block: keep every underside closed even beside
        # opaque terrain. noOcclusion also preserves the ground around the feet.
        faces[face] = f
    element = {'name':name,'from':a,'to':b,'faces':faces}
    if rotation: element['rotation'] = rotation
    if emission: element['light_emission'] = emission
    elements.append(element)


box('Stepped foundation',[.5,0,.5],[15.5,1.25,15.5])
box('Foundation bevel',[1,1.25,1],[15,1.75,15])
box('Inset pedestal',[2.5,1.75,2.5],[13.5,9,13.5])
# Framed recessed panels on all four sides; full-face UVs keep runes upright.
box('North rune panel',[4.5,2.5,2.35],[11.5,8.5,2.5],'panel',emission=3)
box('South rune panel',[4.5,2.5,13.5],[11.5,8.5,13.65],'panel',emission=3)
box('West rune panel',[2.35,2.5,4.5],[2.5,8.5,11.5],'panel',emission=3)
box('East rune panel',[13.5,2.5,4.5],[13.65,8.5,11.5],'panel',emission=3)
for x in (1.5,11.5):
    for z in (1.5,11.5):
        # Avoid coplanar foot/plinth faces, which flicker when viewed underneath.
        box('Corner foot',[x-.6,.05,z-.6],[x+3.6,1.85,z+3.6])
        box('Brass foot collar',[x-.1,1.75,z-.1],[x+3.1,2.35,z+3.1],'brass')
        box('Carved corner support',[x,2.35,z],[x+3,8.5,z+3],'pillar')
        box('Brass capital',[x-.15,8.5,z-.15],[x+3.15,9.1,z+3.15],'brass')
        box('Stepped capital',[x-.35,9.1,z-.35],[x+3.35,9.65,z+3.35])
box('Table underside',[.4,9.25,.4],[15.6,9.8,15.6])
box('Aged brass rim',[0,9.8,0],[16,10.45,16],'brass')
# Restore the entire original brick tabletop silhouette, not just its inlay.
# These ledges use the masonry texture and original cropped UV coordinates.
box('Broad obsidian edge',[0,10.45,0],[16,10.9,16])
box('Lower bevel step',[.25,10.9,.25],[15.75,11.25,15.75])
box('Upper bevel step',[.6,11.25,.6],[15.4,11.6,15.4])
box('Inlaid enchanting surface',[1,11.6,1],[15,12,15],top_tex='top')
for x in (2.2,13.8):
    for z in (2.2,13.8):
        box('Brass crystal socket',[x-1.1,11.8,z-1.1],[x+1.1,12.25,z+1.1],'brass')

textures = {n:'eplus:block/arcane_table/'+n for n in ('stone','brass','pillar','panel','top','crystal','edge')}
textures['particle'] = textures['stone']
from table_mesh import export_table
mesh_faces=export_table(ASSETS,elements,textures)
for e in elements:
    assert all(a<b for a,b in zip(e['from'],e['to'])), e['name']
    assert all(0<=v<=16 for v in e['from']+e['to']), e['name']
    for f in e['faces'].values():
        assert f['texture'][1:] in textures
        assert all(0<=v<=16 for v in f['uv'])

# A NEW texture for the existing animated BookModel, using its real 64x32 UV
# layout at 4x resolution. Covers, page edges and flipping sheets stay separate.
S = 4
book = Image.new('RGB',(64*S,32*S),(48,23,48))
bd = ImageDraw.Draw(book)
def rect(bounds,color,width=0):
    b=tuple(round(v*S) for v in bounds)
    if width: bd.rectangle(b,outline=color,width=width)
    else: bd.rectangle(b,fill=color)

for start in (0,6,16,22):
    rect((start,0,start+5.9,9.9),(54,25,51))
    rect((start+.4,.4,start+5.5,9.5),(99,54,80),1)
    rect((start+.7,.7,start+5.2,9.2),(32,16,35),1)
    rect((start+.1,.1,start+5.8,9.8),(192,136,59),2)
    rect((start+.3,.3,start+5.6,9.6),(235,184,91),1)
    # Small brass corner protectors, leaving the plum leather dominant.
    for x in (start+.25,start+4.75):
        for y in (.25,8.8): rect((x,y,x+.8,y+.8),(166,119,61))
    cx=(start+3)*S
    bd.line([(cx,3*S),(cx+S,5*S),(cx,7*S),(cx-S,5*S),(cx,3*S)],fill=(163,78,186),width=2)
rect((12,0,15.9,9.9),(43,20,43))
for y in (1,4.5,8.5): rect((12,y,15.9,y+.3),(151,104,54))
# Cream page blocks and all four page faces, including the turning sheets.
rect((0,10,35,19.5),(217,192,150))
for x0,x1,y0,y1 in [(1,6,11,19),(7,12,11,19),(13,18,11,19),(19,24,11,19),(24,29,10,18),(29,34,10,18)]:
    rect((x0+.1,y0+.1,x1-.1,y1-.1),(241,220,182))
    rect((x0+.35,y0+.35,x1-.35,y1-.35),(224,198,156),1)
    # Angular glyphs remain readable on fixed pages and flipping sheets.
    for row in range(3):
        gx=x0+.9+(row%2)*.35; gy=y0+1.2+row*2
        if row==1:
            rect((gx,gy,gx+1.7,gy+1.05),(133,67,148))
            rect((gx+.5,gy+.3,gx+1.2,gy+.75),(241,220,182))
        else:
            rect((gx,gy,gx+1.6,gy+.4),(133,67,148))
            rect((gx+1.1,gy+.4,gx+1.6,gy+.95),(133,67,148))
            rect((gx+.5,gy+.65,gx+1.1,gy+.95),(133,67,148))
# Reserved atlas area for the new three-dimensional cover rails. The existing
# cover and page UV regions above stay unchanged.
rect((36,0,51.9,15.9),(185,125,47))
for y,color in [(0,(255,212,116)),(1,(230,172,73)),(14,(132,80,31)),(15,(100,59,27))]:
    rect((36,y,51.9,y+.5),color)
book_out=ASSETS/'textures/entity/book/arcane.png'
book.save(book_out)
assert book.size==(256,128)
assert all('cullface' not in e['faces']['down'] for e in elements)
print(f'Generated and validated {len(mesh_faces)} mesh faces, seven block textures, the ritual circle and the book UV texture.')
