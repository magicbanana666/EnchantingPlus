"""Orthographic software preview of the actual exported block model (no book).

This checks geometry/UVs; it does not reproduce Minecraft lighting or shaders.
Requires Pillow and NumPy. Output is an ignored build artifact.
"""
import json
import math
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/eplus'
model = json.loads((ASSETS / 'models/block/advanced_table.json').read_text())
textures = {k:np.array(Image.open(ASSETS / ('textures/'+v.split(':')[1]+'.png')).convert('RGB')) for k,v in model['textures'].items()}
W,H = 960,840
pixels = np.full((H,W,3), (25,23,31), dtype=np.uint8)
depth = np.full((H,W), -np.inf)
right = np.array([.8,0,-.6])
up = np.array([-.3,.8660254,-.4])
camera = np.cross(right,up)


def triangle(verts, uv, texture, shade):
    v = np.array([[480+np.dot(p-[8,5,8],right)*37,440-np.dot(p-[8,5,8],up)*37,np.dot(p,camera)] for p in verts])
    xmin,xmax = max(0,int(v[:,0].min())), min(W-1,int(np.ceil(v[:,0].max())))
    ymin,ymax = max(0,int(v[:,1].min())), min(H-1,int(np.ceil(v[:,1].max())))
    if xmin>xmax or ymin>ymax: return
    xx,yy = np.meshgrid(np.arange(xmin,xmax+1)+.5,np.arange(ymin,ymax+1)+.5)
    a,b,c = v
    det = (b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
    if abs(det)<1e-8: return
    wa = ((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/det
    wb = ((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/det
    wc = 1-wa-wb
    z = wa*a[2]+wb*b[2]+wc*c[2]
    subdepth = depth[ymin:ymax+1,xmin:xmax+1]
    mask = (wa>=-1e-7)&(wb>=-1e-7)&(wc>=-1e-7)&(z>subdepth)
    uv = np.array(uv)
    u = wa*uv[0,0]+wb*uv[1,0]+wc*uv[2,0]
    vv = wa*uv[0,1]+wb*uv[1,1]+wc*uv[2,1]
    tx = np.clip((u/16*texture.shape[1]).astype(int),0,texture.shape[1]-1)
    ty = np.clip((vv/16*texture.shape[0]).astype(int),0,texture.shape[0]-1)
    color = np.clip(texture[ty,tx]*shade,0,255).astype(np.uint8)
    pixels[ymin:ymax+1,xmin:xmax+1][mask] = color[mask]
    subdepth[mask] = z[mask]


# Parse the actual OBJ/MTL used by the game, not a separate mock-up mesh.
materials={}
current=None
for line in (ASSETS/'models/block/advanced_table.mtl').read_text().splitlines():
    parts=line.split()
    if not parts: continue
    if parts[0]=='newmtl':
        current=parts[1];materials[current]={'tint':1.0,'emission':0}
    elif parts[0]=='Kd': materials[current]['tint']=float(parts[1])
    elif parts[0]=='Ka': materials[current]['emission']=float(parts[1])
    elif parts[0]=='map_Kd': materials[current]['texture']=parts[1][1:]
positions=[];uvs=[];normals=[]
for line in (ASSETS/'models/block/advanced_table.obj').read_text().splitlines():
    parts=line.split()
    if not parts: continue
    if parts[0]=='v': positions.append(np.array(list(map(float,parts[1:])))*16)
    elif parts[0]=='vt': uvs.append([float(parts[1])*16,(1-float(parts[2]))*16])
    elif parts[0]=='vn': normals.append(np.array(list(map(float,parts[1:]))))
    elif parts[0]=='usemtl': current=materials[parts[1]]
    elif parts[0]=='f':
        ids=[list(map(int,v.split('/'))) for v in parts[1:]]
        verts=np.array([positions[v[0]-1] for v in ids])
        uv=np.array([uvs[v[1]-1] for v in ids])
        normal=normals[ids[0][2]-1]
        if np.dot(normal,camera)<=0: continue
        shade=.72+.28*max(0,np.dot(normal,np.array([-.3,.85,.43])))
        if current['emission']: shade=.96
        shade*=current['tint']
        for i in range(1,len(ids)-1):
            tri=[0,i,i+1]
            triangle(verts[tri],uv[tri],textures[current['texture']],shade)

out=ROOT/'build/table-model-preview.png'
out.parent.mkdir(exist_ok=True)
Image.fromarray(pixels).save(out)
print(out)
