# Temporary cover and icon, until an artist draws the real ones, in the style of
# Beltworks', Groundworks' and Craftworks' publish/make-cover.py. Run from the repo root
# with Pillow installed, with the Beltworks checkout and the decompiled Minecraft sources
# under ~/minecraft_mods.
#
# The Pack reads as the series' modpack rather than a fourth mod by an orange frame round
# the whole image, a capital W on an orange "Works" pill, and a background of all three
# mods' rows plus the Pack's own ore.
import os
from PIL import Image, ImageDraw, ImageFont
MM=os.path.expanduser('~/minecraft_mods/')
MC=MM+'mc-26.1.2.109-src/assets/minecraft/textures/'
BW=MM+'beltworks/src/main/resources/assets/beltworks/textures/block/'
PK='kubejs/assets/planetaryfactory/textures/block/'
BG=(24,26,32)
ACCENT=(255,146,32)   # Factorio orange
SIB={'belt':(245,200,40),'ground':(90,165,255),'craft':(80,200,190)}
FADE=120  # 0 leaves the rows at full strength, 255 hides them
IMPACT='/System/Library/Fonts/Supplemental/Impact.ttf'
BLACK='/System/Library/Fonts/Supplemental/Arial Black.ttf'
_cache={}
def tex(path,s,rot=0):
    if (path,s,rot) not in _cache:
        im=Image.open(path).convert('RGBA')
        if im.height>im.width: im=im.crop((0,0,im.width,im.width))  # first frame of an animation strip
        if rot: im=im.rotate(rot,expand=True)
        _cache[path,s,rot]=im.resize((s,s),Image.NEAREST)
    return _cache[path,s,rot]
