(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-10bd1444"],{"0393":function(e,t,n){"use strict";var s=n("5530"),a=(n("0481"),n("210b"),n("604c")),i=n("d9bd");t["a"]=a["a"].extend({name:"v-expansion-panels",provide:function(){return{expansionPanels:this}},props:{accordion:Boolean,disabled:Boolean,flat:Boolean,hover:Boolean,focusable:Boolean,inset:Boolean,popout:Boolean,readonly:Boolean,tile:Boolean},computed:{classes:function(){return Object(s["a"])(Object(s["a"])({},a["a"].options.computed.classes.call(this)),{},{"v-expansion-panels":!0,"v-expansion-panels--accordion":this.accordion,"v-expansion-panels--flat":this.flat,"v-expansion-panels--hover":this.hover,"v-expansion-panels--focusable":this.focusable,"v-expansion-panels--inset":this.inset,"v-expansion-panels--popout":this.popout,"v-expansion-panels--tile":this.tile})}},created:function(){this.$attrs.hasOwnProperty("expand")&&Object(i["a"])("expand","multiple",this),Array.isArray(this.value)&&this.value.length>0&&"boolean"===typeof this.value[0]&&Object(i["a"])(':value="[true, false, true]"',':value="[0, 2]"',this)},methods:{updateItem:function(e,t){var n=this.getValue(e,t),s=this.getValue(e,t+1);e.isActive=this.toggleMethod(n),e.nextIsActive=this.toggleMethod(s)}}})},"11a3":function(e,t,n){"use strict";n.r(t);var s=function(){var e=this,t=e.$createElement,n=e._self._c||t;return e.experiment&&e.assessment?n("div",{staticClass:"terracotta-builder"},[n("h1",[e._v(" Add your treatment for "+e._s(e.assignment.title)+"'s condition: "),n("strong",[e._v(e._s(e.condition.name))])]),n("form",{staticClass:"my-5",on:{submit:function(t){return t.preventDefault(),e.saveAll("AssignmentYourAssignments")}}},[n("v-text-field",{attrs:{rules:e.rules,label:"Treatment name",placeholder:"e.g. Lorem ipsum",autofocus:"",outlined:"",required:""},model:{value:e.assessment.title,callback:function(t){e.$set(e.assessment,"title",t)},expression:"assessment.title"}}),n("v-textarea",{attrs:{label:"Instructions or description (optional)",placeholder:"e.g. Lorem ipsum",outlined:""},model:{value:e.assessment.html,callback:function(t){e.$set(e.assessment,"html",t)},expression:"assessment.html"}}),e._m(0),e.questions&&e.questions.length>0?[n("v-expansion-panels",{staticClass:"v-expansion-panels--outlined mb-6",attrs:{flat:"",accordion:""}},e._l(e.questions,(function(t,s){return n("v-expansion-panel",{key:s,staticClass:"text-left"},[t?[n("v-expansion-panel-header",{staticClass:"text-left"},[n("h2",{staticClass:"pa-0"},[e._v(" "+e._s(s+1)+" "),t.html?n("span",{staticClass:"pl-3",domProps:{innerHTML:e._s(t.html)}}):e._e()])]),n("v-expansion-panel-content",[n("tiptap-vuetify",{staticClass:"mb-6 outlined",attrs:{placeholder:"Question",extensions:e.extensions,"card-props":{flat:!0},rules:e.rules,required:""},model:{value:t.html,callback:function(n){e.$set(t,"html",n)},expression:"question.html"}}),n("v-text-field",{attrs:{label:"Points",type:"number",outlined:"",required:""},model:{value:t.points,callback:function(n){e.$set(t,"points",n)},expression:"question.points"}}),t.answers?[n("h4",[n("strong",[e._v("Options")])]),n("p",{staticClass:"ma-0 mb-3"},[e._v("Select correct option(s) below")]),n("ul",{staticClass:"options-list pa-0 mb-6"},e._l(t.answers,(function(a,i){return n("li",{key:i,staticClass:"mb-3"},[n("v-row",{attrs:{align:"center"}},[n("v-col",{staticClass:"py-0",attrs:{cols:"1"}},[n("v-btn",{staticClass:"correct",class:{"green--text":a.correct},attrs:{icon:"",tile:""},on:{click:function(t){return e.handleToggleCorrect(s,i)}}},[a.correct?[n("v-icon",[e._v("mdi-checkbox-marked-circle")])]:[n("v-icon",[e._v("mdi-checkbox-marked-circle-outline")])]],2)],1),n("v-col",{attrs:{cols:"9"}},[n("v-text-field",{attrs:{label:"Option "+(i+1),rules:e.rules,"hide-details":"",outlined:"",required:""},model:{value:a.html,callback:function(t){e.$set(a,"html",t)},expression:"answer.html"}})],1),n("v-col",{staticClass:"py-0",attrs:{cols:"2"}},[n("v-btn",{staticClass:"delete_option",attrs:{icon:"",tile:""},on:{click:function(n){return e.handleDeleteAnswer(t,a)}}},[n("v-icon",[e._v("mdi-delete")])],1)],1)],1)],1)})),0)]:e._e(),n("v-row",[n("v-col",[n("v-btn",{staticClass:"px-0",attrs:{elevation:"0",color:"primary",plain:""},on:{click:function(n){return e.handleAddAnswer(t)}}},[e._v(" Add Option ")])],1),n("v-col",{staticClass:"text-right"},[n("v-menu",{scopedSlots:e._u([{key:"activator",fn:function(t){var s=t.on,a=t.attrs;return[n("v-icon",e._g(e._b({attrs:{color:"black"}},"v-icon",a,!1),s),[e._v(" mdi-dots-horizontal ")])]}}],null,!0)},[n("v-list",{staticClass:"text-left"},[n("v-list-item",{on:{click:function(n){return e.handleDeleteQuestion(t)}}},[n("v-list-item-title",[e._v("Delete Question")])],1)],1)],1)],1)],1)],2)]:e._e()],2)})),1)]:[n("p",{staticClass:"grey--text"},[e._v("Add questions to continue")])],n("v-btn",{staticClass:"mr-4 mb-3 px-0",attrs:{elevation:"0",color:"primary",plain:""},on:{click:function(t){return e.handleAddQuestion("MC")}}},[e._v(" Add Question ")]),n("br"),n("v-btn",{staticClass:"mr-4",attrs:{disabled:e.contDisabled,elevation:"0",color:"primary",type:"submit"}},[e._v(" Continue ")])],2)]):e._e()},a=[function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("h4",{staticClass:"mb-3"},[n("strong",[e._v("Multiple Choice Questions")])])}],i=n("1da1"),r=n("5530"),o=(n("96cf"),n("7db0"),n("498a"),n("d3b7"),n("3ca3"),n("ddb0"),n("d81d"),n("2f62")),c=n("3f38"),u={name:"TerracottaBuilder",props:["experiment"],computed:Object(r["a"])(Object(r["a"])({assignment_id:function(){return parseInt(this.$route.params.assignment_id)},exposure_id:function(){return parseInt(this.$route.params.exposure_id)},treatment_id:function(){return parseInt(this.$route.params.treatment_id)},assessment_id:function(){return parseInt(this.$route.params.assessment_id)},condition_id:function(){return parseInt(this.$route.params.condition_id)},condition:function(){var e=this;return this.experiment.conditions.find((function(t){return parseInt(t.conditionId)===parseInt(e.condition_id)}))}},Object(o["c"])({assignment:"assignment/assignment",assessment:"assessment/assessment",questions:"assessment/questions"})),{},{contDisabled:function(){return this.assessment.questions.length<1||this.assessment.questions.some((function(e){return!e.html.trim()}))||!this.assessment.title||!this.assessment.title.trim()}}),data:function(){return{rules:[function(e){return e&&!!e.trim()||"required"},function(e){return(e||"").length<=255||"A maximum of 255 characters is allowed"}],extensions:[c["g"],c["a"],c["j"],c["q"],c["n"],c["i"],c["k"],c["c"],c["l"],[c["f"],{options:{levels:[1,2,3]}}],c["b"],c["d"],c["h"],c["m"],c["e"]]}},methods:Object(r["a"])(Object(r["a"])({},Object(o["b"])({fetchAssessment:"assessment/fetchAssessment",updateAssessment:"assessment/updateAssessment",createQuestion:"assessment/createQuestion",updateQuestion:"assessment/updateQuestion",deleteQuestion:"assessment/deleteQuestion",createAnswer:"assessment/createAnswer",updateAnswer:"assessment/updateAnswer",deleteAnswer:"assessment/deleteAnswer"})),{},{handleAddQuestion:function(e){var t=this;return Object(i["a"])(regeneratorRuntime.mark((function n(){return regeneratorRuntime.wrap((function(n){while(1)switch(n.prev=n.next){case 0:return n.prev=0,n.next=3,t.createQuestion([t.experiment.experimentId,t.condition_id,t.treatment_id,t.assessment_id,0,e,0,""]);case 3:n.next=8;break;case 5:n.prev=5,n.t0=n["catch"](0),console.error(n.t0);case 8:case"end":return n.stop()}}),n,null,[[0,5]])})))()},handleAddAnswer:function(e){var t=this;return Object(i["a"])(regeneratorRuntime.mark((function n(){return regeneratorRuntime.wrap((function(n){while(1)switch(n.prev=n.next){case 0:return n.prev=0,n.next=3,t.createAnswer([t.experiment.experimentId,t.condition_id,t.treatment_id,t.assessment_id,e.questionId,"",!1,0]);case 3:n.next=8;break;case 5:n.prev=5,n.t0=n["catch"](0),console.error(n.t0);case 8:case"end":return n.stop()}}),n,null,[[0,5]])})))()},handleToggleCorrect:function(e,t){this.questions[e].answers[t].correct=!this.questions[e].answers[t].correct},handleDeleteAnswer:function(e,t){var n=this;return Object(i["a"])(regeneratorRuntime.mark((function s(){return regeneratorRuntime.wrap((function(s){while(1)switch(s.prev=s.next){case 0:return s.prev=0,s.next=3,n.deleteAnswer([n.experiment.experimentId,n.condition_id,n.treatment_id,n.assessment_id,e.questionId,t.answerId]);case 3:return s.abrupt("return",s.sent);case 6:s.prev=6,s.t0=s["catch"](0),console.error("handleDeleteAnswer | catch",{error:s.t0});case 9:case"end":return s.stop()}}),s,null,[[0,6]])})))()},handleDeleteQuestion:function(e){var t=this;return Object(i["a"])(regeneratorRuntime.mark((function n(){return regeneratorRuntime.wrap((function(n){while(1)switch(n.prev=n.next){case 0:return n.prev=0,n.next=3,t.deleteQuestion([t.experiment.experimentId,t.condition.conditionId,t.treatment_id,t.assessment_id,e.questionId]);case 3:return n.abrupt("return",n.sent);case 6:n.prev=6,n.t0=n["catch"](0),console.error("handleDeleteQuestion | catch",{error:n.t0});case 9:case"end":return n.stop()}}),n,null,[[0,6]])})))()},handleSaveAssessment:function(){var e=this;return Object(i["a"])(regeneratorRuntime.mark((function t(){return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.prev=0,t.next=3,e.updateAssessment([e.experiment.experimentId,e.condition.conditionId,e.treatment_id,e.assessment_id,e.assessment.title,e.assessment.html]);case 3:return t.abrupt("return",t.sent);case 6:t.prev=6,t.t0=t["catch"](0),console.error("handleCreateAssessment | catch",{error:t.t0});case 9:case"end":return t.stop()}}),t,null,[[0,6]])})))()},handleSaveQuestions:function(){var e=this;return Object(i["a"])(regeneratorRuntime.mark((function t(){return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.abrupt("return",Promise.all(e.questions.map(function(){var t=Object(i["a"])(regeneratorRuntime.mark((function t(n,s){var a;return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.prev=0,t.next=3,e.updateQuestion([e.experiment.experimentId,e.condition_id,e.treatment_id,e.assessment_id,n.questionId,n.html,n.points,s,n.questionType]);case 3:return a=t.sent,t.abrupt("return",Promise.resolve(a));case 7:return t.prev=7,t.t0=t["catch"](0),t.abrupt("return",Promise.reject(t.t0));case 10:case"end":return t.stop()}}),t,null,[[0,7]])})));return function(e,n){return t.apply(this,arguments)}}())));case 1:case"end":return t.stop()}}),t)})))()},handleSaveAnswers:function(){var e=this;return Object(i["a"])(regeneratorRuntime.mark((function t(){return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.abrupt("return",Promise.all(e.questions.map((function(t){var n;null===t||void 0===t||null===(n=t.answers)||void 0===n||n.map(function(){var t=Object(i["a"])(regeneratorRuntime.mark((function t(n,s){var a;return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.prev=0,t.next=3,e.updateAnswer([e.experiment.experimentId,e.condition_id,e.treatment_id,e.assessment_id,n.questionId,n.answerId,n.answerType,n.html,n.correct,s]);case 3:return a=t.sent,t.abrupt("return",Promise.resolve(a));case 7:return t.prev=7,t.t0=t["catch"](0),t.abrupt("return",Promise.reject(t.t0));case 10:case"end":return t.stop()}}),t,null,[[0,7]])})));return function(e,n){return t.apply(this,arguments)}}())}))));case 1:case"end":return t.stop()}}),t)})))()},saveAll:function(e){var t=this;return Object(i["a"])(regeneratorRuntime.mark((function n(){var s;return regeneratorRuntime.wrap((function(n){while(1)switch(n.prev=n.next){case 0:if(!t.assessment.questions.some((function(e){return!e.html}))){n.next=3;break}return alert("Please fill or delete empty questions."),n.abrupt("return",!1);case 3:return n.next=5,t.handleSaveAssessment();case 5:if(s=n.sent,!s){n.next=12;break}return n.next=9,t.handleSaveQuestions();case 9:return n.next=11,t.handleSaveAnswers();case 11:t.$router.push({name:e,params:{exposure_id:t.exposure_id}});case 12:case"end":return n.stop()}}),n)})))()},saveExit:function(){this.saveAll("home")}}),created:function(){this.fetchAssessment([this.experiment.experimentId,this.condition_id,this.treatment_id,this.assessment_id])},components:{TiptapVuetify:c["o"]}},l=u,d=(n("b0e0"),n("2877")),p=n("6544"),h=n.n(p),m=n("8336"),v=n("62ad"),f=n("cd55"),x=n("49e2"),b=n("c865"),w=n("0393"),g=n("132d"),_=n("8860"),A=n("da13"),k=n("5d23"),I=n("e449"),y=n("0fd9"),C=n("8654"),O=n("a844"),j=Object(d["a"])(l,s,a,!1,null,null,null);t["default"]=j.exports;h()(j,{VBtn:m["a"],VCol:v["a"],VExpansionPanel:f["a"],VExpansionPanelContent:x["a"],VExpansionPanelHeader:b["a"],VExpansionPanels:w["a"],VIcon:g["a"],VList:_["a"],VListItem:A["a"],VListItemTitle:k["b"],VMenu:I["a"],VRow:y["a"],VTextField:C["a"],VTextarea:O["a"]})},1681:function(e,t,n){},"210b":function(e,t,n){},"49e2":function(e,t,n){"use strict";var s=n("0789"),a=n("9d65"),i=n("a9ad"),r=n("3206"),o=n("80d2"),c=n("58df"),u=Object(c["a"])(a["a"],i["a"],Object(r["a"])("expansionPanel","v-expansion-panel-content","v-expansion-panel"));t["a"]=u.extend().extend({name:"v-expansion-panel-content",computed:{isActive:function(){return this.expansionPanel.isActive}},created:function(){this.expansionPanel.registerContent(this)},beforeDestroy:function(){this.expansionPanel.unregisterContent()},render:function(e){var t=this;return e(s["a"],this.showLazyContent((function(){return[e("div",t.setBackgroundColor(t.color,{staticClass:"v-expansion-panel-content",directives:[{name:"show",value:t.isActive}]}),[e("div",{class:"v-expansion-panel-content__wrap"},Object(o["r"])(t))])]})))}})},a844:function(e,t,n){"use strict";var s=n("5530"),a=(n("a9e3"),n("1681"),n("8654")),i=n("58df"),r=Object(i["a"])(a["a"]);t["a"]=r.extend({name:"v-textarea",props:{autoGrow:Boolean,noResize:Boolean,rowHeight:{type:[Number,String],default:24,validator:function(e){return!isNaN(parseFloat(e))}},rows:{type:[Number,String],default:5,validator:function(e){return!isNaN(parseInt(e,10))}}},computed:{classes:function(){return Object(s["a"])({"v-textarea":!0,"v-textarea--auto-grow":this.autoGrow,"v-textarea--no-resize":this.noResizeHandle},a["a"].options.computed.classes.call(this))},noResizeHandle:function(){return this.noResize||this.autoGrow}},watch:{lazyValue:function(){this.autoGrow&&this.$nextTick(this.calculateInputHeight)},rowHeight:function(){this.autoGrow&&this.$nextTick(this.calculateInputHeight)}},mounted:function(){var e=this;setTimeout((function(){e.autoGrow&&e.calculateInputHeight()}),0)},methods:{calculateInputHeight:function(){var e=this.$refs.input;if(e){e.style.height="0";var t=e.scrollHeight,n=parseInt(this.rows,10)*parseFloat(this.rowHeight);e.style.height=Math.max(n,t)+"px"}},genInput:function(){var e=a["a"].options.methods.genInput.call(this);return e.tag="textarea",delete e.data.attrs.type,e.data.attrs.rows=this.rows,e},onInput:function(e){a["a"].options.methods.onInput.call(this,e),this.autoGrow&&this.calculateInputHeight()},onKeyDown:function(e){this.isFocused&&13===e.keyCode&&e.stopPropagation(),this.$emit("keydown",e)}}})},b0e0:function(e,t,n){"use strict";n("e75d")},c865:function(e,t,n){"use strict";var s=n("5530"),a=n("0789"),i=n("9d26"),r=n("a9ad"),o=n("3206"),c=n("5607"),u=n("80d2"),l=n("58df"),d=Object(l["a"])(r["a"],Object(o["a"])("expansionPanel","v-expansion-panel-header","v-expansion-panel"));t["a"]=d.extend().extend({name:"v-expansion-panel-header",directives:{ripple:c["a"]},props:{disableIconRotate:Boolean,expandIcon:{type:String,default:"$expand"},hideActions:Boolean,ripple:{type:[Boolean,Object],default:!1}},data:function(){return{hasMousedown:!1}},computed:{classes:function(){return{"v-expansion-panel-header--active":this.isActive,"v-expansion-panel-header--mousedown":this.hasMousedown}},isActive:function(){return this.expansionPanel.isActive},isDisabled:function(){return this.expansionPanel.isDisabled},isReadonly:function(){return this.expansionPanel.isReadonly}},created:function(){this.expansionPanel.registerHeader(this)},beforeDestroy:function(){this.expansionPanel.unregisterHeader()},methods:{onClick:function(e){this.$emit("click",e)},genIcon:function(){var e=Object(u["r"])(this,"actions")||[this.$createElement(i["a"],this.expandIcon)];return this.$createElement(a["c"],[this.$createElement("div",{staticClass:"v-expansion-panel-header__icon",class:{"v-expansion-panel-header__icon--disable-rotate":this.disableIconRotate},directives:[{name:"show",value:!this.isDisabled}]},e)])}},render:function(e){var t=this;return e("button",this.setBackgroundColor(this.color,{staticClass:"v-expansion-panel-header",class:this.classes,attrs:{tabindex:this.isDisabled?-1:null,type:"button","aria-expanded":this.isActive},directives:[{name:"ripple",value:this.ripple}],on:Object(s["a"])(Object(s["a"])({},this.$listeners),{},{click:this.onClick,mousedown:function(){return t.hasMousedown=!0},mouseup:function(){return t.hasMousedown=!1}})}),[Object(u["r"])(this,"default",{open:this.isActive},!0),this.hideActions||this.genIcon()])}})},cd55:function(e,t,n){"use strict";var s=n("5530"),a=n("4e82"),i=n("3206"),r=n("80d2"),o=n("58df");t["a"]=Object(o["a"])(Object(a["a"])("expansionPanels","v-expansion-panel","v-expansion-panels"),Object(i["b"])("expansionPanel",!0)).extend({name:"v-expansion-panel",props:{disabled:Boolean,readonly:Boolean},data:function(){return{content:null,header:null,nextIsActive:!1}},computed:{classes:function(){return Object(s["a"])({"v-expansion-panel--active":this.isActive,"v-expansion-panel--next-active":this.nextIsActive,"v-expansion-panel--disabled":this.isDisabled},this.groupClasses)},isDisabled:function(){return this.expansionPanels.disabled||this.disabled},isReadonly:function(){return this.expansionPanels.readonly||this.readonly}},methods:{registerContent:function(e){this.content=e},unregisterContent:function(){this.content=null},registerHeader:function(e){this.header=e,e.$on("click",this.onClick)},unregisterHeader:function(){this.header=null},onClick:function(e){e.detail&&this.header.$el.blur(),this.$emit("click",e),this.isReadonly||this.isDisabled||this.toggle()},toggle:function(){var e=this;this.content&&(this.content.isBooted=!0),this.$nextTick((function(){return e.$emit("change")}))}},render:function(e){return e("div",{staticClass:"v-expansion-panel",class:this.classes,attrs:{"aria-expanded":String(this.isActive)}},Object(r["r"])(this))}})},e75d:function(e,t,n){}}]);
//# sourceMappingURL=chunk-10bd1444.6a9a4035.js.map