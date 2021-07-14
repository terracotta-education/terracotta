(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-a2a52dea"],{4915:function(t,e,i){"use strict";i.r(e);var n=function(){var t=this,e=t.$createElement,i=t._self._c||e;return i("div",[i("h1",{staticClass:"mb-5"},[t._v(" Select the percent of students you would like to receive each condition ")]),t._m(0),i("v-card",{staticClass:"mt-2 mb-3 py-3 mx-auto lighten-5 rounded-lg",attrs:{outlined:""}},t._l(this.conditions,(function(e,n){return i("v-card-text",{key:e.conditionId,staticClass:"pa-5"},[i("v-row",{staticClass:"justify-space-between align-center"},[i("v-col",{staticClass:"py-0",attrs:{cols:"9"}},[i("v-card-title",{staticClass:"ma-0 pa-0 body-1"},[t._v(" "+t._s(e.name)+" will receive ")])],1),i("v-col",{staticClass:"py-0",attrs:{cols:"3"}},[i("v-text-field",{staticClass:"pa-0 ma-0 text-right",attrs:{outlined:"",suffix:"%",rules:[function(t){return!!t&&!!t.trim()||"Required"}],required:""},model:{value:t.distributionValue[n],callback:function(e){t.$set(t.distributionValue,n,e)},expression:"distributionValue[index]"}})],1)],1)],1)})),1),t.isDisabled()?i("p",{staticClass:"errorMessage mt-3"},[t._v(" Please Provide Positive Value for Each Condition Distribution and All Condition Distributions should be equal to 100%. ")]):t._e(),i("v-btn",{staticClass:"mt-3",attrs:{elevation:"0",disabled:t.isDisabled(),color:"primary"},on:{click:function(e){return t.updateDistribution("ParticipationSummary")}}},[t._v("Continue ")])],1)},s=[function(){var t=this,e=t.$createElement,i=t._self._c||e;return i("div",{staticClass:"row mx-2"},[i("div",{staticClass:"col-9 label"},[t._v(" Condition ")]),i("div",{staticClass:"col-3 label text-right"},[t._v(" Distribution ")])])}],a=i("5530"),r=(i("d81d"),i("2f62")),o={name:"ParticipationCustomDistribution",props:["experiment"],data:function(){return{distributionValue:this.experiment.conditions.map((function(t){return t.distributionPct}))}},computed:{conditions:function(){return this.experiment.conditions},totalDistribution:function(){return this.distributionValue.map((function(t){return+t})).reduce((function(t,e){return t+e}),0)},experimentId:function(){return this.experiment.experimentId}},methods:Object(a["a"])(Object(a["a"])({},Object(r["b"])({updateConditions:"condition/updateConditions"})),{},{isDisabled:function(){return 100!==this.totalDistribution||this.distributionValue.some((function(t){return parseInt(t)<=0||isNaN(parseInt(t))}))},updateDistribution:function(t){var e=this,i=this.conditions.map((function(t,i){return Object(a["a"])(Object(a["a"])({},t),{},{distributionPct:parseFloat(e.distributionValue[i]),experimentId:e.experimentId})}));this.updateConditions(i).then((function(i){null!==i&&void 0!==i&&i.every((function(t){return 200===t.status}))?e.$router.push({name:t,params:{experiment:e.experimentId}}):alert(i.error)})).catch((function(t){console.log("updateConditions | catch",{response:t})}))},saveExit:function(){this.isDisabled()?this.$router.push({name:"Home",params:{experiment:this.experiment.experiment_id}}):this.updateDistribution("Home")}})},c=o,u=(i("7f47"),i("2877")),l=i("6544"),d=i.n(l),p=i("8336"),b=i("b0af"),f=i("99d9"),h=i("62ad"),m=i("0fd9"),v=i("8654"),C=Object(u["a"])(c,n,s,!1,null,null,null);e["default"]=C.exports;d()(C,{VBtn:p["a"],VCard:b["a"],VCardText:f["a"],VCardTitle:f["b"],VCol:h["a"],VRow:m["a"],VTextField:v["a"]})},"615b":function(t,e,i){},"7f47":function(t,e,i){"use strict";i("fcc9")},"99d9":function(t,e,i){"use strict";i.d(e,"a",(function(){return o})),i.d(e,"b",(function(){return c}));var n=i("b0af"),s=i("80d2"),a=Object(s["f"])("v-card__actions"),r=Object(s["f"])("v-card__subtitle"),o=Object(s["f"])("v-card__text"),c=Object(s["f"])("v-card__title");n["a"]},b0af:function(t,e,i){"use strict";var n=i("5530"),s=(i("a9e3"),i("0481"),i("615b"),i("10d2")),a=i("297c"),r=i("1c87"),o=i("58df");e["a"]=Object(o["a"])(a["a"],r["a"],s["a"]).extend({name:"v-card",props:{flat:Boolean,hover:Boolean,img:String,link:Boolean,loaderHeight:{type:[Number,String],default:4},raised:Boolean},computed:{classes:function(){return Object(n["a"])(Object(n["a"])({"v-card":!0},r["a"].options.computed.classes.call(this)),{},{"v-card--flat":this.flat,"v-card--hover":this.hover,"v-card--link":this.isClickable,"v-card--loading":this.loading,"v-card--disabled":this.disabled,"v-card--raised":this.raised},s["a"].options.computed.classes.call(this))},styles:function(){var t=Object(n["a"])({},s["a"].options.computed.styles.call(this));return this.img&&(t.background='url("'.concat(this.img,'") center center / cover no-repeat')),t}},methods:{genProgress:function(){var t=a["a"].options.methods.genProgress.call(this);return t?this.$createElement("div",{staticClass:"v-card__progress",key:"progress"},[t]):null}},render:function(t){var e=this.generateRouteLink(),i=e.tag,n=e.data;return n.style=this.styles,this.isClickable&&(n.attrs=n.attrs||{},n.attrs.tabindex=0),t(i,this.setBackgroundColor(this.color,n),[this.genProgress(),this.$slots.default])}})},fcc9:function(t,e,i){}}]);
//# sourceMappingURL=chunk-a2a52dea.0a2a6d30.js.map