def slot(img,d,x,y,s,path):
    b=max(2,s//12)
    d.rectangle([x,y,x+s-1,y+s-1],fill=(52,54,62))
    d.rectangle([x,y,x+s-1,y+b-1],fill=(30,31,37)); d.rectangle([x,y,x+b-1,y+s-1],fill=(30,31,37))
    d.rectangle([x,y+s-b,x+s-1,y+s-1],fill=(84,87,98)); d.rectangle([x+s-b,y,x+s-1,y+s-1],fill=(84,87,98))
    p=s//8; t=tex(path,s-2*p); img.paste(t,(x+p,y+p),t)
def row_belt(img,d,y,s,k):
    t=tex(BW+['conveyorbelt','improved_conveyorbelt','express_conveyorbelt','turbo_conveyorbelt'][k%4]+'/frame_00.png',s,90)
    for x in range(-((k*s//3)%s)-s,img.width,s): img.paste(t,(x,y),t)
def row_ground(img,d,y,s,k):
    t=tex(MC+'block/'+['stone_bricks','oak_planks','bricks'][k%3]+'.png',s)
    laid=4+k%3
    for i,x in enumerate(range(-((k*s//2)%s)-s,img.width,s)):
        if i<laid: img.paste(t,(x,y),t); continue
        w=Image.alpha_composite(t,Image.new('RGBA',(s,s),SIB['ground']+(150,))); w.putalpha(200)
        img.alpha_composite(w,(x,y)); d.rectangle([x,y,x+s-1,y+s-1],outline=(255,255,255),width=max(2,s//20))
def row_craft(img,d,y,s,k):
    R=[(['iron_ingot','coal'],'iron_nugget'),(['copper_ingot','redstone'],'comparator'),(['gold_ingot'],'gold_nugget')]
    x=-((k*s)%(s*2))-s; g=s//8; j=k
    while x<img.width:
        ins,out=R[j%3]
        for n in ins: slot(img,d,x,y,s,MC+'item/'+n+'.png'); x+=s+g
        ay=y+s//2; d.rectangle([x,ay-s//10,x+s*2//3,ay+s//10],fill=SIB['craft'])
        d.polygon([(x+s*2//3-2,ay-s//4),(x+s,ay),(x+s*2//3-2,ay+s//4)],fill=SIB['craft']); x+=s+g
        slot(img,d,x,y,s,MC+'item/'+out+'.png'); x+=s*2; j+=1
def row_ore(img,d,y,s,k):
    ores=['iron','copper','coal','stone','uranium']
    for i,x in enumerate(range(-((k*s//2)%s)-s,img.width,s)):
        t=tex(PK+f'ore/{ores[(i//3+k)%5]}_stage{(i*3+k)%4}.png',s); img.paste(t,(x,y),t)
ROWS=[row_belt,row_ground,row_craft,row_ore]
def fonts(size): return ImageFont.truetype(IMPACT,size),ImageFont.truetype(BLACK,int(size*0.62))
def make(W,H,out,size,s,tag=None):
    fw=max(8,W//40)
    d=ImageDraw.Draw(Image.new('RGB',(1,1)))
    while True:
        f1,f2=fonts(size)
        if d.textbbox((0,0),'FACTORY',font=f1)[2]+d.textbbox((0,0),'Works',font=f2)[2]+int(size*0.42)<=W-2*fw-W//12: break
        size-=2
    img=Image.new('RGBA',(W,H),BG+(255,)); d=ImageDraw.Draw(img)
    bh=int(size*(2.3 if tag else 1.6)); t0=(H-bh)//2; t1=t0+bh
    # rows sit inside the frame, centred in the space between it and the band
    free=t0-fw; pitch=s+s//4; n=max(1,(free+s//4)//pitch); pad=(free-(n*pitch-s//4))//2
    ys=[fw+pad+i*pitch for i in range(n)]+[t1+pad+i*pitch for i in range(n)]
    for k,y in enumerate(ys): ROWS[k%len(ROWS)](img,d,y,s,k)
    img=Image.alpha_composite(img,Image.new('RGBA',(W,H),BG+(FADE,)))
    band=Image.new('RGBA',(W,H),(0,0,0,0)); ImageDraw.Draw(band).rectangle([0,t0,W,t1],fill=(16,17,22,235))
    img=Image.alpha_composite(img,band); d=ImageDraw.Draw(img)
    lt=max(3,size//25)
    for i,c in enumerate([SIB['belt'],SIB['ground'],SIB['craft'],ACCENT]):
        d.rectangle([W*i//4,t0,W*(i+1)//4,t0+lt],fill=c); d.rectangle([W*i//4,t1-lt,W*(i+1)//4,t1],fill=c)
    f1,f2=fonts(size); a,w='FACTORY','Works'
    b1=d.textbbox((0,0),a,font=f1); b2=d.textbbox((0,0),w,font=f2)
    w1=b1[2]-b1[0]; h1=b1[3]-b1[1]; w2=b2[2]-b2[0]; h2=b2[3]-b2[1]
    px=int(size*0.16); gap=int(size*0.1); bw=w2+2*px; x=(W-(w1+gap+bw))//2
    bt=(H-h1)//2-(int(size*0.35) if tag else 0); y=bt-b1[1]; sh=max(3,size//20)
    d.text((x+sh-b1[0],y+sh),a,font=f1,fill=(0,0,0)); d.text((x-b1[0],y),a,font=f1,fill=(240,240,236))
    bx=x+w1+gap
    d.rounded_rectangle([bx+sh,bt+sh,bx+bw+sh,bt+h1+sh],radius=px,fill=(0,0,0))
    d.rounded_rectangle([bx,bt,bx+bw,bt+h1],radius=px,fill=ACCENT)
    d.text((bx+px-b2[0],bt+(h1-h2)//2-b2[1]),w,font=f2,fill=BG)
    if tag:
        # the core mod's page wears the pack's art with its own plate, so the two never read as one project
        f3=ImageFont.truetype(BLACK,int(size*0.3)); t=' '.join(tag); b3=d.textbbox((0,0),t,font=f3)
        p=int(size*0.1); tw=b3[2]-b3[0]; th=b3[3]-b3[1]; ty=bt+h1+int(size*0.2)
        d.rectangle([(W-tw)//2-p,ty,(W+tw)//2+p,ty+th+2*p],fill=(240,240,236))
        d.text(((W-tw)//2-b3[0],ty+p-b3[1]),t,font=f3,fill=BG)
    d.rectangle([0,0,W-1,H-1],outline=ACCENT,width=fw)
    img.convert('RGB').save(out)
make(1280,640,'publish/factoryworks-cover.png',150,64)
make(512,512,'publish/factoryworks-icon.png',84,56)
make(1280,640,'publish/core/factoryworks-core-cover.png',150,64,'CORE')
make(512,512,'publish/core/factoryworks-core-icon.png',84,56,'CORE')
