import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "./styles.css";
import "./services/i18n";
import "./services/theme";

createApp(App).use(router).mount("#app");
