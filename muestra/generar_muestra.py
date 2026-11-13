import re, sys
src = sys.argv[1]; out = sys.argv[2]

def body(path, key):
    s = open(path).read()
    s = s[s.index('</helmet>') + len('</helmet>'):s.index('</x-dc>')].strip()
    s = s.replace('{{accent}}', 'var(--accent)')
    # Atributos de presentación SVG con el color -> estilo (los atributos no aceptan var()).
    s = re.sub(r'(stop-color|stroke|fill)="var\(--accent\)"', r'style="\1: var(--accent)"', s)
    s = s.replace('{{speed}}', '<span class="spd">0</span>')
    s = s.replace('transform="{{needle}}"', 'class="needle"')
    s = s.replace('stroke-dasharray="{{arc}}"', 'class="arc"')
    s = s.replace('<sc-if value="{{isMusic}}" hint-placeholder-val="{{ true }}">', '<div class="src-music" style="height: 100%">')
    s = s.replace('<sc-if value="{{isRadio}}" hint-placeholder-val="{{ false }}">', '<div class="src-radio" style="height: 100%">')
    s = s.replace('</sc-if>', '</div>')
    s = s.replace('onClick="{{showRadio}}" aria-label="Radio" style="{{radioBtn}}"', 'data-src="radio" aria-label="Radio" class="dockbtn"')
    s = s.replace('onClick="{{showMusic}}" aria-label="Música" style="{{musicBtn}}"', 'data-src="music" aria-label="Música" class="dockbtn"')
    s = s.replace('>18:09<', '><span class="clock">18:09</span><')
    s = s.replace('>viernes · 13-11-2026<', '><span class="date">viernes · 13-11-2026</span><')
    s = s.replace('id="band1"', 'id="band-' + key + '"').replace('url(#band1)', 'url(#band-' + key + ')')
    assert '{{' not in s, re.findall(r'\{\{[^}]*\}\}', s)[:5]
    # El primer div es la pantalla completa de esta propuesta.
    return '<div class="design" id="d-' + key + '"' + s[4:]

banda = body(src + '/V2Banda.dc.html', 'banda')
negro = body(src + '/V2Negro.dc.html', 'negro')

