(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-2d21365e"],{ad05:function(e,t,s){"use strict";s.r(t);var n=function(){var e=this,t=e.$createElement,s=e._self._c||t;return s("div",[e.assignments&&e.assignments.length?[s("ul",e._l(e.assignments,(function(t,n){return s("li",{key:n},[s("router-link",{attrs:{to:{name:"AssignmentTreatmentSelect",params:{exposure_id:t.exposureId,assignment_id:t.assignmentId}}}},[e._v(e._s(t.title))])],1)})),0),s("v-btn",{attrs:{elevation:"0",color:"primary",to:{name:"AssignmentCreateAssignment",params:{exposure_id:this.exposure_id}}}},[e._v("create assignment")])]:[s("p",[e._v("No assignments yet")]),s("v-btn",{attrs:{elevation:"0",color:"primary",to:{name:"AssignmentCreateAssignment",params:{exposure_id:this.exposure_id}}}},[e._v("create first assignment")])]],2)},i=[],a=s("5530"),r=s("2f62"),m={name:"YourAssignments",props:["experiment"],computed:Object(a["a"])({exposure_id:function(){return parseInt(this.$route.params.exposure_id)}},Object(r["c"])({assignments:"assignment/assignments"})),data:function(){return{}},methods:Object(a["a"])(Object(a["a"])({},Object(r["b"])({fetchAssignments:"assignment/fetchAssignments"})),{},{saveExit:function(){this.$router.push({name:"Home"})}}),created:function(){this.fetchAssignments([this.experiment.experimentId,this.exposure_id])}},o=m,u=s("2877"),c=s("6544"),p=s.n(c),d=s("8336"),g=Object(u["a"])(o,n,i,!1,null,null,null);t["default"]=g.exports;p()(g,{VBtn:d["a"]})}}]);
//# sourceMappingURL=chunk-2d21365e.aa020b78.js.map