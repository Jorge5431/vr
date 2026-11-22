'use strict';
const library = [
 ['Storm Target','Mire nos círculos e acerte o centro.'],
 ['Storm Blocks','Solte a peça quando ela passar sobre a torre.'],
 ['Storm Boxing','Toque a manopla iluminada antes que ela recue.'],
 ['Storm Lab','Arraste cada amostra para a bandeja com o mesmo símbolo.'],
 ['Storm Rhythm','Toque quando os anéis se encontrarem na batida.']
];
const el = id => document.getElementById(id);
const board=el('board'), ctx=board.getContext('2d');
board.width=1000; board.height=500;
const ink='#eef3ff', aqua='#72e4d3', coral='#ff987d';
let mode=-1, start=0, score=0, round=0, target, stack, block, token, dragging=false, ended=false, raf=0;
let audio, lastBeat=-1, hitBeat=-1, best=0, feedback='', until=0;
const elapsed=()=> (performance.now()-start)/1000;
const nowMessage=(s)=>{ feedback=s; until=elapsed()+0.6; };
function beep(freq,duration=.07) {
 if(!audio || audio.state!=='running') return;
 const osc=audio.createOscillator(), gain=audio.createGain(), t=audio.currentTime;
 osc.frequency.value=freq; gain.gain.setValueAtTime(.09,t); gain.gain.exponentialRampToValueAtTime(.001,t+duration);
 osc.connect(gain);gain.connect(audio.destination);osc.start(t);osc.stop(t+duration);
}
function exitGame(){ cancelAnimationFrame(raf); mode=-1; dragging=false; if(audio) audio.suspend().catch(()=>{}); el('play').hidden=true; el('lobby').hidden=false; }
window.pauseArcade=exitGame;
el('back').onclick=exitGame;
library.forEach(([name,help],i)=>{ const button=document.createElement('button'), title=document.createElement('b'); title.textContent=name; button.append(title,help); button.onclick=()=>begin(i); el('library').append(button); });
function spawn(t){
 const x=110+Math.random()*780, y=88+Math.random()*288;
 target={x,y,t,limit:Math.max(.7,1.7-round*.025)};
 token={x:190,y:230,kind:Math.floor(Math.random()*3)}; dragging=false;
}
function begin(i){ cancelAnimationFrame(raf); mode=i; start=performance.now(); ended=false; score=round=0; feedback=''; until=0; lastBeat=hitBeat=-1; block=170;
 stack=[{x:500,w:240}]; spawn(0);
 try{audio=audio||new AudioContext();audio.resume().catch(()=>{});}catch(_){}
 el('lobby').hidden=true;el('play').hidden=false;el('name').textContent=library[i][0];el('hint').textContent=library[i][1]; frame(); }
function circle(x,y,r,color,fill=true){ctx.beginPath();ctx.arc(x,y,Math.max(r,1),0,Math.PI*2);if(fill){ctx.fillStyle=color;ctx.fill();}else{ctx.strokeStyle=color;ctx.lineWidth=4;ctx.stroke();}}
function label(s,x,y,size=28,color=ink){ctx.fillStyle=color;ctx.font='600 '+size+'px sans-serif';ctx.textAlign='center';ctx.fillText(s,x,y);}
function finish(){ended=true; dragging=false; try{const key='storm-arcade-'+mode;best=Math.max(score,Number(localStorage.getItem(key))||0);localStorage.setItem(key,best);}catch(_){best=score;}
 el('hud').textContent=score+' pontos';el('hint').textContent='Toque na área de jogo para repetir ou volte à biblioteca.';paintEnd();}
