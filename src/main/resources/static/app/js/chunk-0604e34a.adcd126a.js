(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-0604e34a"],{"166a":function(t,e,i){},3860:function(t,e,i){"use strict";var n=i("604c");e["a"]=n["a"].extend({name:"button-group",provide:function(){return{btnToggle:this}},computed:{classes:function(){return n["a"].options.computed.classes.call(this)}},methods:{genData:n["a"].options.methods.genData}})},5100:function(t,e,i){"use strict";i.r(e);var n=function(){var t=this,e=t.$createElement,i=t._self._c||e;return i("div",[t._m(0),i("div",{staticClass:"mt-3"},[i("strong",[t._v(" Exposre Set: ")]),i("v-btn-toggle",{staticClass:"ml-3",attrs:{color:"primary",mandatory:""},model:{value:t.toggle_exclusive,callback:function(e){t.toggle_exclusive=e},expression:"toggle_exclusive"}},t._l([1,2,3],(function(e){return i("v-btn",{key:e},[t._v(t._s(e))])})),1)],1),i("v-card",{staticClass:"mt-5 pt-5 px-5 mx-auto lighten-5 rounded-lg",attrs:{outlined:""}},[i("v-card-title",[t._v("Group A will receive "),i("v-chip",{staticClass:"ma-2",attrs:{color:"primary"}},[t._v("Your Condidtion A")])],1),i("v-card-title",[t._v("Group B will receive "),i("v-chip",{staticClass:"ma-2",attrs:{color:"primary"}},[t._v("Your Condidtion B")])],1)],1),i("v-btn",{staticClass:"mt-5",attrs:{elevation:"0",color:"primary",to:{name:"AssignmentExposureSets"}}},[t._v("Continue ")])],1)},a=[function(){var t=this,e=t.$createElement,i=t._self._c||e;return i("h1",[t._v(" Because you have "),i("strong",[t._v("two conditions")]),t._v(" and would like your students to be "),i("strong",[t._v("exposed to every conditions")]),t._v("(within-subject), we will set you up with two exposure sets. ")])}],s=i("5530"),r=i("2f62"),l=i("4360"),o={name:"AssignmentExposureSets",props:["experiment"],computed:{exposures:function(){return this.$store.state.exposures}},methods:Object(s["a"])({},Object(r["b"])({fetchExposures:"exposures/fetchExposures"})),beforeRouteEnter:function(t,e,i){return l["a"].dispatch("exposures/fetchExposures",t.params.experiment_id).then(i,i)},beforeRouteUpdate:function(t,e,i){return l["a"].dispatch("exposures/fetchExposures",t.params.experiment_id).then(i,i)}},u=o,c=i("2877"),h=i("6544"),d=i.n(h),p=i("8336"),f=(i("7e58"),i("3860")),v=i("a9ad"),g=i("58df"),m=Object(g["a"])(f["a"],v["a"]).extend({name:"v-btn-toggle",props:{backgroundColor:String,borderless:Boolean,dense:Boolean,group:Boolean,rounded:Boolean,shaped:Boolean,tile:Boolean},computed:{classes:function(){return Object(s["a"])(Object(s["a"])({},f["a"].options.computed.classes.call(this)),{},{"v-btn-toggle":!0,"v-btn-toggle--borderless":this.borderless,"v-btn-toggle--dense":this.dense,"v-btn-toggle--group":this.group,"v-btn-toggle--rounded":this.rounded,"v-btn-toggle--shaped":this.shaped,"v-btn-toggle--tile":this.tile},this.themeClasses)}},methods:{genData:function(){var t=this.setTextColor(this.color,Object(s["a"])({},f["a"].options.methods.genData.call(this)));return this.group?t:this.setBackgroundColor(this.backgroundColor,t)}}}),b=i("b0af"),x=i("99d9"),y=i("3835"),C=(i("4de4"),i("8adc"),i("0789")),_=i("9d26"),V=i("4e82"),O=i("7560"),j=i("f2e7"),B=i("1c87"),k=i("af2b"),I=i("d9bd"),w=Object(g["a"])(v["a"],k["a"],B["a"],O["a"],Object(V["a"])("chipGroup"),Object(j["b"])("inputValue")).extend({name:"v-chip",props:{active:{type:Boolean,default:!0},activeClass:{type:String,default:function(){return this.chipGroup?this.chipGroup.activeClass:""}},close:Boolean,closeIcon:{type:String,default:"$delete"},closeLabel:{type:String,default:"$vuetify.close"},disabled:Boolean,draggable:Boolean,filter:Boolean,filterIcon:{type:String,default:"$complete"},label:Boolean,link:Boolean,outlined:Boolean,pill:Boolean,tag:{type:String,default:"span"},textColor:String,value:null},data:function(){return{proxyClass:"v-chip--active"}},computed:{classes:function(){return Object(s["a"])(Object(s["a"])(Object(s["a"])(Object(s["a"])({"v-chip":!0},B["a"].options.computed.classes.call(this)),{},{"v-chip--clickable":this.isClickable,"v-chip--disabled":this.disabled,"v-chip--draggable":this.draggable,"v-chip--label":this.label,"v-chip--link":this.isLink,"v-chip--no-color":!this.color,"v-chip--outlined":this.outlined,"v-chip--pill":this.pill,"v-chip--removable":this.hasClose},this.themeClasses),this.sizeableClasses),this.groupClasses)},hasClose:function(){return Boolean(this.close)},isClickable:function(){return Boolean(B["a"].options.computed.isClickable.call(this)||this.chipGroup)}},created:function(){var t=this,e=[["outline","outlined"],["selected","input-value"],["value","active"],["@input","@active.sync"]];e.forEach((function(e){var i=Object(y["a"])(e,2),n=i[0],a=i[1];t.$attrs.hasOwnProperty(n)&&Object(I["a"])(n,a,t)}))},methods:{click:function(t){this.$emit("click",t),this.chipGroup&&this.toggle()},genFilter:function(){var t=[];return this.isActive&&t.push(this.$createElement(_["a"],{staticClass:"v-chip__filter",props:{left:!0}},this.filterIcon)),this.$createElement(C["b"],t)},genClose:function(){var t=this;return this.$createElement(_["a"],{staticClass:"v-chip__close",props:{right:!0,size:18},attrs:{"aria-label":this.$vuetify.lang.t(this.closeLabel)},on:{click:function(e){e.stopPropagation(),e.preventDefault(),t.$emit("click:close"),t.$emit("update:active",!1)}}},this.closeIcon)},genContent:function(){return this.$createElement("span",{staticClass:"v-chip__content"},[this.filter&&this.genFilter(),this.$slots.default,this.hasClose&&this.genClose()])}},render:function(t){var e=[this.genContent()],i=this.generateRouteLink(),n=i.tag,a=i.data;a.attrs=Object(s["a"])(Object(s["a"])({},a.attrs),{},{draggable:this.draggable?"true":void 0,tabindex:this.chipGroup&&!this.disabled?0:a.attrs.tabindex}),a.directives.push({name:"show",value:this.active}),a=this.setBackgroundColor(this.color,a);var r=this.textColor||this.outlined&&this.color;return t(n,this.setTextColor(r,a),e)}}),$=Object(c["a"])(u,n,a,!1,null,null,null);e["default"]=$.exports;d()($,{VBtn:p["a"],VBtnToggle:m,VCard:b["a"],VCardTitle:x["b"],VChip:w})},"604c":function(t,e,i){"use strict";i.d(e,"a",(function(){return o}));var n=i("5530"),a=(i("a9e3"),i("4de4"),i("caad"),i("2532"),i("a434"),i("159b"),i("fb6a"),i("7db0"),i("c740"),i("166a"),i("a452")),s=i("7560"),r=i("58df"),l=i("d9bd"),o=Object(r["a"])(a["a"],s["a"]).extend({name:"base-item-group",props:{activeClass:{type:String,default:"v-item--active"},mandatory:Boolean,max:{type:[Number,String],default:null},multiple:Boolean,tag:{type:String,default:"div"}},data:function(){return{internalLazyValue:void 0!==this.value?this.value:this.multiple?[]:void 0,items:[]}},computed:{classes:function(){return Object(n["a"])({"v-item-group":!0},this.themeClasses)},selectedIndex:function(){return this.selectedItem&&this.items.indexOf(this.selectedItem)||-1},selectedItem:function(){if(!this.multiple)return this.selectedItems[0]},selectedItems:function(){var t=this;return this.items.filter((function(e,i){return t.toggleMethod(t.getValue(e,i))}))},selectedValues:function(){return null==this.internalValue?[]:Array.isArray(this.internalValue)?this.internalValue:[this.internalValue]},toggleMethod:function(){var t=this;if(!this.multiple)return function(e){return t.internalValue===e};var e=this.internalValue;return Array.isArray(e)?function(t){return e.includes(t)}:function(){return!1}}},watch:{internalValue:"updateItemsState",items:"updateItemsState"},created:function(){this.multiple&&!Array.isArray(this.internalValue)&&Object(l["c"])("Model must be bound to an array if the multiple property is true.",this)},methods:{genData:function(){return{class:this.classes}},getValue:function(t,e){return null==t.value||""===t.value?e:t.value},onClick:function(t){this.updateInternalValue(this.getValue(t,this.items.indexOf(t)))},register:function(t){var e=this,i=this.items.push(t)-1;t.$on("change",(function(){return e.onClick(t)})),this.mandatory&&!this.selectedValues.length&&this.updateMandatory(),this.updateItem(t,i)},unregister:function(t){if(!this._isDestroyed){var e=this.items.indexOf(t),i=this.getValue(t,e);this.items.splice(e,1);var n=this.selectedValues.indexOf(i);if(!(n<0)){if(!this.mandatory)return this.updateInternalValue(i);this.multiple&&Array.isArray(this.internalValue)?this.internalValue=this.internalValue.filter((function(t){return t!==i})):this.internalValue=void 0,this.selectedItems.length||this.updateMandatory(!0)}}},updateItem:function(t,e){var i=this.getValue(t,e);t.isActive=this.toggleMethod(i)},updateItemsState:function(){var t=this;this.$nextTick((function(){if(t.mandatory&&!t.selectedItems.length)return t.updateMandatory();t.items.forEach(t.updateItem)}))},updateInternalValue:function(t){this.multiple?this.updateMultiple(t):this.updateSingle(t)},updateMandatory:function(t){if(this.items.length){var e=this.items.slice();t&&e.reverse();var i=e.find((function(t){return!t.disabled}));if(i){var n=this.items.indexOf(i);this.updateInternalValue(this.getValue(i,n))}}},updateMultiple:function(t){var e=Array.isArray(this.internalValue)?this.internalValue:[],i=e.slice(),n=i.findIndex((function(e){return e===t}));this.mandatory&&n>-1&&i.length-1<1||null!=this.max&&n<0&&i.length+1>this.max||(n>-1?i.splice(n,1):i.push(t),this.internalValue=i)},updateSingle:function(t){var e=t===this.internalValue;this.mandatory&&e||(this.internalValue=e?void 0:t)}},render:function(t){return t(this.tag,this.genData(),this.$slots.default)}});o.extend({name:"v-item-group",provide:function(){return{itemGroup:this}}})},"7e58":function(t,e,i){},"8adc":function(t,e,i){},"99d9":function(t,e,i){"use strict";i.d(e,"a",(function(){return l})),i.d(e,"b",(function(){return o}));var n=i("b0af"),a=i("80d2"),s=Object(a["e"])("v-card__actions"),r=Object(a["e"])("v-card__subtitle"),l=Object(a["e"])("v-card__text"),o=Object(a["e"])("v-card__title");n["a"]},a434:function(t,e,i){"use strict";var n=i("23e7"),a=i("23cb"),s=i("a691"),r=i("50c4"),l=i("7b0b"),o=i("65f0"),u=i("8418"),c=i("1dde"),h=c("splice"),d=Math.max,p=Math.min,f=9007199254740991,v="Maximum allowed length exceeded";n({target:"Array",proto:!0,forced:!h},{splice:function(t,e){var i,n,c,h,g,m,b=l(this),x=r(b.length),y=a(t,x),C=arguments.length;if(0===C?i=n=0:1===C?(i=0,n=x-y):(i=C-2,n=p(d(s(e),0),x-y)),x+i-n>f)throw TypeError(v);for(c=o(b,n),h=0;h<n;h++)g=y+h,g in b&&u(c,h,b[g]);if(c.length=n,i<n){for(h=y;h<x-n;h++)g=h+n,m=h+i,g in b?b[m]=b[g]:delete b[m];for(h=x;h>x-n+i;h--)delete b[h-1]}else if(i>n)for(h=x-n;h>y;h--)g=h+n-1,m=h+i-1,g in b?b[m]=b[g]:delete b[m];for(h=0;h<i;h++)b[h+y]=arguments[h+2];return b.length=x-n+i,c}})}}]);
//# sourceMappingURL=chunk-0604e34a.adcd126a.js.map