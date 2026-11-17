"""Check the actual OBJ/MTL loader inputs, texture slots and closed crown."""
import collections
import hashlib
import json
import math
import subprocess
import sys
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/eplusplus'
MODEL=ASSETS/'models/block'
model=json.loads((MODEL/'advanced_table.json').read_text())
assert model['loader']=='neoforge:obj' and model['automatic_culling'] is False
assert (ASSETS/model['model'].split(':')[1]).is_file()
for ref in model['textures'].values():
    im=Image.open(ASSETS/('textures/'+ref.split(':')[1]+'.png'))
    assert all(n>0 and n&(n-1)==0 for n in im.size)
materials=set();slots=set()
for line in (MODEL/'advanced_table.mtl').read_text().splitlines():
    p=line.split()
    if p and p[0]=='newmtl': materials.add(p[1])
    if p and p[0]=='map_Kd': slots.add(p[1][1:])
assert slots<=model['textures'].keys()
verts=[];uvs=[];normals=[];groups=set();group='';crown_edges=collections.Counter();count=0
for line in (MODEL/'advanced_table.obj').read_text().splitlines():
    p=line.split()
    if not p or p[0].startswith('#'): continue
    if p[0]=='v':
        v=tuple(map(float,p[1:]));assert all(math.isfinite(x) and 0<=x<=1 for x in v);verts.append(v)
    elif p[0]=='vt':
        uv=tuple(map(float,p[1:]));assert all(0<=x<=1 for x in uv);uvs.append(uv)
    elif p[0]=='vn':
        n=tuple(map(float,p[1:]));assert abs(sum(x*x for x in n)-1)<1e-5;normals.append(n)
    elif p[0]=='g':
        group=p[1];assert group not in groups,'Duplicate OBJ group would discard faces';groups.add(group)
    elif p[0]=='usemtl': assert p[1] in materials
    elif p[0]=='f':
        ids=[tuple(map(int,t.split('/'))) for t in p[1:]]
        assert len(ids) in (3,4)
        for a,b,c in ids: assert 1<=a<=len(verts) and 1<=b<=len(uvs) and 1<=c<=len(normals)
        points=[verts[t[0]-1] for t in ids]
        a,b,c=points[:3];ab=[b[i]-a[i] for i in range(3)];ac=[c[i]-a[i] for i in range(3)]
        n=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
        assert sum(n[i]*normals[ids[0][2]-1][i] for i in range(3))>1e-9,'Winding/normal mismatch'
        if group.startswith('Inlaid_enchanting_surface_'):
            for i in range(len(points)): crown_edges[tuple(sorted((points[i],points[(i+1)%len(points)])))]+=1
        count+=1
assert crown_edges and set(crown_edges.values())=={2},'Open crown seam'
for name in ('arcane_circle','arcane_ribbon','arcane_jewel'):
    im=Image.open(ASSETS/f'textures/fx/{name}.png');assert im.mode=='RGBA'
generated=[*MODEL.glob('advanced_table.*'),*(ASSETS/'textures/block/arcane_table').glob('*.png'),ASSETS/'textures/entity/book/arcane.png',ASSETS/'textures/fx/arcane_circle.png',ASSETS/'textures/fx/arcane_ribbon.png']
before=[hashlib.sha256(p.read_bytes()).digest() for p in generated]
subprocess.run([sys.executable,str(ROOT/'tools/build_table_assets.py')],check=True)
assert before==[hashlib.sha256(p.read_bytes()).digest() for p in generated],'Non-reproducible export'
print(f'PASS: {count} faces, unique loader groups, normals/winding, closed crown, all materials/textures, deterministic regeneration.')
