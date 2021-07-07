(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-a2a52dea"],{4915:function(t,i,e){"use strict";e.r(i);var n=function(){var t=this,i=t.$createElement,e=t._self._c||i;return e("div",[e("h1",{staticClass:"mb-5"},[t._v(" Select the percent of students you would like to receive each condition ")]),t._m(0),e("v-card",{staticClass:"mt-2 mb-3 py-3 mx-auto lighten-5 rounded-lg",attrs:{outlined:""}},t._l(this.conditions,(function(i,n){return e("v-card-text",{key:i.conditionId,staticClass:"pa-5"},[e("v-row",{staticClass:"justify-space-between align-center"},[e("v-col",{staticClass:"py-0",attrs:{cols:"9"}},[e("v-card-title",{staticClass:"ma-0 pa-0 body-1"},[t._v(" "+t._s(i.name)+" will receive ")])],1),e("v-col",{staticClass:"py-0",attrs:{cols:"3"}},[e("v-text-field",{staticClass:"pa-0 ma-0 text-right",attrs:{outlined:"",suffix:"%",rules:[function(t){return!!t&&!!t.trim()||"Required"}],required:""},model:{value:t.distributionValue[n],callback:function(i){t.$set(t.distributionValue,n,i)},expression:"distributionValue[index]"}})],1)],1)],1)})),1),t.isDisabled()?e("p",{staticClass:"errorMessage mt-3"},[t._v(" Please Provide Positive Value for Each Condition Distribution and All Condition Distributions should be equal to 100%. ")]):t._e(),e("v-btn",{staticClass:"mt-3",attrs:{elevation:"0",disabled:t.isDisabled(),color:"primary"},on:{click:function(i){return t.updateDistribution()}}},[t._v("Continue ")])],1)},s=[function(){var t=this,i=t.$createElement,e=t._self._c||i;return e("div",{staticClass:"row mx-2"},[e("div",{staticClass:"col-9 label"},[t._v(" Condition ")]),e("div",{staticClass:"col-3 label text-right"},[t._v(" Distribution ")])])}],a=e("5530"),r=(e("d81d"),e("2f62")),o={name:"ParticipationCustomDistribution",props:["experiment"],data:function(){return{distributionValue:this.experiment.conditions.map((function(t){return t.distributionPct}))}},computed:{conditions:function(){return this.experiment.conditions},totalDistribution:function(){return this.distributionValue.map((function(t){return+t})).reduce((function(t,i){return t+i}),0)},experimentId:function(){return this.experiment.experimentId}},methods:Object(a["a"])(Object(a["a"])({},Object(r["b"])({updateConditions:"condition/updateConditions"})),{},{isDisabled:function(){return 100!==this.totalDistribution||this.distributionValue.some((function(t){return parseInt(t)<=0||isNaN(parseInt(t))}))},updateDistribution:function(){var t=this,i=this.conditions.map((function(i,e){return Object(a["a"])(Object(a["a"])({},i),{},{distributionPct:parseFloat(t.distributionValue[e]),experimentId:t.experimentId})}));this.updateConditions(i).then((function(i){200===(null===i||void 0===i?void 0:i.status)?t.$router.push({name:"ParticipationSummary",params:{experiment:t.experimentId}}):alert(i.error)})).catch((function(t){console.log("updateConditions | catch",{response:t})}))}})},c=o,u=(e("7f47"),e("2877")),l=e("6544"),d=e.n(l),b=e("8336"),p=e("b0af"),f=e("99d9"),h=e("62ad"),v=e("0fd9"),m=e("8654"),C=Object(u["a"])(c,n,s,!1,null,null,null);i["default"]=C.exports;d()(C,{VBtn:b["a"],VCard:p["a"],VCardText:f["a"],VCardTitle:f["b"],VCol:h["a"],VRow:v["a"],VTextField:m["a"]})},"615b":function(t,i,e){},"7f47":function(t,i,e){"use strict";e("fcc9")},"99d9":function(t,i,e){"use strict";e.d(i,"a",(function(){return o})),e.d(i,"b",(function(){return c}));var n=e("b0af"),s=e("80d2"),a=Object(s["f"])("v-card__actions"),r=Object(s["f"])("v-card__subtitle"),o=Object(s["f"])("v-card__text"),c=Object(s["f"])("v-card__title");n["a"]},b0af:function(t,i,e){"use strict";var n=e("5530"),s=(e("a9e3"),e("0481"),e("615b"),e("10d2")),a=e("297c"),r=e("1c87"),o=e("58df");i["a"]=Object(o["a"])(a["a"],r["a"],s["a"]).extend({name:"v-card",props:{flat:Boolean,hover:Boolean,img:String,link:Boolean,loaderHeight:{type:[Number,String],default:4},raised:Boolean},computed:{classes:function(){return Object(n["a"])(Object(n["a"])({"v-card":!0},r["a"].options.computed.classes.call(this)),{},{"v-card--flat":this.flat,"v-card--hover":this.hover,"v-card--link":this.isClickable,"v-card--loading":this.loading,"v-card--disabled":this.disabled,"v-card--raised":this.raised},s["a"].options.computed.classes.call(this))},styles:function(){var t=Object(n["a"])({},s["a"].options.computed.styles.call(this));return this.img&&(t.background='url("'.concat(this.img,'") center center / cover no-repeat')),t}},methods:{genProgress:function(){var t=a["a"].options.methods.genProgress.call(this);return t?this.$createElement("div",{staticClass:"v-card__progress",key:"progress"},[t]):null}},render:function(t){var i=this.generateRouteLink(),e=i.tag,n=i.data;return n.style=this.styles,this.isClickable&&(n.attrs=n.attrs||{},n.attrs.tabindex=0),t(e,this.setBackgroundColor(this.color,n),[this.genProgress(),this.$slots.default])}})},fcc9:function(t,i,e){}}]);
//# sourceMappingURL=chunk-a2a52dea.fabc2999.js.map