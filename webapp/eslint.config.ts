import vue from "eslint-plugin-vue";
import vuetify from "eslint-plugin-vue";

export default [
  ...vue.configs["flat/essential"],
  ...vuetify.configs["flat/base"],
];