function paintEnd(){ctx.fillStyle='#17192c';ctx.fillRect(0,0,1000,500);label('Rodada concluída',500,185,48);label(score+' pontos   ·   Recorde '+best,500,260);label('Jogar de novo',500,362,30,aqua);}
function blockX(t){const w=stack[stack.length-1].w;return w/2+(1000-w)*(.5+.5*Math.sin(t*(1.7+round*.09)));}
function frame(){ if(mode<0||ended)return; const t=elapsed();if(t>=45){finish();return;}
 ctx.fillStyle='#141629';ctx.fillRect(0,0,1000,500);
 ctx.lineWidth=1;ctx.strokeStyle='#222640';for(let x=0;x<1000;x+=50){ctx.beginPath();ctx.moveTo(x,0);ctx.lineTo(x,500);ctx.stroke();}
 el('hud').textContent=score+' pontos   ·   '+Math.ceil(45-t)+' s';
 if(mode===0){const r=Math.max(26,56-round);circle(target.x,target.y,r,aqua);circle(target.x,target.y,r*.65,'#234c59');circle(target.x,target.y,r*.3,coral);}
 if(mode===1){const visible=stack.slice(-9);visible.forEach((b,n)=>{ctx.fillStyle=n%2?aqua:'#6fa8cb';ctx.fillRect(b.x-b.w/2,451-n*32,b.w,29);});
 block=blockX(t);const w=stack[stack.length-1].w;ctx.fillStyle=coral;ctx.fillRect(block-w/2,451-visible.length*32,w,29);}
 if(mode===2){if(t-target.t>=target.limit){round++;spawn(t);}circle(target.x,target.y,62,'#90483e');circle(target.x,target.y,45,coral);
 circle(target.x,target.y,66+20*Math.max(0,1-(t-target.t)/target.limit),ink,false);label('✦',target.x,target.y+13,42,'#572c34');}
 if(mode===3){const glyph=['●','▲','■'];[0,1,2].forEach((v)=>{const y=90+v*155;ctx.fillStyle='#333954';ctx.fillRect(717,y-60,225,120);label(glyph[v],828,y+14,44,aqua);});circle(token.x,token.y,48,coral);label(glyph[token.kind],token.x,token.y+13,38,'#211c32');}
 if(mode===4){const beat=Math.floor(t/.6);if(beat!==lastBeat){lastBeat=beat;beep(beat%4?440:660);}circle(500,250,83,aqua,false);circle(500,250,83+(1-(t%.6)/.6)*112,coral,false);label('♪',500,268,64);}
 if(t<until)label(feedback,500,40,26);raf=requestAnimationFrame(frame); }
const coords=e=>{const r=board.getBoundingClientRect();return {x:(e.clientX-r.left)*1000/r.width,y:(e.clientY-r.top)*500/r.height};};
board.onpointerdown=e=>{ if(ended){begin(mode);return;} const {x,y}=coords(e), t=elapsed(), dist=Math.hypot(x-target.x,y-target.y);
 if(mode===0 && dist<=Math.max(26,56-round)){score+=dist<18?3:1;round++;beep(730);spawn(t);}
 if(mode===1){const prev=stack[stack.length-1], px=blockX(t), lo=Math.max(prev.x-prev.w/2,px-prev.w/2),hi=Math.min(prev.x+prev.w/2,px+prev.w/2);if(hi-lo<5){finish();return;}stack.push({x:(hi+lo)/2,w:hi-lo});score+=2;round++;beep(340+round*20);}
 if(mode===2 && dist<64 && t-target.t<target.limit){score+=2;round++;beep(165);spawn(t);}
 if(mode===3 && Math.hypot(x-token.x,y-token.y)<66){dragging=true;board.setPointerCapture(e.pointerId);}
 if(mode===4){const beat=Math.round(t/.6),delta=Math.abs(t-beat*.6);if(beat!==hitBeat&&delta<=.14){score+=delta<.075?3:1;hitBeat=beat;nowMessage(delta<.075?'No tempo!':'Boa');beep(900);}else nowMessage('Acompanhe a batida');}
};
board.onpointermove=e=>{if(dragging&&!ended)Object.assign(token,coords(e));};
board.onpointerup=e=>{if(!dragging)return;dragging=false;const {x,y}=coords(e);if(x>=717&&x<=942&&Math.abs(y-(90+token.kind*155))<=60){score+=3;round++;beep(820);spawn(elapsed());}else{token.x=190;token.y=230;nowMessage('Tente a bandeja com o mesmo símbolo');}};
board.onpointercancel=()=>{dragging=false;if(token){token.x=190;token.y=230;}};
document.addEventListener('visibilitychange',()=>{if(document.hidden)exitGame();});
