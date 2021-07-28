(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-5caf6851"],{"47ad":function(e,n,t){},6062:function(e,n,t){"use strict";var i=t("6d61"),o=t("6566");e.exports=i("Set",(function(e){return function(){return e(this,arguments.length?arguments[0]:void 0)}}),o)},8217:function(e,n,t){"use strict";t.r(n);var i=function(){var e=this,n=e.$createElement,t=e._self._c||n;return t("div",[t("h1",[e._v("Name your conditions")]),t("p",[e._v("These will be used to label the different experimental versions of your assignments.")]),e.experiment?t("form",{staticClass:"my-5 mb-15",on:{submit:function(n){return n.preventDefault(),e.saveConditions("ExperimentDesignType")}}},[t("v-container",{staticClass:"pa-0"},e._l(e.experiment.conditions,(function(n,i){return t("v-row",{key:n.conditionId},[i<2?[t("v-col",{staticClass:"py-0"},[t("v-text-field",{attrs:{name:"condition-"+n.conditionId,rules:e.rules,label:"Condition name",placeholder:"e.g. Condition Name",outlined:"",required:""},model:{value:n.name,callback:function(t){e.$set(n,"name",t)},expression:"condition.name"}})],1)]:[t("v-col",{staticClass:"py-0"},[t("v-text-field",{attrs:{name:"condition-"+n.conditionId,rules:e.rules,label:"Condition name",placeholder:"e.g. Condition Name",outlined:"",required:""},model:{value:n.name,callback:function(t){e.$set(n,"name",t)},expression:"condition.name"}})],1),t("v-col",{staticClass:"py-0",attrs:{cols:"4",sm:"2"}},[t("v-btn",{staticClass:"delete_condition",attrs:{icon:"",outlined:"",tile:""},on:{click:function(t){return e.handleDeleteCondition(n)}}},[t("v-icon",[e._v("mdi-delete")])],1)],1)]],2)})),1),t("div",[t("v-btn",{staticClass:"add_condition px-0 mb-10",attrs:{color:"blue",text:""},on:{click:function(n){return e.createCondition({name:"",experiment_experiment_id:e.experiment.experimentId})}}},[e._v("Add another condition ")])],1),t("v-btn",{staticClass:"mr-4",attrs:{disabled:e.hasDuplicateValues(e.experiment.conditions,"name")||!e.experiment.conditions.length>0||!e.experiment.conditions.every((function(e){return e.name&&e.name.trim()})),elevation:"0",color:"primary",type:"submit"}},[e._v(" Next ")])],1):e._e()])},o=[],a=t("1da1"),r=t("5530"),s=(t("96cf"),t("498a"),t("4de4"),t("2f62")),c=t("4360"),d=(t("d3b7"),t("6062"),t("3ca3"),t("ddb0"),t("d81d"),{methods:{hasDuplicateValues:function(e,n){var t=new Set(e.map((function(e){return e[n]})));return t.size<e.length}}}),u={name:"DesignConditions",props:["experiment"],mixins:[d],data:function(){return{rules:[function(e){return e&&!!e.trim()||"Condition name is required"},function(e){return(e||"").length<=255||"A maximum of 255 characters is allowed"}]}},methods:Object(r["a"])(Object(r["a"])({},Object(s["b"])({createCondition:"condition/createCondition",deleteCondition:"condition/deleteCondition",updateConditions:"condition/updateConditions"})),{},{saveConditions:function(e){var n=this;return Object(a["a"])(regeneratorRuntime.mark((function t(){var i;return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return i=n.experiment,t.next=3,n.updateConditions(i.conditions).then((function(t){null!==t&&void 0!==t&&t.every((function(e){return 200===e.status}))?n.$router.push({name:e,params:{experiment:n.experiment.experimentId}}):null!==t&&void 0!==t&&t.some((function(e){return Object.prototype.hasOwnProperty.call(e,"message")}))?n.$swal(t.filter((function(e){return"undefined"!==typeof e.message}))[0].message):n.$swal("There was an error saving your conditions.")})).catch((function(e){console.log("updateConditions | catch",{response:e}),n.$swal("There was an error saving your conditions.")}));case 3:case"end":return t.stop()}}),t)})))()},handleDeleteCondition:function(e){var n=e.defaultCondition;n?this.$swal("You are attempting to delete the default condition. You must set one of the other existing conditions as the default before deleting this condition."):this.deleteCondition(e)},saveExit:function(){var e=this;return Object(a["a"])(regeneratorRuntime.mark((function n(){return regeneratorRuntime.wrap((function(n){while(1)switch(n.prev=n.next){case 0:e.saveConditions("Home");case 1:case"end":return n.stop()}}),n)})))()}}),beforeRouteEnter:function(e,n,t){c["a"].state.experiment.experiment.conditions.length<2?c["a"].dispatch("condition/createDefaultConditions",e.params.experiment_id).then((function(){return t()})):t()},beforeRouteUpdate:function(e,n,t){c["a"].state.experiment.experiment.conditions.length<2?c["a"].dispatch("condition/createDefaultConditions",e.params.experiment_id).then((function(){return t()})):t()}},l=u,m=(t("985a"),t("2877")),p=t("6544"),f=t.n(p),h=t("8336"),v=t("62ad"),x=t("a523"),C=t("132d"),b=t("0fd9"),g=t("8654"),w=Object(m["a"])(l,i,o,!1,null,"2001d4e5",null);n["default"]=w.exports;f()(w,{VBtn:h["a"],VCol:v["a"],VContainer:x["a"],VIcon:C["a"],VRow:b["a"],VTextField:g["a"]})},"985a":function(e,n,t){"use strict";t("47ad")}}]);
//# sourceMappingURL=chunk-5caf6851.3dd413df.js.map