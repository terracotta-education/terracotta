import "@mdi/font/css/materialdesignicons.css";
import Vue from "vue";
import Vuetify from "vuetify/lib/framework";

Vue.use(Vuetify);

export default new Vuetify({
    icons: {
        iconfont: "mdi",
    },
    theme: {
        themes: {
            light: {
                primary: "#0077d2",
                info: "#0077d2",
                success: "#008568",
                error: "#b40808",
                warning: "#bf360c"
            }
        }
    }
});
