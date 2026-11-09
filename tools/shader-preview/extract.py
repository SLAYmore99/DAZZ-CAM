import re, json, sys
import os
root=os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../app/src/main/java/com/funcam/app/')
sh=open(root+'gl/Shaders.kt').read()
def grab(name):
    m=re.search(r'const val '+name+r' = """(.*?)"""', sh, re.S); return m.group(1)
shaders={k:grab(k) for k in ['VERTEX','INPUT','COPY','FILM']}
cams=open(root+'model/Cameras.kt').read()
out=[]
for m in re.finditer(r'Recipe\(\s*"(\w+)", "([^"]+)", Category\.(\w+).*?Look\((.*?)\),\s*\n(.*?)\n        \),', cams, re.S):
    cid,name,cat,look,rest=m.groups()
    d={}
    # V3(...) values
    for k,a,b,c in re.findall(r'(\w+) = V3\(([-\d.]+)f, ([-\d.]+)f, ([-\d.]+)f\)', look):
        d[k]=[float(a),float(b),float(c)]
    for k,v in re.findall(r'(\w+) = (-?[\d.]+)f', look):
        d[k]=float(v)
    border=re.search(r'border = Border\.(\w+)',rest); stamp=re.search(r'stamp = StampStyle\.(\w+)',rest)
    ratio=re.search(r'ratios = listOf\((Ratio\(\d+, \d+\)|\w+)',rest)
    out.append(dict(id=cid,name=name,cat=cat,look=d,border=border.group(1) if border else 'NONE',ratio=ratio.group(1) if ratio else 'r34'))
json.dump(dict(shaders=shaders,cams=out),open('data.json','w'))
print(len(out),'cameras')
