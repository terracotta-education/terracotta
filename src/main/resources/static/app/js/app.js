(function(e){function t(t){for(var r,o,s=t[0],i=t[1],c=t[2],l=0,p=[];l<s.length;l++)o=s[l],Object.prototype.hasOwnProperty.call(a,o)&&a[o]&&p.push(a[o][0]),a[o]=0;for(r in i)Object.prototype.hasOwnProperty.call(i,r)&&(e[r]=i[r]);f&&f(t);while(p.length)p.shift()();return u.push.apply(u,c||[]),n()}function n(){for(var e,t=0;t<u.length;t++){for(var n=u[t],r=!0,o=1;o<n.length;o++){var s=n[o];0!==a[s]&&(r=!1)}r&&(u.splice(t--,1),e=i(i.s=n[0]))}return e}var r={},o={app:0},a={app:0},u=[];function s(e){return i.p+"js/"+({}[e]||e)+".js"}function i(t){if(r[t])return r[t].exports;var n=r[t]={i:t,l:!1,exports:{}};return e[t].call(n.exports,n,n.exports,i),n.l=!0,n.exports}i.e=function(e){var t=[],n={"chunk-3c70197a":1,"chunk-ddb96dbe":1};o[e]?t.push(o[e]):0!==o[e]&&n[e]&&t.push(o[e]=new Promise((function(t,n){for(var r="css/"+({}[e]||e)+".css",a=i.p+r,u=document.getElementsByTagName("link"),s=0;s<u.length;s++){var c=u[s],l=c.getAttribute("data-href")||c.getAttribute("href");if("stylesheet"===c.rel&&(l===r||l===a))return t()}var p=document.getElementsByTagName("style");for(s=0;s<p.length;s++){c=p[s],l=c.getAttribute("data-href");if(l===r||l===a)return t()}var f=document.createElement("link");f.rel="stylesheet",f.type="text/css",f.onload=t,f.onerror=function(t){var r=t&&t.target&&t.target.src||a,u=new Error("Loading CSS chunk "+e+" failed.\n("+r+")");u.code="CSS_CHUNK_LOAD_FAILED",u.request=r,delete o[e],f.parentNode.removeChild(f),n(u)},f.href=a;var d=document.getElementsByTagName("head")[0];d.appendChild(f)})).then((function(){o[e]=0})));var r=a[e];if(0!==r)if(r)t.push(r[2]);else{var u=new Promise((function(t,n){r=a[e]=[t,n]}));t.push(r[2]=u);var c,l=document.createElement("script");l.charset="utf-8",l.timeout=120,i.nc&&l.setAttribute("nonce",i.nc),l.src=s(e);var p=new Error;c=function(t){l.onerror=l.onload=null,clearTimeout(f);var n=a[e];if(0!==n){if(n){var r=t&&("load"===t.type?"missing":t.type),o=t&&t.target&&t.target.src;p.message="Loading chunk "+e+" failed.\n("+r+": "+o+")",p.name="ChunkLoadError",p.type=r,p.request=o,n[1](p)}a[e]=void 0}};var f=setTimeout((function(){c({type:"timeout",target:l})}),12e4);l.onerror=l.onload=c,document.head.appendChild(l)}return Promise.all(t)},i.m=e,i.c=r,i.d=function(e,t,n){i.o(e,t)||Object.defineProperty(e,t,{enumerable:!0,get:n})},i.r=function(e){"undefined"!==typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},i.t=function(e,t){if(1&t&&(e=i(e)),8&t)return e;if(4&t&&"object"===typeof e&&e&&e.__esModule)return e;var n=Object.create(null);if(i.r(n),Object.defineProperty(n,"default",{enumerable:!0,value:e}),2&t&&"string"!=typeof e)for(var r in e)i.d(n,r,function(t){return e[t]}.bind(null,r));return n},i.n=function(e){var t=e&&e.__esModule?function(){return e["default"]}:function(){return e};return i.d(t,"a",t),t},i.o=function(e,t){return Object.prototype.hasOwnProperty.call(e,t)},i.p="/app/",i.oe=function(e){throw console.error(e),e};var c=window["webpackJsonp"]=window["webpackJsonp"]||[],l=c.push.bind(c);c.push=t,c=c.slice();for(var p=0;p<c.length;p++)t(c[p]);var f=l;u.push([0,"chunk-vendors"]),n()})({0:function(e,t,n){e.exports=n("56d7")},"56d7":function(e,t,n){"use strict";n.r(t);n("e260"),n("e6cf"),n("cca6"),n("a79d");var r=n("2b0e"),o=function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("v-app",[n("v-main",[n("router-view")],1)],1)},a=[],u={name:"App",data:function(){return{}}},s=u,i=n("2877"),c=n("6544"),l=n.n(c),p=n("7496"),f=n("f6c4"),d=Object(i["a"])(s,o,a,!1,null,null,null),h=d.exports;l()(d,{VApp:p["a"],VMain:f["a"]});n("d3b7"),n("3ca3"),n("ddb0");var m=n("8c4f"),g=function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("v-row",{staticClass:"text-center"},[n("v-col",{staticClass:"mt-10",attrs:{cols:"12"}},[n("v-btn",{on:{click:e.createExperiment}},[e._v("Get Started")])],1)],1)},v=[],b={name:"Home",components:{},methods:{createExperiment:function(){this.$router.push({name:"ExperimentDesignIntro",params:{experiment_id:1}})}}},y=b,w=n("8336"),k=n("62ad"),O=n("0fd9"),E=Object(i["a"])(y,g,v,!1,null,null,null),x=E.exports;l()(E,{VBtn:w["a"],VCol:k["a"],VRow:O["a"]}),r["a"].use(m["a"]);var S=[{path:"/",name:"Home",component:x},{path:"/experiment/:experiment_id",component:function(){return n.e("chunk-2d0ba0bb").then(n.bind(null,"3613"))},children:[{path:"",alias:"design",component:function(){return n.e("chunk-ddb96dbe").then(n.bind(null,"7f79"))},children:[{path:"",alias:"intro",name:"ExperimentDesignIntro",component:function(){return n.e("chunk-2d207d1a").then(n.bind(null,"a1c7"))}},{path:"title",name:"ExperimentDesignTitle",component:function(){return n.e("chunk-2d21db86").then(n.bind(null,"d301"))}}]},{path:"participation",component:function(){return n.e("chunk-ddb96dbe").then(n.bind(null,"7f79"))},children:[]},{path:"assignments",component:function(){return n.e("chunk-ddb96dbe").then(n.bind(null,"7f79"))},children:[]},{path:"summary",name:"ExperimentSummary",component:function(){return n.e("chunk-3c70197a").then(n.bind(null,"75c7"))}}]}],j=new m["a"]({mode:"history",base:"/app/",routes:S}),T=j,_=n("f309");r["a"].use(_["a"]);var C=new _["a"]({}),P=n("2f62"),A=n("0e44"),L=n("5530");function N(){return X.state.account&&X.state.account.user&&X.state.account.user.api_token?{Authorization:"Bearer "+X.state.account.user.api_token,"Content-Type":"application/json"}:{}}var I={login:B,getAll:D,getById:J,update:M,delete:V};function B(e,t){var n={method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({username:e,password:t})};return fetch("/api/users/authenticate",n).then(q).then((function(e){return e&&e.user&&e.user.api_token?e:{status:"failure",message:e&&e.message?e.message:"Unknown error, please contact support."}})).catch((function(e){return{status:"failure",error:e}}))}function D(){var e={method:"GET",headers:N()};return fetch("/api/users",e).then(q)}function J(e){var t={method:"GET",headers:N()};return fetch("/api/users/".concat(e),t).then(q)}function M(e){var t={method:"PUT",headers:Object(L["a"])(Object(L["a"])({},N()),{},{"Content-Type":"application/json"}),body:JSON.stringify(e)};return fetch("/api/users/".concat(e.id),t).then(q)}function V(e){var t={method:"DELETE",headers:N()};return fetch("/api/users/".concat(e),t).then(q)}function q(e){return e.text().then((function(t){var n=t&&JSON.parse(t);if(!e||!e.ok){if(401===e.status)return{status:"failure",message:"Invalid Credentials"};var r=n&&n.message||e.statusText;return Promise.reject(r)}return n})).catch((function(e){console.log(e)}))}var F={user:null},$={login:function(e,t){var n=e.dispatch,r=e.commit,o=t.email,a=t.password;return r("loginRequest",{email:o}),n("alert/error",null,{root:!0}),I.login(o,a).then((function(e){var t;if(e&&"success"===e.status)return r("loginSuccess",e),1===(null===(t=e.user)||void 0===t?void 0:t.length)&&T.push("/"),{status:"success"};r("loginFailure",e),n("alert/error",e.message,{root:!0})}),(function(e){return r("loginFailure",e),n("alert/error",e,{root:!0}),{status:"fail",error:e}})).catch((function(e){return{status:"fail",error:e}}))},logout:function(e){var t=e.commit,n={method:"POST",headers:N(),body:JSON.stringify({})};fetch("/api/users/logout",n).then((function(){t("userLogout")}))}},G={loginRequest:function(e,t){e.user=t},loginSuccess:function(e,t){e.user=t.user},loginFailure:function(e){e.user=null},userLogout:function(e){e.user=null,localStorage.removeItem("terracotta")}},H={isLoggedIn:function(e){return e.user}},R={namespaced:!0,state:F,actions:$,mutations:G,getters:H},U={type:null,message:null},z={success:function(e,t){var n=e.commit;n("success",t)},error:function(e,t){var n=e.commit;n("error",t)},clear:function(e,t){var n=e.commit;n("success",t)}},K={success:function(e,t){e.type="alert-success",e.message=t},error:function(e,t){e.type="alert-danger",e.message=t},clear:function(e){e.type=null,e.message=null}},Q={namespaced:!0,state:U,actions:z,mutations:K};r["a"].use(P["a"]);var W=new P["a"].Store({plugins:[Object(A["a"])({key:"terracotta"})],modules:{account:R,alert:Q}}),X=W;r["a"].config.productionTip=!1,new r["a"]({store:X,router:T,vuetify:C,render:function(e){return e(h)}}).$mount("#app")}});
//# sourceMappingURL=app.js.map