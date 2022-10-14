import '@mdi/font/css/materialdesignicons.css'
import Vue from 'vue';
import Vuetify from 'vuetify/lib/framework';
import DatetimePicker from 'vuetify-datetime-picker';

Vue.use(Vuetify);
Vue.use(DatetimePicker);

export default new Vuetify({
    icons: {
        iconfont: 'mdi',
    },
    theme: {
        themes: {
            light: {
                primary: '#0077D2',
                success: '#008568',
                error: '#f64747',
            }
        }
    }
});
