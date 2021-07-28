(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-74f96c60"],{"0393":function(e,n,t){"use strict";var a=t("5530"),i=(t("0481"),t("210b"),t("604c")),s=t("d9bd");n["a"]=i["a"].extend({name:"v-expansion-panels",provide:function(){return{expansionPanels:this}},props:{accordion:Boolean,disabled:Boolean,flat:Boolean,hover:Boolean,focusable:Boolean,inset:Boolean,popout:Boolean,readonly:Boolean,tile:Boolean},computed:{classes:function(){return Object(a["a"])(Object(a["a"])({},i["a"].options.computed.classes.call(this)),{},{"v-expansion-panels":!0,"v-expansion-panels--accordion":this.accordion,"v-expansion-panels--flat":this.flat,"v-expansion-panels--hover":this.hover,"v-expansion-panels--focusable":this.focusable,"v-expansion-panels--inset":this.inset,"v-expansion-panels--popout":this.popout,"v-expansion-panels--tile":this.tile})}},created:function(){this.$attrs.hasOwnProperty("expand")&&Object(s["a"])("expand","multiple",this),Array.isArray(this.value)&&this.value.length>0&&"boolean"===typeof this.value[0]&&Object(s["a"])(':value="[true, false, true]"',':value="[0, 2]"',this)},methods:{updateItem:function(e,n){var t=this.getValue(e,n),a=this.getValue(e,n+1);e.isActive=this.toggleMethod(t),e.nextIsActive=this.toggleMethod(a)}}})},"210b":function(e,n,t){},"49e2":function(e,n,t){"use strict";var a=t("0789"),i=t("9d65"),s=t("a9ad"),r=t("3206"),o=t("80d2"),c=t("58df"),l=Object(c["a"])(i["a"],s["a"],Object(r["a"])("expansionPanel","v-expansion-panel-content","v-expansion-panel"));n["a"]=l.extend().extend({name:"v-expansion-panel-content",computed:{isActive:function(){return this.expansionPanel.isActive}},created:function(){this.expansionPanel.registerContent(this)},beforeDestroy:function(){this.expansionPanel.unregisterContent()},render:function(e){var n=this;return e(a["a"],this.showLazyContent((function(){return[e("div",n.setBackgroundColor(n.color,{staticClass:"v-expansion-panel-content",directives:[{name:"show",value:n.isActive}]}),[e("div",{class:"v-expansion-panel-content__wrap"},Object(o["r"])(n))])]})))}})},aa6c:function(e,n,t){"use strict";t.r(n);var a=function(){var e=this,n=e.$createElement,t=e._self._c||n;return e.assignment?t("div",[t("h1",{staticClass:"pa-0 mb-7"},[e._v("Now, let’s upload your treatments for each condition for "),t("strong",[e._v(e._s(e.assignment.title))])]),e.conditions?[t("v-expansion-panels",{staticClass:"v-expansion-panels--outlined mb-7",attrs:{flat:""}},[t("v-expansion-panel",{staticClass:"py-3"},[t("v-expansion-panel-header",[e._v(e._s(e.assignment.title)+" ("+e._s(e.assignment.treatments&&e.assignment.treatments.length||0)+"/"+e._s(e.conditions.length)+") ")]),t("v-expansion-panel-content",[t("v-list",{staticClass:"pa-0"},e._l(e.conditions,(function(n){return t("v-list-item",{key:n.conditionId,staticClass:"justify-center px-0"},[t("v-list-item-content",[t("p",{staticClass:"ma-0 pa-0"},[e._v(e._s(n.name))])]),t("v-list-item-action",[e.hasTreatment(n)?[t("v-btn",{attrs:{icon:"",outlined:"",text:"",tile:""},on:{click:function(t){return e.goToBuilder(n.conditionId)}}},[t("v-icon",[e._v("mdi-pencil")])],1)]:[t("v-btn",{attrs:{color:"primary",outlined:""},on:{click:function(t){return e.goToBuilder(n.conditionId)}}},[e._v("Select ")])]],2)],1)})),1)],1)],1)],1)]:[t("p",[e._v("no conditions")])]],2):e._e()},i=[],s=t("2909"),r=t("b85c"),o=t("1da1"),c=t("5530"),l=(t("7db0"),t("99af"),t("4de4"),t("96cf"),t("2f62")),d={name:"AssignmentTreatmentSelect",props:["experiment"],computed:Object(c["a"])(Object(c["a"])({},Object(l["c"])({assignment:"assignment/assignment",conditions:"experiment/conditions"})),{},{assignment_id:function(){return parseInt(this.$route.params.assignment_id)},exposure_id:function(){return parseInt(this.$route.params.exposure_id)}}),data:function(){return{tCount:0,conditionTreatments:[]}},methods:Object(c["a"])(Object(c["a"])({},Object(l["b"])({createTreatment:"treatment/createTreatment",createAssessment:"assessment/createAssessment",fetchAssignment:"assignment/fetchAssignment",checkTreatment:"treatment/checkTreatment"})),{},{handleCreateTreatment:function(e){var n=this;return Object(o["a"])(regeneratorRuntime.mark((function t(){return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.prev=0,t.next=3,n.createTreatment([n.experiment.experimentId,e,n.assignment_id]);case 3:return t.abrupt("return",t.sent);case 6:t.prev=6,t.t0=t["catch"](0),console.error("handleCreateTreatment | catch",{error:t.t0});case 9:case"end":return t.stop()}}),t,null,[[0,6]])})))()},handleCreateAssessment:function(e,n){var t=this;return Object(o["a"])(regeneratorRuntime.mark((function a(){return regeneratorRuntime.wrap((function(a){while(1)switch(a.prev=a.next){case 0:return a.prev=0,a.next=3,t.createAssessment([t.experiment.experimentId,e,n.treatmentId]);case 3:return a.abrupt("return",a.sent);case 6:a.prev=6,a.t0=a["catch"](0),console.error("handleCreateAssessment | catch",{error:a.t0});case 9:case"end":return a.stop()}}),a,null,[[0,6]])})))()},goToBuilder:function(e){var n=this;return Object(o["a"])(regeneratorRuntime.mark((function t(){var a,i,s,r,o,c;return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.next=2,n.handleCreateTreatment(e);case 2:return o=t.sent,t.next=5,n.handleCreateAssessment(e,null===o||void 0===o?void 0:o.data);case 5:if(c=t.sent,!(null!==o&&void 0!==o&&null!==(a=o.data)&&void 0!==a&&a.error||null!==c&&void 0!==c&&null!==(i=c.data)&&void 0!==i&&i.error)){t.next=9;break}return n.$swal("There was a problem creating your assessment"),t.abrupt("return",!1);case 9:n.$router.push({name:"TerracottaBuilder",params:{experiment_id:n.experiment.experimentId,condition_id:e,treatment_id:null===o||void 0===o||null===(s=o.data)||void 0===s?void 0:s.treatmentId,assessment_id:null===c||void 0===c||null===(r=c.data)||void 0===r?void 0:r.assessmentId}});case 10:case"end":return t.stop()}}),t)})))()},hasTreatment:function(e){var n=this;return!!this.conditionTreatments.find((function(t){return t.treatment&&t.condition.conditionId===e.conditionId&&t.treatment.assignmentId===n.assignment_id}))},checkConditionTreatments:function(){var e=this;return Object(o["a"])(regeneratorRuntime.mark((function n(){var t,a,i,o,l;return regeneratorRuntime.wrap((function(n){while(1)switch(n.prev=n.next){case 0:t=Object(r["a"])(e.conditions),n.prev=1,t.s();case 3:if((a=t.n()).done){n.next=11;break}return o=a.value,n.next=7,e.checkTreatment([e.experiment.experimentId,o.conditionId,e.assignment_id]);case 7:l=n.sent,null!==l&&void 0!==l&&null!==(i=l.data)&&void 0!==i&&i.find((function(n){return parseInt(n.assignmentId)===e.assignment_id}))&&function(){var n={treatment:l.data?l.data.find((function(n){return parseInt(n.assignmentId)===e.assignment_id})):null,condition:o};e.conditionTreatments=[].concat(Object(s["a"])(e.conditionTreatments.filter((function(t){return t.conditionId===n.conditionId&&t.treatment.assignmentId===e.assignment_id}))),[Object(c["a"])({},n)])}();case 9:n.next=3;break;case 11:n.next=16;break;case 13:n.prev=13,n.t0=n["catch"](1),t.e(n.t0);case 16:return n.prev=16,t.f(),n.finish(16);case 19:case"end":return n.stop()}}),n,null,[[1,13,16,19]])})))()},saveExit:function(){this.$router.push({name:"Home"})}}),created:function(){var e=this;return Object(o["a"])(regeneratorRuntime.mark((function n(){return regeneratorRuntime.wrap((function(n){while(1)switch(n.prev=n.next){case 0:return n.next=2,e.fetchAssignment([e.experiment.experimentId,e.exposure_id,e.assignment_id]);case 2:return n.next=4,e.checkConditionTreatments();case 4:case"end":return n.stop()}}),n)})))()}},u=d,p=t("2877"),h=t("6544"),m=t.n(h),v=t("8336"),x=t("cd55"),f=t("49e2"),b=t("c865"),g=t("0393"),I=t("132d"),_=t("8860"),j=t("da13"),w=t("1800"),O=t("5d23"),C=Object(p["a"])(u,a,i,!1,null,null,null);n["default"]=C.exports;m()(C,{VBtn:v["a"],VExpansionPanel:x["a"],VExpansionPanelContent:f["a"],VExpansionPanelHeader:b["a"],VExpansionPanels:g["a"],VIcon:I["a"],VList:_["a"],VListItem:j["a"],VListItemAction:w["a"],VListItemContent:O["a"]})},c865:function(e,n,t){"use strict";var a=t("5530"),i=t("0789"),s=t("9d26"),r=t("a9ad"),o=t("3206"),c=t("5607"),l=t("80d2"),d=t("58df"),u=Object(d["a"])(r["a"],Object(o["a"])("expansionPanel","v-expansion-panel-header","v-expansion-panel"));n["a"]=u.extend().extend({name:"v-expansion-panel-header",directives:{ripple:c["a"]},props:{disableIconRotate:Boolean,expandIcon:{type:String,default:"$expand"},hideActions:Boolean,ripple:{type:[Boolean,Object],default:!1}},data:function(){return{hasMousedown:!1}},computed:{classes:function(){return{"v-expansion-panel-header--active":this.isActive,"v-expansion-panel-header--mousedown":this.hasMousedown}},isActive:function(){return this.expansionPanel.isActive},isDisabled:function(){return this.expansionPanel.isDisabled},isReadonly:function(){return this.expansionPanel.isReadonly}},created:function(){this.expansionPanel.registerHeader(this)},beforeDestroy:function(){this.expansionPanel.unregisterHeader()},methods:{onClick:function(e){this.$emit("click",e)},genIcon:function(){var e=Object(l["r"])(this,"actions")||[this.$createElement(s["a"],this.expandIcon)];return this.$createElement(i["c"],[this.$createElement("div",{staticClass:"v-expansion-panel-header__icon",class:{"v-expansion-panel-header__icon--disable-rotate":this.disableIconRotate},directives:[{name:"show",value:!this.isDisabled}]},e)])}},render:function(e){var n=this;return e("button",this.setBackgroundColor(this.color,{staticClass:"v-expansion-panel-header",class:this.classes,attrs:{tabindex:this.isDisabled?-1:null,type:"button","aria-expanded":this.isActive},directives:[{name:"ripple",value:this.ripple}],on:Object(a["a"])(Object(a["a"])({},this.$listeners),{},{click:this.onClick,mousedown:function(){return n.hasMousedown=!0},mouseup:function(){return n.hasMousedown=!1}})}),[Object(l["r"])(this,"default",{open:this.isActive},!0),this.hideActions||this.genIcon()])}})},cd55:function(e,n,t){"use strict";var a=t("5530"),i=t("4e82"),s=t("3206"),r=t("80d2"),o=t("58df");n["a"]=Object(o["a"])(Object(i["a"])("expansionPanels","v-expansion-panel","v-expansion-panels"),Object(s["b"])("expansionPanel",!0)).extend({name:"v-expansion-panel",props:{disabled:Boolean,readonly:Boolean},data:function(){return{content:null,header:null,nextIsActive:!1}},computed:{classes:function(){return Object(a["a"])({"v-expansion-panel--active":this.isActive,"v-expansion-panel--next-active":this.nextIsActive,"v-expansion-panel--disabled":this.isDisabled},this.groupClasses)},isDisabled:function(){return this.expansionPanels.disabled||this.disabled},isReadonly:function(){return this.expansionPanels.readonly||this.readonly}},methods:{registerContent:function(e){this.content=e},unregisterContent:function(){this.content=null},registerHeader:function(e){this.header=e,e.$on("click",this.onClick)},unregisterHeader:function(){this.header=null},onClick:function(e){e.detail&&this.header.$el.blur(),this.$emit("click",e),this.isReadonly||this.isDisabled||this.toggle()},toggle:function(){var e=this;this.content&&(this.content.isBooted=!0),this.$nextTick((function(){return e.$emit("change")}))}},render:function(e){return e("div",{staticClass:"v-expansion-panel",class:this.classes,attrs:{"aria-expanded":String(this.isActive)}},Object(r["r"])(this))}})}}]);
//# sourceMappingURL=chunk-74f96c60.1f41590b.js.map