page = '''<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Muestra launcher</title>
<style>
@font-face{font-family:'Barlow';font-weight:400;src:url(fonts/barlow-400.woff2) format('woff2')}
@font-face{font-family:'Barlow';font-weight:500;src:url(fonts/barlow-500.woff2) format('woff2')}
@font-face{font-family:'Barlow';font-weight:600;src:url(fonts/barlow-600.woff2) format('woff2')}
@font-face{font-family:'Barlow';font-weight:700;src:url(fonts/barlow-700.woff2) format('woff2')}
@font-face{font-family:'Barlow Condensed';font-weight:500 600;src:url(fonts/barlowc-600.woff2) format('woff2')}
@font-face{font-family:'Barlow Condensed';font-weight:700;src:url(fonts/barlowc-700.woff2) format('woff2')}
html,body{margin:0;height:100%;background:#0B0E13;overflow:hidden;-webkit-user-select:none;user-select:none;-webkit-tap-highlight-color:transparent}
:root{--accent:#FF5A1F}
#stage{position:absolute;left:0;top:0;width:1024px;height:552px;transform-origin:0 0}
.design{position:absolute;left:0;top:0}
button{font-family:inherit;cursor:pointer}
.needle{transition:transform .5s ease-out}
#opt-btn{position:absolute;left:10px;top:8px;height:40px;padding:0 14px;border-radius:20px;border:1px solid #2A3240;background:rgba(21,26,34,.85);color:#A3ADBB;font:600 15px Barlow,sans-serif;z-index:5}
#opts{position:absolute;inset:0;background:rgba(0,0,0,.72);display:none;align-items:center;justify-content:center;z-index:10;font-family:Barlow,sans-serif;color:#F2F4F7}
#opts.on{display:flex}
.card{width:640px;background:#151A22;border:1px solid #2A3240;border-radius:20px;padding:22px 24px;display:flex;flex-direction:column;gap:16px}
.card h2{margin:0;font-size:24px;font-weight:600}
.row{display:flex;align-items:center;gap:12px}
.row>.lbl{width:120px;font-size:18px;color:#A3ADBB}
.seg{flex:1;height:52px;border-radius:12px;border:1px solid #2A3240;background:#1B212B;color:#DCE2EA;font-size:18px;font-weight:600}
.seg.sel{border:2px solid var(--accent);color:var(--accent);background:#151A22}
.sw{width:64px;height:52px;border-radius:12px;border:3px solid transparent;font-size:12px;color:#140A00;font-weight:700}
.sw.sel{border-color:#FFFFFF}
input[type=range]{flex:1;height:44px;accent-color:var(--accent)}
.val{width:80px;text-align:right;font:700 26px 'Barlow Condensed',sans-serif;color:var(--accent)}
.close{height:56px;border-radius:14px;border:2px solid var(--accent);background:#151A22;color:var(--accent);font-size:20px;font-weight:700}
</style>
</head>
<body>
<div id="stage">
''' + banda + '\n' + negro + '''
<button id="opt-btn">Opciones</button>
<div id="opts">
  <div class="card">
    <h2>Muestra del launcher</h2>
    <div class="row"><span class="lbl">Velocímetro</span>
      <button class="seg" data-design="banda">1 · Franja naranja</button>
      <button class="seg" data-design="negro">2 · Esfera negra</button></div>
    <div class="row"><span class="lbl">Panel</span>
      <button class="seg" data-panel="music">Música</button>
      <button class="seg" data-panel="radio">Radio</button></div>
    <div class="row"><span class="lbl">Naranja</span>
      <button class="sw" data-c="#FF5A1F" style="background:#FF5A1F">1</button>
      <button class="sw" data-c="#FF4A14" style="background:#FF4A14">2</button>
      <button class="sw" data-c="#FF6A1A" style="background:#FF6A1A">3</button>
      <button class="sw" data-c="#FF8A1E" style="background:#FF8A1E">4</button>
      <span style="flex:1"></span></div>
    <div class="row"><span class="lbl">Velocidad</span>
      <input id="spd" type="range" min="0" max="240" step="1" value="87"><span class="val" id="spdv">87</span></div>
    <div class="row"><span class="lbl"></span>
      <button class="seg" id="sim">Simular marcha</button></div>
    <button class="close" id="close">Cerrar</button>
  </div>
</div>
</div>
<script>
(function(){
  var st = {design:'banda', src:'music', color:'#FF5A1F', speed:87, sim:false};
  try { var saved = JSON.parse(localStorage.getItem('muestra')||'null'); if (saved) for (var k in saved) st[k]=saved[k]; } catch(e) {}
  st.sim = false;
  var $ = function(q,r){return Array.prototype.slice.call((r||document).querySelectorAll(q));};
  var DAYS=['domingo','lunes','martes','miércoles','jueves','viernes','sábado'];
  function pad(n){return (n<10?'0':'')+n;}
  function save(){ try{localStorage.setItem('muestra', JSON.stringify(st));}catch(e){} }
  function setSpeed(v){
    v=Math.max(0,Math.min(240,Math.round(v)));
    var deg=-135+v/240*270;
    $('.needle').forEach(function(n){n.setAttribute('transform','rotate('+deg.toFixed(2)+' 200 200)');});
    $('.arc').forEach(function(a){a.setAttribute('stroke-dasharray',(v*1.125).toFixed(1)+' 400');});
    $('.spd').forEach(function(s){s.textContent=v;});
    document.getElementById('spdv').textContent=v;
  }
  function render(){
    document.documentElement.style.setProperty('--accent', st.color);
    $('.design').forEach(function(d){d.style.display = d.id==='d-'+st.design ? 'flex' : 'none';});
    $('.src-music').forEach(function(e){e.style.display = st.src==='music' ? 'flex' : 'none';});
    $('.src-radio').forEach(function(e){e.style.display = st.src==='radio' ? 'flex' : 'none';});
    $('.dockbtn').forEach(function(b){
      var on = b.getAttribute('data-src')===st.src;
      b.style.cssText = 'flex: 1; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; ' +
        (on ? 'border: 2px solid var(--accent); background: #151A22; color: var(--accent);' : 'border: 1px solid #2A3240; background: #1B212B; color: #A3ADBB;');
    });
    $('[data-design]').forEach(function(b){b.classList.toggle('sel', b.getAttribute('data-design')===st.design);});
    $('[data-panel]').forEach(function(b){b.classList.toggle('sel', b.getAttribute('data-panel')===st.src);});
    $('[data-c]').forEach(function(b){b.classList.toggle('sel', b.getAttribute('data-c')===st.color);});
    document.getElementById('sim').classList.toggle('sel', st.sim);
    document.getElementById('spd').value = st.speed;
    setSpeed(st.speed);
    save();
  }
  // Las secciones de música y radio son columnas flexibles como en el diseño.
  $('.src-music,.src-radio').forEach(function(e){e.style.flexDirection='column';e.style.justifyContent='space-between';});
  $('.dockbtn').forEach(function(b){b.addEventListener('click',function(){st.src=b.getAttribute('data-src');render();});});
  $('[data-design]').forEach(function(b){b.addEventListener('click',function(){st.design=b.getAttribute('data-design');render();});});
  $('[data-panel]').forEach(function(b){b.addEventListener('click',function(){st.src=b.getAttribute('data-panel');render();});});
  $('[data-c]').forEach(function(b){b.addEventListener('click',function(){st.color=b.getAttribute('data-c');render();});});
  document.getElementById('spd').addEventListener('input',function(e){st.sim=false;st.speed=+e.target.value;render();});
  document.getElementById('opt-btn').addEventListener('click',function(){document.getElementById('opts').classList.add('on');});
  document.getElementById('close').addEventListener('click',function(){document.getElementById('opts').classList.remove('on');});
  document.getElementById('opts').addEventListener('click',function(e){if(e.target.id==='opts')e.target.classList.remove('on');});
  // Simulación: acelera, frena y vuelve a empezar.
  var simT=0;
  document.getElementById('sim').addEventListener('click',function(){st.sim=!st.sim;simT=0;render();});
  setInterval(function(){
    if(!st.sim) return;
    simT+=0.5;
    var v = 120 - 120*Math.cos(simT/12*Math.PI);
    st.speed = Math.round(Math.min(240, v*0.9 + 8*Math.sin(simT)));
    if (st.speed<0) st.speed=0;
    document.getElementById('spd').value = st.speed;
    setSpeed(st.speed);
  }, 500);
  function tick(){
    var d=new Date();
    $('.clock').forEach(function(c){c.textContent=pad(d.getHours())+':'+pad(d.getMinutes());});
    $('.date').forEach(function(c){c.textContent=DAYS[d.getDay()]+' · '+pad(d.getDate())+'-'+pad(d.getMonth()+1)+'-'+d.getFullYear();});
  }
  function fit(){
    var s=Math.min(window.innerWidth/1024, window.innerHeight/552);
    var stage=document.getElementById('stage');
    stage.style.transform='scale('+s+')';
    stage.style.left=((window.innerWidth-1024*s)/2)+'px';
    stage.style.top=((window.innerHeight-552*s)/2)+'px';
  }
  window.addEventListener('resize',fit);
  fit(); tick(); setInterval(tick, 5000); render();
})();
</script>
</body>
</html>
'''
open(out, 'w').write(page)
print(len(page))
