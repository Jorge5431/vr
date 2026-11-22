const fs=require('fs'),vm=require('vm'),assert=require('assert');
const els={};let clock=0;const ctx=new Proxy({}, {get:()=>()=>{},set:()=>true});
const el=id=>els[id]??=( {hidden:false,textContent:'',append(){},getContext:()=>ctx,getBoundingClientRect:()=>({left:0,top:0,width:1000,height:500}),setPointerCapture(){}} );
const box={document:{getElementById:el,createElement:()=>el(Math.random()),addEventListener(){}},window:{},performance:{now:()=>clock},Math,localStorage:{getItem:()=>null,setItem(){}},requestAnimationFrame:()=>1,cancelAnimationFrame(){}};
vm.createContext(box);vm.runInContext(fs.readFileSync(require('path').join(__dirname,'../android/app/src/main/assets/arcade/arcade.js'),'utf8'),box);
for(let i=0;i<5;i++){
 vm.runInContext(`begin(${i})`,box);
 assert.equal(el('play').hidden,false);
 vm.runInContext(i===0||i===2?'board.onpointerdown({clientX:target.x,clientY:target.y})':i===1?'board.onpointerdown({clientX:500,clientY:250})':i===3?'board.onpointerdown({clientX:token.x,clientY:token.y,pointerId:1});board.onpointerup({clientX:828,clientY:90+token.kind*155})':'board.onpointerdown({clientX:500,clientY:250})',box);
 assert(vm.runInContext('score>0',box),`score mode ${i}`);
 clock+=46000;vm.runInContext('frame()',box);assert(vm.runInContext('ended',box));
 vm.runInContext('exitGame()',box);assert.equal(el('play').hidden,true);
}
console.log('PASS: 5 games start, score, finish and exit (simulated DOM; not browser validation)');
